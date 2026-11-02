package com.flansmod.warforge.client.border;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderMeshBuilderTest {
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 64;

    private static int vertices(BorderMeshBuilder.Blocks blocks, int sides) {
        int[] count = new int[1];
        new BorderMeshBuilder().build(blocks, MIN_Y, MAX_Y, 0, 0, 0xFFFFFFFF, sides,
                (matrix, x, y, z, argb, u, v) -> count[0]++, new Matrix4f());
        return count[0];
    }

    private static BorderMeshBuilder.Blocks flatGround(int surface) {
        return new BorderMeshBuilder.Blocks() {
            @Override
            public boolean isAir(int x, int y, int z) {
                return y >= surface;
            }

            @Override
            public int surfaceHeight(int x, int z) {
                return surface;
            }
        };
    }

    @Test
    void noSidesNoGeometry() {
        assertEquals(0, vertices(flatGround(10), 0));
    }

    @Test
    void northEdgeEmitsWallsAndSilhouette() {
        int count = vertices(flatGround(10), BorderMeshBuilder.NORTH);
        assertTrue(count > 0);
        assertEquals(0, count % 4);
    }

    @Test
    void flatGroundSilhouetteIsOneRowOfQuadsPerEdgeColumn() {
        int walls = vertices(flatGround(10), BorderMeshBuilder.NORTH | BorderMeshBuilder.WEST | BorderMeshBuilder.SOUTH | BorderMeshBuilder.EAST);
        int northOnly = vertices(flatGround(10), BorderMeshBuilder.NORTH);
        assertTrue(walls > northOnly);
    }

    @Test
    void reusableAcrossBuilds() {
        BorderMeshBuilder builder = new BorderMeshBuilder();
        int[] count = new int[1];
        for (int i = 0; i < 2; i++) {
            count[0] = 0;
            builder.build(flatGround(10), MIN_Y, MAX_Y, 0, 0, 0xFFFFFFFF, BorderMeshBuilder.ALL_SIDES,
                    (matrix, x, y, z, argb, u, v) -> count[0]++, new Matrix4f());
        }
        assertEquals(vertices(flatGround(10), BorderMeshBuilder.ALL_SIDES), count[0]);
    }
}
