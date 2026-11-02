package com.flansmod.warforge.client.render;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.compat.Ids;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/** 26.2 reversed depth buffer: default GREATER_THAN_OR_EQUAL+write == old LEQUAL. Sampler nearest REPEAT (uv > 1). */
final class BorderBackend {
    static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withLocation(Ids.of(Tags.MODID, "pipeline/claim_border"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build();

    private static final ByteBufferBuilder BYTES = new ByteBufferBuilder(DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 1024);

    private BorderBackend() {}

    static @Nullable MeshData buildMesh(Consumer<BorderVertexSink> geometry) {
        BufferBuilder builder = new BufferBuilder(BYTES, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        geometry.accept((matrix, x, y, z, argb, u, v) -> builder.addVertex(matrix, x, y, z).setColor(argb).setUv(u, v));
        return builder.build();
    }

    static RenderPass openPass(RenderTarget target) {
        return RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "WarForge claim border",
                target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty());
    }

    static void drawQuads(RenderPass pass, GpuBuffer vertices, int indexCount) {
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        pass.setVertexBuffer(0, vertices.slice());
        pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
        pass.drawIndexed(indexCount, 1, 0, 0, 0);
    }
}
