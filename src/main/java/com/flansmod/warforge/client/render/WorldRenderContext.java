package com.flansmod.warforge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

/**
 * The world render stage after translucent blocks (see ClientRenderEvents).
 *
 * @param partialTick fraction of the current tick
 * @param poseStack   the stage's pose stack (vertex transform relative to the camera position)
 * @param viewMatrix  the camera's view (rotation) matrix, the model-view matrix for camera-relative geometry
 */
public record WorldRenderContext(float partialTick, PoseStack poseStack, Matrix4f viewMatrix) {
}
