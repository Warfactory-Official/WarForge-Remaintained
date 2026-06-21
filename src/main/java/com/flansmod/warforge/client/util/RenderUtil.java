package com.flansmod.warforge.client.util;

import com.flansmod.warforge.common.util.DimChunkPos;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;

//Trying to move that crap out of tick handler
public class RenderUtil {

    public static void vertexAt(DimChunkPos chunkPos, Level world, Matrix4f matrix, VertexConsumer buffer, int color, int x, int z, double groundLevelBlend, double playerHeight) {
        double topHeight = playerHeight + 128;

        double maxHeight = world.getHeight(Heightmap.Types.MOTION_BLOCKING, chunkPos.x * 16 + x, chunkPos.z * 16 + z) + 8;
        if (maxHeight > playerHeight + 16) maxHeight = playerHeight + 16;

        double height = topHeight + (maxHeight - topHeight) * groundLevelBlend;

        buffer.vertex(matrix, x, (float) height, z).color(color).uv(z / 16f, x / 16f).endVertex();
    }

    public static void drawTexturedModalRect(Matrix4f matrix, VertexConsumer buffer, int color, int x, int y, float u, float v, int w, int h) {
        float texScale = 1f / 256f;

        buffer.vertex(matrix, x, y + h, -90f).color(color).uv(u * texScale, (v + h) * texScale).endVertex();
        buffer.vertex(matrix, x + w, y + h, -90f).color(color).uv((u + w) * texScale, (v + h) * texScale).endVertex();
        buffer.vertex(matrix, x + w, y, -90f).color(color).uv((u + w) * texScale, (v) * texScale).endVertex();
        buffer.vertex(matrix, x, y, -90f).color(color).uv(u * texScale, (v) * texScale).endVertex();
    }

    public static void renderZAlignedSquare(Matrix4f matrix, VertexConsumer buffer, int color, int x, int y, double z, int ori) {
        buffer.vertex(matrix, x, y, (float) z).color(color).uv(((ori) / 2) % 2, ((ori + 3) / 2) % 2).endVertex();
        buffer.vertex(matrix, x + 1, y, (float) z).color(color).uv(((ori + 1) / 2) % 2, ((ori) / 2) % 2).endVertex();
        buffer.vertex(matrix, x + 1, y + 1, (float) z).color(color).uv(((ori + 2) / 2) % 2, ((ori + 1) / 2) % 2).endVertex();
        buffer.vertex(matrix, x, y + 1, (float) z).color(color).uv(((ori + 3) / 2) % 2, ((ori + 2) / 2) % 2).endVertex();
    }

    public static void renderZAlignedRecangle(Matrix4f matrix, VertexConsumer buffer, int color, double x, int y, double z, int ori, double width) {
        buffer.vertex(matrix, (float) (x + 0 - width), y, (float) z).color(color).uv(((ori) / 2) % 2, ((ori + 3) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) (x + 1), y, (float) z).color(color).uv(((ori + 1) / 2) % 2, ((ori) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) (x + 1), y + 1, (float) z).color(color).uv(((ori + 2) / 2) % 2, ((ori + 1) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) (x + 0 - width), y + 1, (float) z).color(color).uv(((ori + 3) / 2) % 2, ((ori + 2) / 2) % 2).endVertex();
    }

    public static void renderXAlignedSquare(Matrix4f matrix, VertexConsumer buffer, int color, double x, int y, int z, int ori) {
        buffer.vertex(matrix, (float) x, y, z).color(color).uv(((ori) / 2) % 2, ((ori + 3) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) x, y, z + 1).color(color).uv(((ori + 1) / 2) % 2, ((ori) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) x, y + 1, z + 1).color(color).uv(((ori + 2) / 2) % 2, ((ori + 1) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) x, y + 1, z).color(color).uv(((ori + 3) / 2) % 2, ((ori + 2) / 2) % 2).endVertex();
    }

    public static void renderXAlignedRecangle(Matrix4f matrix, VertexConsumer buffer, int color, double x, int y, double z, int ori, double width) {
        buffer.vertex(matrix, (float) x, y, (float) (z + 0 - width)).color(color).uv(((ori) / 2) % 2, ((ori + 3) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) x, y, (float) (z + 1)).color(color).uv(((ori + 1) / 2) % 2, ((ori) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) x, y + 1, (float) (z + 1)).color(color).uv(((ori + 2) / 2) % 2, ((ori + 1) / 2) % 2).endVertex();
        buffer.vertex(matrix, (float) x, y + 1, (float) (z + 0 - width)).color(color).uv(((ori + 3) / 2) % 2, ((ori + 2) / 2) % 2).endVertex();
    }

    // Helper for rendering horizontal edges (along Z-axis)
    public static void renderZEdge(Level world, Matrix4f matrix, VertexConsumer buffer, int color, int x, int y, int z, double align, boolean air0, boolean air1, int dir) {
        if (!air0 && air1) RenderUtil.renderZAlignedSquare(matrix, buffer, color, x + 1, y, align, dir); // Entering air
        if (air0 && !air1) RenderUtil.renderZAlignedSquare(matrix, buffer, color, x, y, align, 2 + dir); // Exiting air
    }

    // X-aligned horizontal edge
    public static void renderXEdge(Level world, Matrix4f matrix, VertexConsumer buffer, int color, int x, int y, int z, double align, boolean air0, boolean air1, int dir) {
        if (!air0 && air1) RenderUtil.renderXAlignedSquare(matrix, buffer, color, align, y, z + 1, dir);
        if (air0 && !air1) RenderUtil.renderXAlignedSquare(matrix, buffer, color, align, y, z, 2 + dir);
    }

    // X-aligned vertical edge
    public static void renderXVerticalEdge(Level world, Matrix4f matrix, VertexConsumer buffer, int color, int x, int y, int z, double align, boolean air0, boolean air1, int dir) {
        if (!air0 && air1) RenderUtil.renderXAlignedSquare(matrix, buffer, color, align, y + 1, z, 3 + dir);
        if (air0 && !air1) RenderUtil.renderXAlignedSquare(matrix, buffer, color, align, y, z, 1 + dir);
    }

    // X-aligned vertical corner
    public static void renderXVerticalCorner(Level world, Matrix4f matrix, VertexConsumer buffer, int color, double x, int y, double z, boolean air0, boolean air1, int dir, double width) {
        if (!air0 && air1) RenderUtil.renderXAlignedRecangle(matrix, buffer, color, x, y + 1, z, 3 + dir, width);
        if (air0 && !air1) RenderUtil.renderXAlignedRecangle(matrix, buffer, color, x, y, z, 1 + dir, width);
    }

    // Z-aligned vertical corner
    public static void renderZVerticalCorner(Level world, Matrix4f matrix, VertexConsumer buffer, int color, double x, int y, double z, boolean air0, boolean air1, int dir, double width) {
        if (!air0 && air1) RenderUtil.renderZAlignedRecangle(matrix, buffer, color, x, y + 1, z, 3 + dir, width);
        if (air0 && !air1) RenderUtil.renderZAlignedRecangle(matrix, buffer, color, x, y, z, 1 + dir, width);
    }

    // Z-aligned vertical edge
    public static void renderZVerticalEdge(Level world, Matrix4f matrix, VertexConsumer buffer, int color, int x, int y, int z, double align, boolean air0, boolean air1, int dir) {
        if (!air0 && air1) RenderUtil.renderZAlignedSquare(matrix, buffer, color, x, y + 1, align, 3 + dir); // Entering air upward
        if (air0 && !air1) RenderUtil.renderZAlignedSquare(matrix, buffer, color, x, y, align, 1 + dir);     // Exiting air downward
    }
}
