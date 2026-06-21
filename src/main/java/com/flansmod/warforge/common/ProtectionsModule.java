package com.flansmod.warforge.common;

import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionStorage;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityEvent.EnteringSection;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent.PotentialSpawns;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

public class ProtectionsModule {
    public static boolean OP_OVERRIDE = false;
    private static boolean inLoop = false;

    public ProtectionsModule() {
    }

    @Nonnull
    public static ProtectionConfig GetProtections(UUID playerID, DimBlockPos pos) {
        return GetProtections(playerID, pos.toChunkPos());
    }

    // It is generally expected that you are asking about a loaded chunk, not that that should matter
    @Nonnull
    public static ProtectionConfig GetProtections(UUID playerID, DimChunkPos pos) {
        FactionStorage.SiegeZoneRelation siegeRelation = WarForgeMod.FACTIONS.getSiegeZoneRelation(playerID, pos);

        UUID factionID = WarForgeMod.FACTIONS.getClaim(pos);
        if (factionID.equals(FactionStorage.SAFE_ZONE_ID))
            return WarForgeConfig.SAFE_ZONE;

        if (factionID.equals(FactionStorage.WAR_ZONE_ID))
            return WarForgeConfig.WAR_ZONE;

        Faction faction = WarForgeMod.FACTIONS.getFaction(factionID);
        if (faction != null) {
            boolean playerIsInFaction = playerID != null && !playerID.equals(Faction.nullUuid) && faction.isPlayerInFaction(playerID);
            Faction.ClaimType claimType = faction.getClaimType(pos);

            // A faction's own siege-camp claim should still behave like a friendly claim for its members.
            if (playerIsInFaction && claimType == Faction.ClaimType.SIEGE)
                return WarForgeConfig.CLAIM_FRIEND;
        }

        if (siegeRelation == FactionStorage.SiegeZoneRelation.ATTACKER) {
            return WarForgeConfig.SIEGECAMP_SIEGER;
        }

        if (faction != null) {
            boolean playerIsInFaction = playerID != null && !playerID.equals(Faction.nullUuid) && faction.isPlayerInFaction(playerID);
            if (playerIsInFaction && siegeRelation == FactionStorage.SiegeZoneRelation.DEFENDER)
                return WarForgeConfig.CLAIM_DEFENDED;

            if (faction.citadelPos.toChunkPos().equals(pos))
                return playerIsInFaction ? WarForgeConfig.CITADEL_FRIEND : WarForgeConfig.CITADEL_FOE;

            if (playerIsInFaction)
                return WarForgeConfig.CLAIM_FRIEND;

            // Allies of the owning faction get the CLAIM_ALLY profile, but only when that faction has
            // enabled ally interaction. Otherwise allies are treated like any other foreign player.
            if (faction.allowAllyInteraction && playerID != null && !playerID.equals(Faction.nullUuid)) {
                Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(playerID);
                if (playerFaction != null && faction.isAllyOf(playerFaction.uuid)) {
                    return WarForgeConfig.CLAIM_ALLY;
                }
            }

            return WarForgeConfig.CLAIM_FOE;
        }

        return WarForgeConfig.UNCLAIMED;
    }

    public void UpdateServer() {
		/*
		 * oof pistons are a pain
		for(World world: WarForgeMod.MC_SERVER.worlds)
		{
			ArrayList<TileEntityPiston> list = new ArrayList<TileEntityPiston>();

			for(TileEntity te : world.loadedTileEntityList)
			{
				if(te instanceof TileEntityPiston)
				{
					list.add( (TileEntityPiston)te);
				}
			}

			for(TileEntityPiston piston : list)
			{
				NBTTagCompound tags = new NBTTagCompound();
				piston.writeToNBT(tags);
				tags.setBoolean("extending", !tags.getBoolean("extending"));
				piston.readFromNBT(tags);

				piston.clearPistonTileEntity();
			}
		}
		*/

    }

    @SubscribeEvent
    public void OnDismount(EntityMountEvent event) {
        if (event.getEntityMounting().level().isClientSide)
            return;

        if (event.getEntityMounting() instanceof Player) {
            Entity vehicle = event.getEntityBeingMounted();
            DimBlockPos vehiclePos = new DimBlockPos(vehicle.level().dimension(), vehicle.blockPosition());
            ProtectionConfig mountConfig = GetProtections(event.getEntityMounting().getUUID(), vehiclePos);

            if (event.isMounting() && !mountConfig.ALLOW_MOUNT_ENTITY)
                event.setCanceled(true);

            if (event.isDismounting() && !mountConfig.ALLOW_DISMOUNT_ENTITY)
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void OnExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide)
            return;

        // Check each pos, but keep a cache of configs so we don't do like 300 lookups
        var dim = event.getLevel().dimension();
        HashMap<DimChunkPos, ProtectionConfig> checkedPositions = new HashMap<DimChunkPos, ProtectionConfig>();

        for (int i = event.getAffectedBlocks().size() - 1; i >= 0; i--) {
            DimChunkPos cPos = new DimChunkPos(dim, event.getAffectedBlocks().get(i));
            if (!checkedPositions.containsKey(cPos)) {
                ProtectionConfig config = GetProtections(Faction.nullUuid, cPos);
                checkedPositions.put(cPos, config);
            }
            if (!checkedPositions.get(cPos).EXPLOSION_DAMAGE || !checkedPositions.get(cPos).BLOCK_REMOVAL) {
                //WarForgeMod.LOGGER.info("Protected block position from explosion");
                event.getAffectedBlocks().remove(i);
            }
        }
    }

    @SubscribeEvent
    public void OnDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide)
            return;

        LivingEntity victim = event.getEntity();
        DimBlockPos damagedPos = new DimBlockPos(victim.level().dimension(), victim.blockPosition());
        ProtectionConfig damagedConfig = GetProtections(victim.getUUID(), damagedPos);

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker != null) {
            if (attacker instanceof Player && victim instanceof Player) {
                // Factions in a post-alliance truce cannot harm each other for the truce's duration.
                Faction attackerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(attacker.getUUID());
                Faction victimFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(victim.getUUID());
                if (attackerFaction != null && victimFaction != null
                        && !attackerFaction.uuid.equals(victimFaction.uuid)
                        && attackerFaction.isInTruceWith(victimFaction.uuid)) {
                    event.setCanceled(true);
                    return;
                }
            }
            if (attacker instanceof Player) {
                if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_PLAYER) {
                    event.setCanceled(true);
                    //WarForgeMod.LOGGER.info("Cancelled damage event from other player because we are in a safe zone");
                    return;
                }

                DimBlockPos attackerPos = new DimBlockPos(attacker.level().dimension(), attacker.blockPosition());
                ProtectionConfig attackerConfig = GetProtections(attacker.getUUID(), attackerPos);

                if (!attackerConfig.PLAYER_DEAL_DAMAGE) {
                    event.setCanceled(true);
                    //WarForgeMod.LOGGER.info("Cancelled damage event from player because they were in a safe zone");
                    return;
                }
            } else if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_MOB) {
                event.setCanceled(true);
                //WarForgeMod.LOGGER.info("Cancelled damage event from mob");
                return;
            }
        } else {
            if (!damagedConfig.PLAYER_TAKE_DAMAGE_FROM_OTHER) {
                event.setCanceled(true);
                //WarForgeMod.LOGGER.info("Cancelled damage event from other source");
                return;
            }
        }
    }

    // might be useful at some point
    @SubscribeEvent
    public void EntityPlaced(BlockEvent.EntityMultiPlaceEvent event) {
        if (event.getLevel().isClientSide()) { return; }
        WarForgeMod.LOGGER.atDebug().log("Multi Place Event: " + event);
    }

    // TODO: Make the protections module properly handle mekanism place events where the placer is actually null
    // is called twice for mekanism cables for some reason; first call has null entity, second has placer (which might be null)
    @SubscribeEvent
    public void BlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide())
            return;

        Entity eventEntity = event.getEntity();

        if (OP_OVERRIDE && eventEntity instanceof Player && WarForgeMod.isOp((Player) eventEntity))
            return;

        // best effort compat with mekanism
        Block placedBlock = event.getPlacedBlock().getBlock();
        var blockId = ForgeRegistries.BLOCKS.getKey(placedBlock);
        if (blockId == null) { WarForgeMod.LOGGER.atDebug().log("Could not get id of block placed in event: " + event); }
        else if (blockId.getNamespace().equals("mekanism") && eventEntity == null) { return; }  // ignore mek place w/ null entity

        if (eventEntity == null) {
            WarForgeMod.LOGGER.atError().log("Detected null entity for event with detals: pos - " + event.getPos() + "; world - " + event.getLevel() + ";");
            event.setCanceled(true);
            return;
        }

        DimBlockPos pos = new DimBlockPos(eventEntity.level().dimension(), event.getPos());
        ProtectionConfig config = GetProtections(eventEntity.getUUID(), pos);

        if (!config.PLACE_BLOCKS) {
            if (!config.BLOCK_PLACE_WHITELIST.contains(placedBlock)) {
                //WarForgeMod.LOGGER.info("Cancelled block placement event");
                event.setCanceled(true);
            }
        } else {
            if (config.BLOCK_PLACE_BLACKLIST.contains(placedBlock)) {
                //WarForgeMod.LOGGER.info("Cancelled block placement event");
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void BlockRemoved(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide())
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getPlayer()))
            return;

        DimBlockPos pos = new DimBlockPos(event.getPlayer().level().dimension(), event.getPos());
        ProtectionConfig config = GetProtections(event.getPlayer().getUUID(), pos);

        if (!config.BREAK_BLOCKS || !config.BLOCK_REMOVAL) {
            if (!config.BLOCK_BREAK_WHITELIST.contains(event.getState().getBlock())) {
                event.setCanceled(true);
            }
        } else {
            if (config.BLOCK_BREAK_BLACKLIST.contains(event.getState().getBlock())) {
                event.setCanceled(true);
            }
        }

    }

    @SubscribeEvent
    public void OnPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide)
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getEntity()))
            return;

        Entity target = event.getTarget();
        DimBlockPos pos = new DimBlockPos(target.level().dimension(), target.blockPosition());
        ProtectionConfig config = GetProtections(event.getEntity().getUUID(), pos);

        if (!config.INTERACT) {
            //WarForgeMod.LOGGER.info("Cancelled interact event");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void OnPlayerRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide)
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getEntity()))
            return;

        DimBlockPos pos = new DimBlockPos(event.getEntity().level().dimension(), event.getPos());
        ProtectionConfig config = GetProtections(event.getEntity().getUUID(), pos);


        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!config.INTERACT) {
            if (block != WarForgeMod.CONTENT.citadelBlock
                    && block != WarForgeMod.CONTENT.basicClaimBlock
                    && block != WarForgeMod.CONTENT.reinforcedClaimBlock
                    && block != WarForgeMod.CONTENT.dummyTranslusent
                    && block != WarForgeMod.CONTENT.statue
                    && !config.BLOCK_INTERACT_WHITELIST.contains(block)) {
                //WarForgeMod.LOGGER.info("Cancelled item use event while looking at block");
                event.setCanceled(true);
            }
        } else {
            if (config.BLOCK_INTERACT_BLACKLIST.contains(block))
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void OnPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide)
            return;

        if (OP_OVERRIDE && WarForgeMod.isOp(event.getEntity()))
            return;

        // Always allow food
        if (event.getItemStack().getItem().isEdible())
            return;

        DimBlockPos pos = new DimBlockPos(event.getEntity().level().dimension(), event.getPos());
        ProtectionConfig config = GetProtections(event.getEntity().getUUID(), pos);

        Item usedItem = event.getItemStack().getItem();
        if (!config.USE_ITEM) {
            if (!config.ITEM_USE_WHITELIST.contains(usedItem))
                event.setCanceled(true);
        } else {
            if (config.ITEM_USE_BLACKLIST.contains(usedItem))
                event.setCanceled(true);

        }
    }

    @SubscribeEvent
    public void OnMobSpawn(PotentialSpawns event) {
        ResourceKey<Level> dim = ((Level) event.getLevel()).dimension();
        ProtectionConfig config = GetProtections(Faction.nullUuid, new DimBlockPos(dim, event.getPos()));
        if (!config.ALLOW_MOB_SPAWNS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void LivingUpdate(EnteringSection event) {
        if (!event.didChunkChange())
            return;

        if (!inLoop) {
            if (!(event.getEntity() instanceof Player)) {
                Entity entity = event.getEntity();
                ProtectionConfig config = GetProtections(Faction.nullUuid, new DimBlockPos(entity.level().dimension(), entity.blockPosition()));
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
