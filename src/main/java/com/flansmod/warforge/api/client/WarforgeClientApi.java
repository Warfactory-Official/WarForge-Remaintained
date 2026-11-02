package com.flansmod.warforge.api.client;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.ClientClaimChunkCache;
import com.flansmod.warforge.client.PlayerNametagCache;
import com.flansmod.warforge.client.map.ChunkMapTextureDaemon;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.PacketRequestTerrainColors;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import javax.annotation.Nullable;
import java.util.UUID;

/** Client-only addon surface; never load on a dedicated server. Render thread only. */
public final class WarforgeClientApi {
    private static volatile NameplatePolicy nameplatePolicy = entity -> NameplatePolicy.Mode.SEE_THROUGH;

    private WarforgeClientApi() {}

    /** Faction display name of a player; null when unknown yet (a request is sent) or factionless. */
    @Nullable
    public static String factionNameOf(UUID player) {
        Player entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getPlayerByUUID(player);
        return entity == null ? null : factionNameOf(entity);
    }

    @Nullable
    public static String factionNameOf(Player player) {
        PlayerNametagCache.NamePlateData data = PlayerNametagCache.INSTANCE.requestIfAbsent(player.getScoreboardName());
        return data == null ? null : data.name;
    }

    @Nullable
    public static UUID ownFactionId() {
        UUID id = ClientClaimChunkCache.playerFactionId();
        return FactionIds.isValid(id) ? id : null;
    }

    /** Another player of the local player's faction. */
    public static boolean isTeammate(UUID player) {
        Player entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getPlayerByUUID(player);
        return entity != null && isTeammate(entity);
    }

    /** Per-frame safe: no allocation, no player scan. */
    public static boolean isTeammate(Player player) {
        Player self = Minecraft.getInstance().player;
        if (self == null || self == player || ownFactionId() == null) {
            return false;
        }
        String own = factionNameOf(self);
        return own != null && own.equals(factionNameOf(player));
    }

    /** Replaces the default (every faction name plate drawn see-through). Consulted per name plate per frame. */
    public static void setNameplatePolicy(NameplatePolicy policy) {
        nameplatePolicy = policy;
    }

    /** For the name plate renderers. */
    public static NameplatePolicy.Mode nameplateMode(Entity entity) {
        return nameplatePolicy.modeFor(entity);
    }

    /** Requests the terrain textures of the square around the chunk and the server terrain data for unloaded chunks. */
    public static void requestChunkMap(String namespace, ResourceKey<Level> dim, int centerX, int centerZ, int radius) {
        ChunkMapTextureDaemon.requestMapUpdate(namespace, dim, centerX, centerZ, radius, new Long2IntOpenHashMap());
        PacketRequestTerrainColors request = new PacketRequestTerrainColors();
        request.center = new DimChunkPos(dim, centerX, centerZ);
        request.radius = radius;
        WarForgeMod.NETWORK.sendToServer(request);
    }

    /** Rebuilds the last request of the namespace, e.g. after new server terrain data arrived. */
    public static void rebuildChunkMap(String namespace) {
        ChunkMapTextureDaemon.rebuildLast(namespace);
    }

    public static ResourceLocation chunkMapTexture(String namespace, ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return Ids.of(Tags.MODID, ChunkMapTextureDaemon.getTextureName(namespace, dim, chunkX, chunkZ));
    }

    /** Frees the textures of the namespace. */
    public static void releaseChunkMap(String namespace) {
        ChunkMapTextureDaemon.releaseNamespace(namespace);
    }
}
