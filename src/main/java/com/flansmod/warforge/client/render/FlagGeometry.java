package com.flansmod.warforge.client.render;

import com.flansmod.warforge.common.blocks.PoleGeometry;
import net.minecraft.util.Mth;

/**
 * The claim pole and its waving flag, independent of the render API. The block entity renderer of each version
 * turns these vertices into its own draw calls.
 */
public final class FlagGeometry {
    private static final int WAVE_SEGMENTS = 8;
    private static final float WAVE_AMPLITUDE = 0.12F;
    private static final float WAVE_SPEED = 0.12F;
    private static final float WAVE_FREQUENCY = 0.45F;

    private static final float ANCHOR_X = 0.0625F;
    private static final float MAX_BANNER_WIDTH = 1.4F;
    private static final float MAX_BANNER_HEIGHT = 0.95F;

    public static final float MODEL_SCALE = PoleGeometry.MODEL_SCALE;
    public static final float SHAFT_BASE_Y = PoleGeometry.SHAFT_BASE_Y;
    public static final float NATURAL_SHAFT_LENGTH = 4.375F;
    private static final float BANNER_BELOW_TOP = 0.1F;

    private FlagGeometry() {
    }

    /** One flag vertex in pole-model space: position, texture coordinates and the z of the face normal. */
    @FunctionalInterface
    public interface FlagVertex {
        void vertex(float x, float y, float z, float u, float v, float normalZ);
    }

    /** Vertical stretch of the pole shaft for a pole length. */
    public static float shaftStretch(float poleLength) {
        return poleLength / NATURAL_SHAFT_LENGTH;
    }

    /** Y of the top edge of the flag for a pole length. */
    public static float bannerTop(float poleLength) {
        return SHAFT_BASE_Y + poleLength - BANNER_BELOW_TOP;
    }

    /** True if a pole model vertex at this Y belongs to the shaft (stretched), not the base. */
    public static boolean isShaftY(float y) {
        return y > SHAFT_BASE_Y + 1.0E-4F;
    }

    /** Animation time of the wave. */
    public static float waveTime(long gameTime, float partialTicks) {
        return WAVE_SPEED * ((float) (gameTime % 100000L) + partialTicks);
    }

    /**
     * Emits the flag as quads (4 vertices each; a front and a back quad per wave segment).
     *
     * @param dims flag texture size {width, height}, or null (square)
     */
    public static void emitFlag(int[] dims, float bannerTop, float time, FlagVertex out) {
        float aspect = (dims != null && dims.length == 2 && dims[1] > 0) ? (float) dims[0] / dims[1] : 1.0F;
        float boxAspect = MAX_BANNER_WIDTH / MAX_BANNER_HEIGHT;
        float bannerWidth;
        float bannerHeight;
        if (aspect >= boxAspect) {
            bannerWidth = MAX_BANNER_WIDTH;
            bannerHeight = bannerWidth / aspect;
        } else {
            bannerHeight = MAX_BANNER_HEIGHT;
            bannerWidth = bannerHeight * aspect;
        }

        float bannerBottom = bannerTop - bannerHeight;
        float x0 = ANCHOR_X;
        float dx = bannerWidth / WAVE_SEGMENTS;

        for (int seg = 0; seg < WAVE_SEGMENTS; seg++) {
            float xa = x0 + dx * seg;
            float xb = x0 + dx * (seg + 1);
            float ua = (float) seg / WAVE_SEGMENTS;
            float ub = (float) (seg + 1) / WAVE_SEGMENTS;

            float za = wave(time, seg);
            float zb = wave(time, seg + 1);

            quad(out, xa, xb, za, zb, ua, ub, bannerTop, bannerBottom, true);
            quad(out, xa, xb, za, zb, ua, ub, bannerTop, bannerBottom, false);
        }
    }

    private static float wave(float time, int seg) {
        return WAVE_AMPLITUDE * Mth.sin(time + seg * WAVE_FREQUENCY) * seg / WAVE_SEGMENTS;
    }

    private static void quad(FlagVertex out, float xa, float xb, float za, float zb, float ua, float ub,
                             float bannerTop, float bannerBottom, boolean front) {
        float nz = front ? 1.0F : -1.0F;
        float z = front ? 0.005F : -0.005F;
        if (front) {
            out.vertex(xa, bannerTop, za + z, ua, 0.0F, nz);
            out.vertex(xa, bannerBottom, za + z, ua, 1.0F, nz);
            out.vertex(xb, bannerBottom, zb + z, ub, 1.0F, nz);
            out.vertex(xb, bannerTop, zb + z, ub, 0.0F, nz);
        } else {
            out.vertex(xb, bannerTop, zb + z, ub, 0.0F, nz);
            out.vertex(xb, bannerBottom, zb + z, ub, 1.0F, nz);
            out.vertex(xa, bannerBottom, za + z, ua, 1.0F, nz);
            out.vertex(xa, bannerTop, za + z, ua, 0.0F, nz);
        }
    }
}
