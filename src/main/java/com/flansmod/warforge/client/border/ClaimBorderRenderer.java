package com.flansmod.warforge.client.border;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.BorderSyncScheduler;
import com.flansmod.warforge.client.ClientBorderCache;
import com.flansmod.warforge.client.render.BorderGpu;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Levels;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;

import java.util.UUID;

public final class ClaimBorderRenderer {
    private static final int MAX_REBUILDS_PER_FRAME = 8;
    private static final int TERRAIN_REFRESH_INTERVAL_TICKS = 5;
    private static final int CHUNK_BLOCKS = 16;
    private static final int CHUNK_CENTER = 8;

    private static final ResourceLocation TEXTURE = Ids.of(Tags.MODID, "world/borders.png");
    private static final ResourceLocation TEXTURE_CONQUERED = Ids.of(Tags.MODID, "world/borders_restricted.png");
    private static final ResourceLocation TEXTURE_FAST = Ids.of(Tags.MODID, "world/borders_fast.png");

    private static final Matrix4f LOCAL_MATRIX = new Matrix4f();
    private static final Matrix4f SCRATCH_MODEL_VIEW = new Matrix4f();
    private static final BorderMeshBuilder MESH_BUILDER = new BorderMeshBuilder();
    private static final LevelBlocks LEVEL_BLOCKS = new LevelBlocks();

    private static final Long2ObjectOpenHashMap<BorderRenderData> BY_CHUNK = new Long2ObjectOpenHashMap<>();
    /** Same entries as BY_CHUNK, plain-texture chunks first, for index iteration without iterator allocation. */
    private static final ObjectArrayList<BorderRenderData> DRAW_ORDER = new ObjectArrayList<>();

    private static boolean visible = true;
    private static ResourceKey<Level> renderDim;
    private static int refreshTicks;
    private static int refreshCursor;

    private ClaimBorderRenderer() {
    }

    public static boolean toggleVisible() {
        visible = !visible;
        return visible;
    }

    public static void setVisible(boolean show) {
        visible = show;
    }

    public static void reset() {
        for (int i = 0; i < DRAW_ORDER.size(); i++) {
            DRAW_ORDER.get(i).disposeMesh();
        }
        BY_CHUNK.clear();
        DRAW_ORDER.clear();
        renderDim = null;
        refreshCursor = 0;
        PlacementOverlayRenderer.reset();
    }

    public static void markTerrainChanged(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        if (dim.equals(renderDim)) {
            BorderRenderData data = BY_CHUNK.get(ClientBorderCache.key(chunkX, chunkZ));
            if (data != null) {
                data.dirty = true;
            }
        }
    }

    /** TODO: mark chunks from a client block-update hook instead (needs a mixin on every node). */
    public static void tick(Minecraft mc, int playerChunkX, int playerChunkZ) {
        int count = DRAW_ORDER.size();
        if (count == 0 || ++refreshTicks < TERRAIN_REFRESH_INTERVAL_TICKS) {
            return;
        }
        refreshTicks = 0;
        int range = BorderSyncScheduler.renderDistanceChunks(mc);
        for (int tries = 0; tries < count; tries++) {
            refreshCursor = (refreshCursor + 1) % count;
            BorderRenderData data = DRAW_ORDER.get(refreshCursor);
            if (Math.abs(data.x - playerChunkX) <= range && Math.abs(data.z - playerChunkZ) <= range) {
                data.dirty = true;
                return;
            }
        }
    }

    public static void render(Minecraft mc, Level world, Matrix4f viewMatrix, double camX, double camY, double camZ) {
        if (!visible) {
            return;
        }
        ResourceKey<Level> dim = world.dimension();
        if (ClientBorderCache.consumeChanged() || renderDim != null && !renderDim.equals(dim)) {
            syncFromCache(dim);
        }
        if (DRAW_ORDER.isEmpty()) {
            return;
        }

        double maxDist = BorderSyncScheduler.renderDistanceChunks(mc) * (double) CHUNK_BLOCKS;
        double maxDistSq = maxDist * maxDist;
        int rebuildBudget = MAX_REBUILDS_PER_FRAME;

        for (int i = 0; i < DRAW_ORDER.size(); i++) {
            BorderRenderData data = DRAW_ORDER.get(i);

            double dx = data.x * CHUNK_BLOCKS + CHUNK_CENTER - camX;
            double dz = data.z * CHUNK_BLOCKS + CHUNK_CENTER - camZ;
            if (dx * dx + dz * dz > maxDistSq) {
                continue;
            }

            if ((data.dirty || data.mesh == null && !data.empty) && rebuildBudget > 0) {
                rebuild(world, data);
                rebuildBudget--;
            }
            if (data.empty || data.mesh == null) {
                continue;
            }

            SCRATCH_MODEL_VIEW.set(viewMatrix)
                    .translate((float) (data.x * CHUNK_BLOCKS - camX), (float) -camY, (float) (data.z * CHUNK_BLOCKS - camZ));
            BorderGpu.draw(data.mesh, textureOf(data), SCRATCH_MODEL_VIEW);
        }
    }

    private static void syncFromCache(ResourceKey<Level> worldDim) {
        if (renderDim != null && !renderDim.equals(worldDim)) {
            for (int i = 0; i < DRAW_ORDER.size(); i++) {
                DRAW_ORDER.get(i).disposeMesh();
            }
            BY_CHUNK.clear();
        }
        renderDim = worldDim;

        var existing = BY_CHUNK.long2ObjectEntrySet().fastIterator();
        while (existing.hasNext()) {
            BorderRenderData data = existing.next().getValue();
            ClaimChunkInfo info = ClientBorderCache.get(worldDim, data.x, data.z);
            if (info == null || !info.hasVisibleOutline()) {
                data.disposeMesh();
                existing.remove();
                markNeighboursDirty(data.x, data.z);
            } else if (data.update(info)) {
                data.dirty = true;
                markNeighboursDirty(data.x, data.z);
            }
        }

        var incoming = ClientBorderCache.chunks(worldDim).long2ObjectEntrySet().fastIterator();
        while (incoming.hasNext()) {
            Long2ObjectMap.Entry<ClaimChunkInfo> entry = incoming.next();
            ClaimChunkInfo info = entry.getValue();
            if (info.hasVisibleOutline() && !BY_CHUNK.containsKey(entry.getLongKey())) {
                BY_CHUNK.put(entry.getLongKey(), new BorderRenderData(info));
                markNeighboursDirty(info.x, info.z);
            }
        }

        DRAW_ORDER.clear();
        for (BorderRenderData data : BY_CHUNK.values()) {
            if (data.style != ClaimChunkInfo.OUTLINE_CONQUERED) {
                DRAW_ORDER.add(data);
            }
        }
        for (BorderRenderData data : BY_CHUNK.values()) {
            if (data.style == ClaimChunkInfo.OUTLINE_CONQUERED) {
                DRAW_ORDER.add(data);
            }
        }
    }

    private static void markNeighboursDirty(int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BorderRenderData neighbour = BY_CHUNK.get(ClientBorderCache.key(x + dx, z + dz));
                if (neighbour != null) {
                    neighbour.dirty = true;
                }
            }
        }
    }

    private static void rebuild(Level world, BorderRenderData data) {
        int sides = sidesNeedingGeometry(data);
        int color = 0xFF000000 | data.colour & 0xFFFFFF;
        int minY = Levels.minY(world);
        int maxY = Levels.maxYExclusive(world);
        LEVEL_BLOCKS.level = world;
        BorderGpu.Mesh built = BorderGpu.build(data.mesh, buffer -> MESH_BUILDER.build(
                LEVEL_BLOCKS, minY, maxY, data.x * CHUNK_BLOCKS, data.z * CHUNK_BLOCKS, color, sides, buffer, LOCAL_MATRIX));
        LEVEL_BLOCKS.level = null;
        data.dirty = false;
        data.empty = built == null;
        if (built != null) {
            data.mesh = built;
        }
    }

    private static int sidesNeedingGeometry(BorderRenderData data) {
        int sides = BorderMeshBuilder.ALL_SIDES;
        sides = clearIfSameGroup(sides, data, 0, -1, BorderMeshBuilder.NORTH);
        sides = clearIfSameGroup(sides, data, 1, 0, BorderMeshBuilder.EAST);
        sides = clearIfSameGroup(sides, data, 0, 1, BorderMeshBuilder.SOUTH);
        sides = clearIfSameGroup(sides, data, -1, 0, BorderMeshBuilder.WEST);
        sides = clearIfSameGroup(sides, data, -1, -1, BorderMeshBuilder.NORTH_WEST);
        sides = clearIfSameGroup(sides, data, 1, -1, BorderMeshBuilder.NORTH_EAST);
        sides = clearIfSameGroup(sides, data, -1, 1, BorderMeshBuilder.SOUTH_WEST);
        sides = clearIfSameGroup(sides, data, 1, 1, BorderMeshBuilder.SOUTH_EAST);
        return sides;
    }

    private static int clearIfSameGroup(int sides, BorderRenderData data, int dx, int dz, int bit) {
        BorderRenderData neighbour = BY_CHUNK.get(ClientBorderCache.key(data.x + dx, data.z + dz));
        return neighbour != null && neighbour.factionId.equals(data.factionId) && neighbour.style == data.style
                ? sides & ~bit : sides;
    }

    private static ResourceLocation textureOf(BorderRenderData data) {
        if (data.style == ClaimChunkInfo.OUTLINE_CONQUERED) {
            return TEXTURE_CONQUERED;
        }
        return ClientCompat.fastGraphics() ? TEXTURE_FAST : TEXTURE;
    }

    private static final class BorderRenderData {
        final int x;
        final int z;
        UUID factionId;
        int colour;
        byte style;
        BorderGpu.Mesh mesh;
        boolean dirty = true;
        boolean empty;

        BorderRenderData(ClaimChunkInfo info) {
            x = info.x;
            z = info.z;
            factionId = info.outlineFactionId;
            colour = info.outlineColour;
            style = info.outlineStyle;
        }

        boolean update(ClaimChunkInfo info) {
            boolean changed = !info.outlineFactionId.equals(factionId) || info.outlineColour != colour
                    || info.outlineStyle != style;
            factionId = info.outlineFactionId;
            colour = info.outlineColour;
            style = info.outlineStyle;
            return changed;
        }

        void disposeMesh() {
            if (mesh != null) {
                BorderGpu.close(mesh);
                mesh = null;
            }
        }
    }

    private static final class LevelBlocks implements BorderMeshBuilder.Blocks {
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        private Level level;

        @Override
        public boolean isAir(int x, int y, int z) {
            return level.isEmptyBlock(cursor.set(x, y, z));
        }

        @Override
        public int surfaceHeight(int x, int z) {
            return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        }
    }
}
