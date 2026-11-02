package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.function.Consumer;

public final class BorderGpu {
    private BorderGpu() {}

    public static final class Mesh {
        private final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    }

    public static void beginFrame() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void endFrame() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static BorderVertexSink sink(BufferBuilder builder) {
        return (matrix, x, y, z, argb, u, v) -> builder.vertex(matrix, x, y, z).uv(u, v).color(argb).endVertex();
    }

    public static @Nullable Mesh build(@Nullable Mesh reuse, Consumer<BorderVertexSink> geometry) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        geometry.accept(sink(builder));
        BufferBuilder.RenderedBuffer rendered = builder.end();
        if (rendered.isEmpty()) {
            rendered.release();
            return null;
        }
        Mesh mesh = reuse != null ? reuse : new Mesh();
        mesh.buffer.bind();
        mesh.buffer.upload(rendered);
        VertexBuffer.unbind();
        return mesh;
    }

    public static void draw(Mesh mesh, ResourceLocation texture, Matrix4f modelView) {
        RenderSystem.setShaderTexture(0, texture);
        mesh.buffer.bind();
        mesh.buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), GameRenderer.getPositionTexColorShader());
        VertexBuffer.unbind();
    }

    public static void drawImmediate(ResourceLocation texture, Consumer<BorderVertexSink> geometry) {
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        geometry.accept(sink(builder));
        BufferBuilder.RenderedBuffer rendered = builder.end();
        if (rendered.isEmpty()) {
            rendered.release();
            return;
        }
        BufferUploader.drawWithShader(rendered);
    }

    public static void close(Mesh mesh) {
        mesh.buffer.close();
    }
}
