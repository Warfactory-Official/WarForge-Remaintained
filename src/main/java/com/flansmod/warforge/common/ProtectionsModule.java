package com.flansmod.warforge.common;

import net.minecraft.network.chat.Component;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.network.PacketClientNotification;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.SiegeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityEvent.EnteringSection;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import com.flansmod.warforge.platform.BlockBreak;
import com.flansmod.warforge.platform.LivingDamage;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.LevelEvent.PotentialSpawns;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;
import com.flansmod.warforge.compat.Stacks;
import com.flansmod.warforge.compat.carryon.CarryOnCompat;

public class ProtectionsModule {
    /** Ops who toggled /f protection; server only. */
    private static final java.util.Set<UUID> OP_OVERRIDE = new java.util.HashSet<>();
    private static boolean inLoop = false;

    public ProtectionsModule() {
    }

    /** @return the new state */
    public static boolean toggleOpOverride(UUID opId) {
        return OP_OVERRIDE.add(opId) || !OP_OVERRIDE.remove(opId);
    }

    public static boolean isOpOverride(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Player player && OP_OVERRIDE.contains(player.getUUID()) && WarForgeMod.isOp(player);
    }

    @Nonnull
    public static ProtectionConfig getProtections(UUID playerID, DimBlockPos pos) {
        return getProtections(playerID, pos.toChunkPos());
    }

    @Nonnull
    public static ProtectionConfig getProtections(UUID playerID, DimChunkPos pos) {
        Faction playerFaction = playerID == null || playerID.equals(FactionIds.NONE)
                ? null : WarForgeMod.FACTIONS.registry.getFactionOfPlayer(playerID);
        return getProtectionsForFaction(playerFaction == null ? null : playerFaction.getUuid(), pos);
    }

    /** Profile for an actor known only by faction; {@code actorFaction} null or NONE = no faction. */
    @Nonnull
    public static ProtectionConfig getProtectionsForFaction(UUID actorFaction, DimChunkPos pos) {
        SiegeRegistry.SiegeZoneResult siegeZone = WarForgeMod.FACTIONS.sieges.getSiegeZone(pos);

        UUID factionID = WarForgeMod.FACTIONS.claims.getClaim(pos);
        if (factionID.equals(FactionIds.SAFE_ZONE))
            return WarForgeConfig.SAFE_ZONE;

        if (factionID.equals(FactionIds.WAR_ZONE))
            return WarForgeConfig.WAR_ZONE;

        Faction faction = WarForgeMod.FACTIONS.registry.getFaction(factionID);
        boolean hasActorFaction = FactionIds.isValid(actorFaction);
        boolean member = faction != null && hasActorFaction && faction.getUuid().equals(actorFaction);

        if (member && faction.getClaimType(pos) == ClaimType.SIEGE)
            return WarForgeConfig.CLAIM_FRIEND;

        if (siegeZone.zone != SiegeRegistry.SiegeZone.NONE) {
            boolean defender = hasActorFaction && actorFaction.equals(siegeZone.defendingFaction);
            if (siegeZone.zone == SiegeRegistry.SiegeZone.SIEGED)
                return defender ? WarForgeConfig.SIEGED_FRIEND : WarForgeConfig.SIEGED_FOE;
            return defender ? WarForgeConfig.WAR_FRIEND : WarForgeConfig.WAR_FOE;
        }

        if (faction != null) {
            if (member && WarForgeMod.FACTIONS.sieges.isDefending(faction.getUuid()))
                return WarForgeConfig.CLAIM_DEFENDED;

            if (faction.getCitadelPos().toChunkPos().equals(pos))
                return member ? WarForgeConfig.CITADEL_FRIEND : WarForgeConfig.CITADEL_FOE;

            if (member)
                return WarForgeConfig.CLAIM_FRIEND;

            if (faction.isAllowAllyInteraction() && hasActorFaction && faction.isAllyOf(actorFaction))
                return WarForgeConfig.CLAIM_ALLY;

            return WarForgeConfig.CLAIM_FOE;
        }

        return WarForgeConfig.UNCLAIMED;
    }

    @SubscribeEvent
    public void onDismount(EntityMountEvent event) {
        if (event.getEntityMounting().level().isClientSide())
            return;

        if (event.getEntityMounting() instanceof Player) {
            Entity vehicle = event.getEntityBeingMounted();
            DimBlockPos vehiclePos = new DimBlockPos(vehicle.level().dimension(), vehicle.blockPosition());
            ProtectionConfig mountConfig = getProtections(event.getEntityMounting().getUUID(), vehiclePos);

            if (event.isMounting() && !mountConfig.ALLOW_MOUNT_ENTITY)
                event.setCanceled(true);

            if (event.isDismounting() && !mountConfig.ALLOW_DISMOUNT_ENTITY)
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide())
            return;

        var dim = event.getLevel().dimension();
        HashMap<DimChunkPos, ProtectionConfig> checkedPositions = new HashMap<DimChunkPos, ProtectionConfig>();

        for (int i = event.getAffectedBlocks().size() - 1; i >= 0; i--) {
            DimChunkPos cPos = new DimChunkPos(dim, event.getAffectedBlocks().get(i));
            if (!checkedPositions.containsKey(cPos)) {
                ProtectionConfig config = getProtections(FactionIds.NONE, cPos);
                checkedPositions.put(cPos, config);
            }
            if (!checkedPositions.get(cPos).EXPLOSION_DAMAGE || !checkedPositions.get(cPos).BLOCK_REMOVAL) {
                event.getAffectedBlocks().remove(i);
            }
        }
    }

    // Piston push/pull across a claim border bypasses per-claim rules: cancel when a moved/destroyed block leaves or enters a chunk not owned by the piston's chunk owner. Cold path.
    @SubscribeEvent
    public void onPistonMove(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide())
            return;

        ResourceKey<Level> dim = level.dimension();
        UUID pistonOwner = WarForgeMod.FACTIONS.claims.getClaim(new DimBlockPos(dim, event.getPos()));

        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver != null && resolver.resolve()) {
            Direction moveDir = resolver.getPushDirection();
            for (BlockPos pushed : resolver.getToPush()) {
                if (pistonDenied(dim, pistonOwner, pushed) || pistonDenied(dim, pistonOwner, pushed.relative(moveDir))) {
                    event.setCanceled(true);
                    return;
                }
            }
            for (BlockPos destroyed : resolver.getToDestroy()) {
                if (pistonDenied(dim, pistonOwner, destroyed)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        // The extending head itself occupies the cell directly in front of the piston.
        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND
                && pistonDenied(dim, pistonOwner, event.getFaceOffsetPos())) {
            event.setCanceled(true);
        }
    }

    /** The piston acts as its chunk owner's faction; the target chunk's profile decides. */
    private static boolean pistonDenied(ResourceKey<Level> dim, UUID pistonOwner, BlockPos pos) {
        DimChunkPos chunk = new DimChunkPos(dim, pos);
        return !WarForgeMod.FACTIONS.claims.getClaim(chunk).equals(pistonOwner)
                && !getProtectionsForFaction(pistonOwner, chunk).ALLOW_FOREIGN_PISTON_PUSH;
    }

    public void onDamage(LivingDamage event) {
        if (event.getEntity().level().isClientSide())
            return;

        LivingEntity victim = event.getEntity();
        DimBlockPos damagedPos = new DimBlockPos(victim.level().dimension(), victim.blockPosition());
        ProtectionConfig damagedConfig = getProtections(victim.getUUID(), damagedPos);

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker != null) {
            if (attacker instanceof Player && victim instanceof Player) {
                Faction attackerFaction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(attacker.getUUID());
                Faction victimFaction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(victim.getUUID());
                if (attackerFaction != null && victimFaction != null
                        && !attackerFaction.getUuid().equals(victimFaction.getUuid())
                        && attackerFaction.isInTruceWith(victimFaction.getUuid())) {
                    event.setCanceled(true);
                    return;
                }
            }
            if (attacker instanceof Player) {
                if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_PLAYER) {
                    event.setCanceled(true);
                    return;
                }

                DimBlockPos attackerPos = new DimBlockPos(attacker.level().dimension(), attacker.blockPosition());
                ProtectionConfig attackerConfig = getProtections(attacker.getUUID(), attackerPos);

                if (!attackerConfig.PLAYER_DEAL_DAMAGE) {
                    event.setCanceled(true);
                    return;
                }
            } else if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_MOB) {
                event.setCanceled(true);
                return;
            }
        } else {
            if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_OTHER) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /** Mekanism fires cable placement twice, the first time without an entity. */
    @SubscribeEvent
    public void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide())
            return;

        Entity eventEntity = event.getEntity();

        if (isOpOverride(eventEntity))
            return;

        Block placedBlock = event.getPlacedBlock().getBlock();
        // Carry On fires this before setting the block: getPlacedBlock is the replaced state
        if (eventEntity instanceof Player player) {
            Block carried = CarryOnCompat.carriedBlock(player);
            if (carried != null) {
                placedBlock = carried;
            }
        }
        if (eventEntity == null && BuiltInRegistries.BLOCK.getKey(placedBlock).getNamespace().equals("mekanism")) {
            return;
        }

        DimBlockPos pos = new DimBlockPos(((Level) event.getLevel()).dimension(), event.getPos());
        ProtectionConfig config = getProtections(eventEntity == null ? FactionIds.NONE : eventEntity.getUUID(), pos);

        if (placeDenied(config, placedBlock))
            event.setCanceled(true);
    }

    public static boolean mayPlace(Player player, Level level, BlockPos pos, Block block) {
        return isOpOverride(player) || !placeDenied(getProtections(player.getUUID(), new DimBlockPos(level.dimension(), pos)), block);
    }

    public static boolean placeDenied(ProtectionConfig config, Block block) {
        if (!config.PLACE_BLOCKS) {
            return !config.BLOCK_PLACE_WHITELIST.contains(block);
        }
        return config.BLOCK_PLACE_BLACKLIST.contains(block);
    }

    public void blockRemoved(BlockBreak event) {
        if (event.getLevel().isClientSide())
            return;

        if (isOpOverride(event.getPlayer()))
            return;

        DimBlockPos pos = new DimBlockPos(event.getPlayer().level().dimension(), event.getPos());
        ProtectionConfig config = getProtections(event.getPlayer().getUUID(), pos);
        Block block = event.getState().getBlock();

        if (!breakDenied(config, block))
            return;

        // MineTime: denied break becomes slow break. Hard cancel kept when MineTime opts out or creative (instabreak => free break). hardness <= 0 cannot be paced; predictor never cancels them, so a server cancel would only rubber-band => let break.
        MineTime.Rule rule = config.mineTime.resolve(block);
        if (rule != null && !event.getPlayer().getAbilities().instabuild)
            return;

        event.setCanceled(true);
    }

    public static boolean breakDenied(ProtectionConfig config, Block block) {
        if (!config.BREAK_BLOCKS || !config.BLOCK_REMOVAL) {
            return !config.BLOCK_BREAK_WHITELIST.contains(block);
        }
        return config.BLOCK_BREAK_BLACKLIST.contains(block);
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        Level level = player.level();
        // Server-authoritative: the client lacks claim-ownership data, and the server paces the
        // survival break, so MineTime only adjusts the speed here.
        if (level.isClientSide())
            return;

        if (isOpOverride(player))
            return;

        BlockPos blockPos = event.getPosition().orElse(null);
        if (blockPos == null)
            return;

        DimBlockPos pos = new DimBlockPos(level.dimension(), blockPos);
        ProtectionConfig config = getProtections(player.getUUID(), pos);
        Block block = event.getState().getBlock();

        if (!breakDenied(config, block))
            return;

        MineTime.Rule rule = config.mineTime.resolve(block);
        if (rule == null)
            return; // hard-protected; BlockRemoved cancels the break outright

        event.setNewSpeed(MineTime.applySpeed(rule, event.getNewSpeed(), event.getState(), level, blockPos, player));
    }

    @SubscribeEvent
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide())
            return;

        if (isOpOverride(event.getEntity()))
            return;

        Entity target = event.getTarget();
        DimBlockPos pos = new DimBlockPos(target.level().dimension(), target.blockPosition());
        ProtectionConfig config = getProtections(event.getEntity().getUUID(), pos);

        if (!config.INTERACT) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide())
            return;

        if (isOpOverride(event.getEntity()))
            return;

        DimBlockPos pos = new DimBlockPos(event.getEntity().level().dimension(), event.getPos());
        ProtectionConfig config = getProtections(event.getEntity().getUUID(), pos);

        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!config.INTERACT) {
            if (!Content.isClaimBlock(block) && !config.BLOCK_INTERACT_WHITELIST.contains(block)) {
                event.setCanceled(true);
            }
        } else {
            if (config.BLOCK_INTERACT_BLACKLIST.contains(block))
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide())
            return;

        if (isOpOverride(event.getEntity()))
            return;

        if (Stacks.isFood(event.getItemStack()))
            return;

        DimBlockPos pos = new DimBlockPos(event.getEntity().level().dimension(), event.getPos());
        ProtectionConfig config = getProtections(event.getEntity().getUUID(), pos);

        Item usedItem = event.getItemStack().getItem();
        if (!config.USE_ITEM) {
            if (!config.ITEM_USE_WHITELIST.contains(usedItem))
                event.setCanceled(true);
        } else {
            if (config.ITEM_USE_BLACKLIST.contains(usedItem))
                event.setCanceled(true);
        }
    }

    // absence from the warzone starts the abandon timer
    private static void handleWarzoneBoundary(ServerPlayer player, SectionPos oldPos, SectionPos newPos) {
        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            return;
        }
        ResourceKey<Level> dim = player.level().dimension();
        DimChunkPos from = new DimChunkPos(dim, oldPos.x(), oldPos.z());
        DimChunkPos to = new DimChunkPos(dim, newPos.x(), newPos.z());
        if (WarForgeMod.FACTIONS.sieges.isInOwnSiegeWarzone(faction.getUuid(), from)
                && !WarForgeMod.FACTIONS.sieges.isInOwnSiegeWarzone(faction.getUuid(), to)) {
            Notifications.notifyPlayer(player, "warforge.leaving_warzone",
                    Component.translatable("warforge.common.leaving_warzone.title"),
                    Component.translatable("warforge.common.leaving_warzone.body"), PacketClientNotification.COLOR_WARNING, 6000);
        }
    }

    private static void notifyConqueredEntry(ServerPlayer player, SectionPos oldPos, SectionPos newPos) {
        ResourceKey<Level> dim = player.level().dimension();
        DimChunkPos to = new DimChunkPos(dim, newPos.x(), newPos.z());
        if (!WarForgeMod.FACTIONS.conquered.isConqueredWilderness(to, WarForgeMod.FACTIONS.claims)) {
            return;
        }
        DimChunkPos from = new DimChunkPos(dim, oldPos.x(), oldPos.z());
        if (WarForgeMod.FACTIONS.conquered.isConqueredWilderness(from, WarForgeMod.FACTIONS.claims)) {
            return; // already inside conquered land; only notify on the way in
        }
        Notifications.notifyPlayer(player, "warforge.entered_conquered",
                Component.translatable("warforge.common.entered_conquered.title"),
                Component.translatable("warforge.common.entered_conquered.body", TimeHelper.formatTime(WarForgeMod.FACTIONS.conquered.getRemainingMs(to))),
                PacketClientNotification.COLOR_WARNING, 6000);
    }

    @SubscribeEvent
    public void onMobSpawn(PotentialSpawns event) {
        ResourceKey<Level> dim = ((Level) event.getLevel()).dimension();
        ProtectionConfig config = getProtections(FactionIds.NONE, new DimBlockPos(dim, event.getPos()));
        if (!config.ALLOW_MOB_SPAWNS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void livingUpdate(EnteringSection event) {
        if (!event.didChunkChange() || event.getEntity().level().isClientSide())
            return;

        if (event.getEntity() instanceof ServerPlayer player) {
            WarForgeMod.FACTIONS.sieges.updateAttackerPresence(player);
            handleWarzoneBoundary(player, event.getOldPos(), event.getNewPos());
            notifyConqueredEntry(player, event.getOldPos(), event.getNewPos());
            return;
        }

        if (!inLoop) {
            if (!(event.getEntity() instanceof Player)) {
                Entity entity = event.getEntity();
                ProtectionConfig config = getProtections(FactionIds.NONE, new DimBlockPos(entity.level().dimension(), entity.blockPosition()));
                if (!config.ALLOW_MOB_ENTRY) {
                    inLoop = true;
                    boolean wasNoClip = entity.noPhysics;
                    entity.noPhysics = true;
                    SectionPos oldPos = event.getOldPos();
                    SectionPos newPos = event.getNewPos();
                    entity.move(MoverType.SELF, new Vec3((oldPos.x() - newPos.x()), 0d, (oldPos.z() - newPos.z())));
                    entity.noPhysics = wasNoClip;
                    inLoop = false;
                }
            }
        }
    }
}
