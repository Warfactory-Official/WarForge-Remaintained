package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** One instance reused every frame: valid during the callback only. */
public final class WorldRenderContext {
    private float partialTick;
    private PoseStack poseStack;
    private final Matrix4f viewMatrix = new Matrix4f();

    public float partialTick() {
        return partialTick;
    }

    public PoseStack poseStack() {
        return poseStack;
    }

    public Matrix4f viewMatrix() {
        return viewMatrix;
    }

    WorldRenderContext update(float partialTick, PoseStack poseStack, Matrix4fc view) {
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.viewMatrix.set(view);
        return this;
    }
}
