package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/** One render pass per draw: uniform and buffer writes must not happen inside an open pass. */
public final class BorderGpu {
    private static final Matrix4f FRAME_VIEW = new Matrix4f();
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f NO_TEXTURE_MATRIX = new Matrix4f();
    private static final Vector4f WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f NO_OFFSET = new Vector3f();

    private static @Nullable GpuBuffer immediateBuffer;

    private BorderGpu() {}

    public static final class Mesh {
        private @Nullable GpuBuffer buffer;
        private int indexCount;
    }

    static void setFrameView(Matrix4fc view) {
        FRAME_VIEW.set(view);
    }

    public static void beginFrame() {
    }

    public static void endFrame() {
    }

    /** No vertices => null, reuse unchanged. */
    public static @Nullable Mesh build(@Nullable Mesh reuse, Consumer<BorderVertexSink> geometry) {
        try (MeshData data = BorderBackend.buildMesh(geometry)) {
            if (data == null) {
                return null;
            }
            Mesh mesh = reuse != null ? reuse : new Mesh();
            if (mesh.buffer != null) {
                mesh.buffer.close();
            }
            mesh.buffer = RenderSystem.getDevice().createBuffer(() -> "WarForge claim border", GpuBuffer.USAGE_VERTEX,
                    data.vertexBuffer());
            mesh.indexCount = data.drawState().indexCount();
            return mesh;
        }
    }

    public static void draw(Mesh mesh, Identifier texture, Matrix4f modelView) {
        if (mesh.buffer != null) {
            drawBuffer(mesh.buffer, mesh.indexCount, texture, modelView);
        }
    }

    public static void drawImmediate(Identifier texture, Consumer<BorderVertexSink> geometry) {
        try (MeshData data = BorderBackend.buildMesh(geometry)) {
            if (data == null) {
                return;
            }
            ByteBuffer vertices = data.vertexBuffer();
            int size = vertices.remaining();
            GpuBuffer buffer = immediateBuffer;
            if (buffer == null || buffer.size() < size) {
                if (buffer != null) {
                    buffer.close();
                }
                buffer = RenderSystem.getDevice().createBuffer(() -> "WarForge placement overlay",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, Math.max(size, 4096));
                immediateBuffer = buffer;
            }
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0, size), vertices);
            drawBuffer(buffer, data.drawState().indexCount(), texture, FRAME_VIEW);
        }
    }

    public static void close(Mesh mesh) {
        if (mesh.buffer != null) {
            mesh.buffer.close();
            mesh.buffer = null;
        }
    }

    private static void drawBuffer(GpuBuffer vertices, int indexCount, Identifier texture, Matrix4fc modelView) {
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        MODEL_VIEW.set(modelView);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(MODEL_VIEW, WHITE, NO_OFFSET, NO_TEXTURE_MATRIX);
        try (RenderPass pass = BorderBackend.openPass(ChunkSectionLayerGroup.TRANSLUCENT.outputTarget())) {
            pass.setPipeline(BorderBackend.PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("Sampler0", tex.getTextureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
            BorderBackend.drawQuads(pass, vertices, indexCount);
        }
    }
}
