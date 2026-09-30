package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.function.Consumer;

/**
 * GPU side of the claim border renderer: the position_tex_color shader, default translucent blending, no face
 * culling, depth test and depth write as the stage leaves them. Meshes are cached in static vertex buffers.
 */
public final class BorderGpu {
    private BorderGpu() {}

    /** A cached border mesh on the GPU. */
    public static final class Mesh {
        private final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    }

    /** Sets the render state for the border and overlay draws. Call once per frame before drawing. */
    public static void beginFrame() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Restores the render state. */
    public static void endFrame() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Builds a mesh from the geometry and uploads it into reuse (or a new mesh). Returns null, and leaves reuse
     * unchanged, if the geometry has no vertices.
     */
    public static @Nullable Mesh build(@Nullable Mesh reuse, Consumer<BorderVertexSink> geometry) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        geometry.accept((matrix, x, y, z, argb, u, v) -> builder.addVertex(matrix, x, y, z).setColor(argb).setUv(u, v));
        MeshData rendered = builder.build();
        if (rendered == null) {
            return null;
        }
        Mesh mesh = reuse != null ? reuse : new Mesh();
        mesh.buffer.bind();
        mesh.buffer.upload(rendered);
        VertexBuffer.unbind();
        return mesh;
    }

    /** Draws a cached mesh with the texture. modelView places the mesh relative to the camera. */
    public static void draw(Mesh mesh, ResourceLocation texture, Matrix4f modelView) {
        RenderSystem.setShaderTexture(0, texture);
        mesh.buffer.bind();
        mesh.buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), GameRenderer.getPositionTexColorShader());
        VertexBuffer.unbind();
    }

    /** Builds and draws geometry at once (not cached). Vertices must already be camera-relative. */
    public static void drawImmediate(ResourceLocation texture, Consumer<BorderVertexSink> geometry) {
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        geometry.accept((matrix, x, y, z, argb, u, v) -> builder.addVertex(matrix, x, y, z).setColor(argb).setUv(u, v));
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    public static void close(Mesh mesh) {
        mesh.buffer.close();
    }
}
