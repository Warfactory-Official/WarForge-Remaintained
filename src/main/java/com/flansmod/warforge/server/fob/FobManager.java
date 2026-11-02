package com.flansmod.warforge.server.fob;

import com.flansmod.warforge.api.FobWarpVeto;
import com.flansmod.warforge.api.WarforgeAPI;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.BlockFob;
import com.flansmod.warforge.common.blocks.FobBlockEntity;
import com.flansmod.warforge.common.blocks.structure.FobStructureLayout;
import com.flansmod.warforge.common.blocks.structure.StructureStamper;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Nbt;
import com.flansmod.warforge.server.ClaimPolicy;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.Siege;
import com.flansmod.warforge.server.SiegeService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

/** Owns every FOB: per-faction lists, the chunk index, the deferred block cleanup queue and warps. */
public class FobManager {
    private static final long TICK_MS = 50L;
    private static final long MIN_HOLD_TICKS = 20L;

    private final HashMap<UUID, ArrayList<Fob>> fobsByFaction = new HashMap<>();
    private final HashMap<DimChunkPos, Fob> fobByChunk = new HashMap<>();
    private final FobWarpQueue warpQueue = new FobWarpQueue();
    private final List<DimBlockPos> pendingCleanup = new ArrayList<>();

    public FobWarpQueue getWarpQueue() {
        return warpQueue;
    }

    public Fob getFobAt(DimChunkPos chunk) {
        return fobByChunk.get(chunk);
    }

    public boolean isNearActiveFob(DimChunkPos chunk, int radius) {
        if (fobByChunk.isEmpty() || radius < 0) {
            return false;
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (fobByChunk.containsKey(new DimChunkPos(chunk.dim, chunk.x + dx, chunk.z + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Fob> fobsOf(Faction faction) {
        ArrayList<Fob> fobs = faction == null ? null : fobsByFaction.get(faction.getUuid());
        return fobs == null ? List.of() : Collections.unmodifiableList(fobs);
    }

    public boolean hasFobCapacity(Faction faction) {
        return faction != null && fobsOf(faction).size() < faction.getMaxFobs();
    }

    /** Denial message for placing a FOB in {@code chunk}, or null when allowed. */
    public Component checkPlacement(Faction owner, DimChunkPos chunk) {
        WarForgeMod.FACTIONS.claims.pruneStaleClaim(chunk);
        if (WarForgeMod.FACTIONS.claims.hasClaimWithin(chunk, WarForgeConfig.FOB_CLAIM_EXCLUSION_RADIUS)) {
            return t("fob.near_claim", WarForgeConfig.FOB_CLAIM_EXCLUSION_RADIUS);
        }
        if (WarForgeMod.FACTIONS.sieges.isChunkContested(chunk)) {
            return t("claim.contested");
        }
        if (!ClaimPolicy.isDimWhitelisted(chunk.dim)) {
            return t("claim.bad_dimension");
        }
        if (WarForgeMod.FACTIONS.conquered.isConquered(chunk)) {
            return t("fob.conquered");
        }
        if (fobByChunk.containsKey(chunk)) {
            return t("fob.exists");
        }
        Faction tooClose = WarForgeMod.FACTIONS.claimPolicy.findNearbyOpposingFaction(owner, chunk);
        if (tooClose != null) {
            return t("claim.too_close", WarForgeConfig.MIN_DISTANCE_BETWEEN_FACTIONS, tooClose.getName());
        }
        if (!hasFobCapacity(owner)) {
            return t(WarForgeConfig.ENABLE_CITADEL_UPGRADES ? "fob.limit_level" : "fob.limit");
        }
        return null;
    }

    public void clearFobs() {
        fobsByFaction.clear();
        fobByChunk.clear();
    }

    public void registerFob(Faction faction, Fob fob) {
        ArrayList<Fob> fobs = fobsByFaction.computeIfAbsent(faction.getUuid(), id -> new ArrayList<>());
        if (!fobs.contains(fob)) {
            fobs.add(fob);
        }
        fobByChunk.put(fob.toChunkPos(), fob);
    }

    public void removeFob(Fob fob) {
        ArrayList<Fob> fobs = fobsByFaction.get(fob.ownerFaction);
        if (fobs != null) {
            fobs.remove(fob);
        }
        fobByChunk.remove(fob.toChunkPos());
    }

    public void destroyFob(Fob fob) {
        removeFob(fob);
        scheduleOrCleanup(fob.pos);
    }

    /** Disband: destroys every FOB of the faction. */
    public void destroyFobsOf(UUID factionId) {
        ArrayList<Fob> fobs = fobsByFaction.get(factionId);
        if (fobs != null) {
            for (Fob fob : new ArrayList<>(fobs)) {
                destroyFob(fob);
            }
        }
        fobsByFaction.remove(factionId);
    }

    public boolean requestCreateFob(FobBlockEntity te, ServerPlayer placer, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        Faction owner = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(placer.getUUID());
        Level level = te.getLevel();
        if (owner == null || level == null || !owner.isPlayerRoleInFaction(placer.getUUID(), Role.OFFICER)) {
            return false;
        }
        BlockPos placedPos = te.getBlockPos();
        Component denial = checkPlacement(owner, new DimChunkPos(level.dimension(), placedPos));
        if (denial != null) {
            placer.sendSystemMessage(denial);
            return false;
        }

        BlockPos centerPos = FobStructureLayout.centerBlock(placedPos);
        FobBlockEntity centerTe = te;
        if (!centerPos.equals(placedPos)) {
            BlockState fobState = level.getBlockState(placedPos);
            level.removeBlock(placedPos, false);
            level.setBlock(centerPos, fobState, 3);
            if (!(level.getBlockEntity(centerPos) instanceof FobBlockEntity relocated)) {
                return false;
            }
            centerTe = relocated;
        }

        Fob fob = new Fob(new DimBlockPos(level.dimension(), centerPos), owner.getUuid(), name.trim());
        fob.maxTickets = owner.getFobTicketLimit();
        fob.tickets = fob.maxTickets;
        registerFob(owner, fob);
        StructureStamper.stampStructure(level, centerPos);

        centerTe.ownerFaction = owner.getUuid();
        centerTe.placer = placer.getUUID();
        centerTe.name = fob.name;
        centerTe.maxTickets = fob.maxTickets;
        centerTe.tickets = fob.tickets;
        centerTe.setChanged();
        return true;
    }

    public boolean requestFobWarp(ServerPlayer player, Fob fob) {
        if (warpQueue.hasPendingWarp(player.getUUID())) {
            return false;
        }
        Faction owner = WarForgeMod.FACTIONS.registry.getFaction(fob.ownerFaction);
        if (owner == null || !owner.isPlayerInFaction(player.getUUID())) {
            return false;
        }
        if (!WarForgeMod.FACTIONS.sieges.isFactionInActiveSiege(owner.getUuid())) {
            player.sendSystemMessage(t("fob.siege_only"));
            return false;
        }
        if (FobPresence.enemyHeld(fob)) {
            player.sendSystemMessage(t("fob.enemies_holding"));
            return false;
        }
        Component veto = WarforgeAPI.fobWarpVeto(player,
                new FobWarpVeto.Target(fob.ownerFaction, fob.pos.dim, fob.pos.toRegularPos(), fob.name));
        if (veto != null) {
            player.sendSystemMessage(veto);
            return false;
        }
        int cost = 1 + FobWarpQueue.vehicleExtraCost(player.getVehicle());
        if (fob.tickets < cost) {
            player.sendSystemMessage(t("fob.no_tickets"));
            return false;
        }
        fob.tickets -= cost;
        warpQueue.requestFobWarp(player, fob, cost);
        return true;
    }

    public void onDefendingSiegeEnded(UUID factionId) {
        ArrayList<Fob> fobs = fobsByFaction.get(factionId);
        if (fobs != null) {
            for (Fob fob : fobs) {
                fob.enemyHoldTicks = 0;
            }
        }
    }

    private void scheduleOrCleanup(DimBlockPos pos) {
        if (WarForgeMod.MC_SERVER == null) {
            return;
        }
        ServerLevel level = WarForgeMod.MC_SERVER.getLevel(pos.dim);
        DimChunkPos chunk = pos.toChunkPos();
        if (level != null && level.hasChunk(chunk.x, chunk.z)) {
            cleanupNow(level, pos);
        } else if (!pendingCleanup.contains(pos)) {
            pendingCleanup.add(pos);
        }
    }

    private static void cleanupNow(ServerLevel level, DimBlockPos pos) {
        StructureStamper.clearStructure(level, pos.toRegularPos());
        if (level.getBlockState(pos.toRegularPos()).getBlock() instanceof BlockFob) {
            level.removeBlock(pos.toRegularPos(), false);
        }
    }

    public void processCleanupQueue() {
        if (pendingCleanup.isEmpty() || WarForgeMod.MC_SERVER == null) {
            return;
        }
        Iterator<DimBlockPos> it = pendingCleanup.iterator();
        while (it.hasNext()) {
            DimBlockPos pos = it.next();
            ServerLevel level = WarForgeMod.MC_SERVER.getLevel(pos.dim);
            if (level == null) {
                it.remove();
            } else if (level.hasChunk(pos.toChunkPos().x, pos.toChunkPos().z)) {
                cleanupNow(level, pos);
                it.remove();
            }
        }
    }

    public void writeToNBT(CompoundTag tags) {
        ListTag list = new ListTag();
        for (DimBlockPos pos : pendingCleanup) {
            CompoundTag entry = new CompoundTag();
            pos.writeToNBT(entry, "pos");
            list.add(entry);
        }
        tags.put("fobCleanupQueue", list);
    }

    public void readFromNBT(CompoundTag tags) {
        pendingCleanup.clear();
        ListTag list = Nbt.getList(tags, "fobCleanupQueue", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            pendingCleanup.add(DimBlockPos.readFromNBT(Nbt.getCompound(list, i), "pos"));
        }
    }

    /** Faction compound key "fobs". */
    public void writeFactionFobs(UUID factionId, CompoundTag factionTags) {
        ListTag list = new ListTag();
        for (Fob fob : fobsByFaction.getOrDefault(factionId, new ArrayList<>())) {
            if (!fob.pos.equals(DimBlockPos.ZERO)) {
                CompoundTag fobTags = new CompoundTag();
                fob.writeToNBT(fobTags);
                list.add(fobTags);
            }
        }
        factionTags.put("fobs", list);
    }

    public void readFactionFobs(UUID factionId, CompoundTag factionTags) {
        for (Tag base : Nbt.getList(factionTags, "fobs", Tag.TAG_COMPOUND)) {
            Fob fob = new Fob();
            fob.readFromNBT((CompoundTag) base);
            fob.ownerFaction = factionId;
            if (!fob.pos.equals(DimBlockPos.ZERO)) {
                fobsByFaction.computeIfAbsent(factionId, id -> new ArrayList<>()).add(fob);
                fobByChunk.put(fob.toChunkPos(), fob);
            }
        }
    }

    /** Ticks an enemy must hold a FOB to destroy it; -1 disables hold-to-destroy. */
    public int getSiegeHoldThresholdTicks(Fob fob) {
        Siege siege = WarForgeMod.FACTIONS.sieges.getActiveSiegeForFaction(fob.ownerFaction);
        Faction attacker = siege == null ? null : WarForgeMod.FACTIONS.registry.getFaction(siege.attackingFaction);
        if (attacker == null) {
            return -1;
        }
        long millis = SiegeService.momentumMillis(attacker.getSiegeMomentum());
        return (int) Math.min(Integer.MAX_VALUE, Math.max(MIN_HOLD_TICKS, millis / TICK_MS));
    }

    public void onSiegeTimerReset(Siege siege) {
        creditRegen(WarForgeMod.FACTIONS.registry.getFaction(siege.attackingFaction));
        creditRegen(WarForgeMod.FACTIONS.registry.getFaction(siege.defendingFaction));
    }

    private void creditRegen(Faction faction) {
        if (faction == null) {
            return;
        }
        int regen = Math.max(0, faction.getFobRegenPerSiegeTick());
        int limit = faction.getFobTicketLimit();
        for (Fob fob : fobsOf(faction)) {
            fob.maxTickets = limit;
            fob.tickets = Math.min(limit, fob.tickets + regen);
        }
    }
}
