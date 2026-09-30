package com.flansmod.warforge.client.render;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.compat.Ids;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * GPU side of the claim border renderer. The pipeline has the same state that 1.21.1 draws the borders with: the
 * position_tex_color shader, translucent blending, no face culling, LEQUAL depth test with depth write, and a
 * nearest-filter REPEAT sampler (the wall texture coordinates go past 1). Meshes are cached in static vertex
 * buffers and drawn into the translucent terrain target.
 */
public final class BorderGpu {
    static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Ids.of(Tags.MODID, "pipeline/claim_border"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build();

    /** The camera view matrix of the current frame, set by ClientRenderEvents (used by drawImmediate). */
    private static final Matrix4f FRAME_VIEW = new Matrix4f();

    private BorderGpu() {}

    /** A cached border mesh on the GPU. */
    public static final class Mesh {
        private @Nullable GpuBuffer buffer;
        private int indexCount;
    }

    static void setFrameView(Matrix4fc view) {
        FRAME_VIEW.set(view);
    }

    /** Sets the render state for the border and overlay draws. On 26.x the pipeline holds all of it. */
    public static void beginFrame() {
    }

    /** Restores the render state. On 26.x there is nothing to restore. */
    public static void endFrame() {
    }

    /**
     * Builds a mesh from the geometry and uploads it into reuse (or a new mesh). Returns null, and leaves reuse
     * unchanged, if the geometry has no vertices.
     */
    public static @Nullable Mesh build(@Nullable Mesh reuse, Consumer<BorderVertexSink> geometry) {
        try (MeshData data = buildMesh(geometry)) {
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

    /** Draws a cached mesh with the texture. modelView places the mesh relative to the camera. */
    public static void draw(Mesh mesh, Identifier texture, Matrix4f modelView) {
        if (mesh.buffer != null) {
            drawBuffer(mesh.buffer, mesh.indexCount, texture, modelView);
        }
    }

    /** Builds and draws geometry at once (not cached). Vertices must already be camera-relative. */
    public static void drawImmediate(Identifier texture, Consumer<BorderVertexSink> geometry) {
        try (MeshData data = buildMesh(geometry)) {
            if (data == null) {
                return;
            }
            try (GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "WarForge placement overlay",
                    GpuBuffer.USAGE_VERTEX, data.vertexBuffer())) {
                drawBuffer(buffer, data.drawState().indexCount(), texture, FRAME_VIEW);
            }
        }
    }

    public static void close(Mesh mesh) {
        if (mesh.buffer != null) {
            mesh.buffer.close();
            mesh.buffer = null;
        }
    }

    private static @Nullable MeshData buildMesh(Consumer<BorderVertexSink> geometry) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        geometry.accept((matrix, x, y, z, argb, u, v) -> builder.addVertex(matrix, x, y, z).setColor(argb).setUv(u, v));
        return builder.build();
    }

    private static void drawBuffer(GpuBuffer vertices, int indexCount, Identifier texture, Matrix4fc modelView) {
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        RenderTarget target = ChunkSectionLayerGroup.TRANSLUCENT.outputTarget();
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(indexCount);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(modelView, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "WarForge claim border",
                target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("Sampler0", tex.getTextureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.drawIndexed(0, 0, indexCount, 1);
        }
    }
}
