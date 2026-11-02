package com.flansmod.warforge.client.render;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.compat.Ids;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

/** Pipeline: position_tex_color, translucent, no cull, LEQUAL+depth write, nearest REPEAT sampler (uv > 1). */
final class BorderBackend {
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

    private BorderBackend() {}

    static @Nullable MeshData buildMesh(Consumer<BorderVertexSink> geometry) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        geometry.accept((matrix, x, y, z, argb, u, v) -> builder.addVertex(matrix, x, y, z).setColor(argb).setUv(u, v));
        return builder.build();
    }

    static RenderPass openPass(RenderTarget target) {
        return RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "WarForge claim border",
                target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty());
    }

    static void drawQuads(RenderPass pass, GpuBuffer vertices, int indexCount) {
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        pass.setVertexBuffer(0, vertices);
        pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
        pass.drawIndexed(0, 0, indexCount, 1);
    }
}
