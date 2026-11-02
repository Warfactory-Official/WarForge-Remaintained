package com.flansmod.warforge.server;

import com.flansmod.warforge.api.ObjectIntPair;
import com.flansmod.warforge.api.WarforgeAPI;
import com.flansmod.warforge.common.ProtectionProfile;
import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.PacketClaimChunksData;
import com.flansmod.warforge.common.network.PacketTerrainColors;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Levels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.UUID;

public final class ClaimMapView {
    /**
     * Unclaimed chunks this close to the window centre also carry the claim permission, so the placement overlay
     * can tell valid chunks from invalid ones.
     */
    private static final int PLACEMENT_PROBE_RADIUS = 1;
    /** Chunks an outline window centre may lag behind the player's chunk. */
    private static final int OUTLINE_CENTER_SLACK = 2;

    private ClaimMapView() {
    }

    /** CITADEL gives away a target, so outsiders are told BASIC; members of the owning faction and ops see the real type. */
    private static ClaimType visibleClaimType(Faction owner, DimChunkPos chunk, ServerPlayer viewer, boolean viewerIsOp) {
        ClaimType actual = owner.getClaimType(chunk);
        if (actual != ClaimType.CITADEL || viewerIsOp || owner.isPlayerInFaction(viewer.getUUID())) {
            return actual;
        }
        return ClaimType.BASIC;
    }

    public static void send(ServerPlayer player, DimChunkPos center, int radius) {
        send(player, center, radius, false);
    }

    public static void send(ServerPlayer player, DimChunkPos center, int radius, boolean outlineOnly) {
        WarForgeMod.NETWORK.sendTo(create(player, center, radius, outlineOnly), player);
    }

    public static PacketClaimChunksData create(ServerPlayer player, DimChunkPos center, int radius, boolean outlineOnly) {
        FactionStorage hub = WarForgeMod.FACTIONS;
        int maxRadius = outlineOnly ? WarForgeConfig.BORDER_SYNC_MAX_RADIUS : WarForgeConfig.CLAIM_MANAGER_RADIUS;
        int clampedRadius = Math.max(1, Math.min(radius, maxRadius));
        center = boundCenter(player, center, outlineOnly ? OUTLINE_CENTER_SLACK : WarForgeConfig.SIEGE_DECLARE_MAX_RANGE);

        Faction faction = hub.registry.getFactionOfPlayer(player.getUUID());
        boolean viewerIsOp = WarForgeMod.isOp(player);
        boolean canManage = faction != null && (viewerIsOp || faction.isPlayerRoleInFaction(player.getUUID(), Role.OFFICER));
        ClaimPolicy.IslandCache islandCache = canManage ? hub.claimPolicy.islandCache(faction) : null;
        boolean opOverride = ProtectionsModule.isOpOverride(player);

        PacketClaimChunksData packet = new PacketClaimChunksData();
        packet.dim = center.dim;
        packet.centerX = center.x;
        packet.centerZ = center.z;
        packet.radius = clampedRadius;
        packet.outlineOnly = outlineOnly;
        packet.playerFactionId = faction == null ? FactionIds.NONE : faction.getUuid();
        packet.forceLoadedCount = faction == null ? 0 : faction.getForcedChunks().size();
        packet.forceLoadedMax = faction == null ? 0 : faction.getMaxForceLoadedChunks();
        packet.claimCount = faction == null ? 0 : faction.getClaimCount();
        packet.claimMax = faction == null ? 0 : WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(faction.getCitadelLevel());
        if (packet.claimMax < 0) {
            packet.claimMax = Short.MAX_VALUE;
        }

        for (int x = center.x - clampedRadius; x <= center.x + clampedRadius; x++) {
            for (int z = center.z - clampedRadius; z <= center.z + clampedRadius; z++) {
                DimChunkPos chunk = new DimChunkPos(center.dim, x, z);
                Faction ownerFaction = hub.registry.getFaction(hub.claims.getClaim(chunk));
                if (ownerFaction != null && ownerFaction.getSpecificPosForClaim(chunk) == null) {
                    continue;
                }
                ObjectIntPair<UUID> conqueredInfo = hub.conquered.entries().get(chunk);
                boolean probe = outlineOnly && canManage && ownerFaction == null
                        && Math.abs(x - center.x) <= PLACEMENT_PROBE_RADIUS && Math.abs(z - center.z) <= PLACEMENT_PROBE_RADIUS;
                if (outlineOnly && ownerFaction == null && conqueredInfo == null && !probe) {
                    continue;
                }

                ClaimChunkInfo info = new ClaimChunkInfo();
                info.x = x;
                info.z = z;
                info.protection = opOverride ? ProtectionProfile.UNRESTRICTED
                        : ProtectionProfile.of(ProtectionsModule.getProtections(player.getUUID(), chunk));
                if (ownerFaction != null) {
                    info.factionId = ownerFaction.getUuid();
                    info.factionName = ownerFaction.getName();
                    info.flagId = ownerFaction.getFlagId();
                    info.colour = ownerFaction.getColour();
                    info.claimType = visibleClaimType(ownerFaction, chunk, player, viewerIsOp);
                }

                if (conqueredInfo != null) {
                    Faction conqueredFaction = hub.registry.getFaction(conqueredInfo.getObj());
                    if (conqueredFaction != null) {
                        info.outlineFactionId = conqueredFaction.getUuid();
                        info.outlineColour = conqueredFaction.getColour();
                        info.outlineStyle = ClaimChunkInfo.OUTLINE_CONQUERED;
                    }
                    // only an unclaimed conquered chunk is reverting no-man's-land; a claimed one just carries a re-siege cooldown
                    if (ownerFaction == null) {
                        info.conqueredRemainingMs = conqueredInfo.getInteger();
                    }
                }
                if (!info.hasVisibleOutline() && ownerFaction != null) {
                    info.outlineFactionId = ownerFaction.getUuid();
                    info.outlineColour = ownerFaction.getColour();
                    info.outlineStyle = ClaimChunkInfo.OUTLINE_CLAIM;
                }

                boolean mayKnowVein = ownerFaction == null || WarforgeAPI.maySeeFactionContent(player.getUUID(), ownerFaction.getUuid());
                if (!outlineOnly && mayKnowVein) {
                    var veinInfo = WarForgeMod.VEIN_HANDLER.getVein(chunk.dim, chunk.x, chunk.z, WarForgeMod.MC_SERVER.overworld().getSeed());
                    if (veinInfo != null) {
                        info.vein = veinInfo.getLeft();
                        info.oreQuality = veinInfo.getRight();
                    }
                    addOwnerFlags(info, faction, ownerFaction, chunk);
                }
                if (canManage && (!outlineOnly || probe)) {
                    addManageFlags(info, player, faction, ownerFaction, chunk, islandCache, hub);
                }

                packet.chunks.add(info);
            }
        }
        return packet;
    }

    /** The requested centre, kept in the player's dimension and within maxOffset chunks of the player's own chunk. */
    private static DimChunkPos boundCenter(ServerPlayer player, DimChunkPos requested, int maxOffset) {
        int playerX = player.blockPosition().getX() >> 4;
        int playerZ = player.blockPosition().getZ() >> 4;
        int x = Math.max(playerX - maxOffset, Math.min(playerX + maxOffset, requested.x));
        int z = Math.max(playerZ - maxOffset, Math.min(playerZ + maxOffset, requested.z));
        return new DimChunkPos(player.level().dimension(), x, z);
    }

    private static void addOwnerFlags(ClaimChunkInfo info, Faction faction, Faction ownerFaction, DimChunkPos chunk) {
        if (faction == null || ownerFaction == null || !ownerFaction.getUuid().equals(faction.getUuid())) {
            return;
        }
        info.flags |= ClaimChunkInfo.FLAG_OWNED_BY_PLAYER;
        if (faction.isForceLoaded(chunk)) {
            info.flags |= ClaimChunkInfo.FLAG_FORCE_LOADED;
        }
        for (DimBlockPos collectorPos : faction.getIslandCollectors()) {
            if (collectorPos.toChunkPos().equals(chunk)) {
                info.flags |= ClaimChunkInfo.FLAG_HAS_COLLECTOR;
                break;
            }
        }
    }

    private static void addManageFlags(ClaimChunkInfo info, ServerPlayer player, Faction faction, Faction ownerFaction,
                                       DimChunkPos chunk, ClaimPolicy.IslandCache islandCache, FactionStorage hub) {
        if (ownerFaction == null) {
            if (hub.claimPolicy.canClaim(player, faction, chunk, islandCache)) {
                info.flags |= ClaimChunkInfo.FLAG_CAN_CLAIM;
            }
        } else if (ownerFaction.getUuid().equals(faction.getUuid())) {
            DimBlockPos claimPos = faction.getSpecificPosForClaim(chunk);
            if (claimPos != null && !claimPos.equals(faction.getCitadelPos()) && !hub.sieges.isSiegeInProgress(chunk)) {
                info.flags |= ClaimChunkInfo.FLAG_CAN_UNCLAIM;
            }
            info.flags |= ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD;
        }
    }

    /**
     * Ships per-block map colours and heights of server-loaded chunks so the claim map can show terrain the
     * client has not loaded. Never force-loads: a client must not be able to trigger chunk generation.
     */
    public static void sendTerrainColors(ServerPlayer player, DimChunkPos center, int radius) {
        radius = Math.max(1, Math.min(radius, WarForgeConfig.CLAIM_MANAGER_RADIUS));
        center = boundCenter(player, center, WarForgeConfig.SIEGE_DECLARE_MAX_RANGE);
        ServerLevel world = WarForgeMod.MC_SERVER.getLevel(center.dim);
        if (world == null) return;

        PacketTerrainColors packet = new PacketTerrainColors();
        packet.dim = center.dim;
        packet.centerX = center.x;
        packet.centerZ = center.z;
        packet.radius = radius;

        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                LevelChunk chunk = world.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                int[] colors = new int[256];
                int[] heights = new int[256];
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        int idx = lx + lz * 16;
                        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
                        heights[idx] = y;
                        if (y <= Levels.minY(world)) {
                            colors[idx] = 0;
                            continue;
                        }
                        // WORLD_SURFACE height is the topmost solid block's Y: sample at y, not y-1
                        BlockPos worldPos = new BlockPos((cx << 4) + lx, y, (cz << 4) + lz);
                        colors[idx] = chunk.getBlockState(worldPos).getMapColor(world, worldPos).col & 0x00FFFFFF;
                    }
                }
                packet.addChunk(cx, cz, colors, heights);
            }
        }
        WarForgeMod.NETWORK.sendTo(packet, player);
    }
}
