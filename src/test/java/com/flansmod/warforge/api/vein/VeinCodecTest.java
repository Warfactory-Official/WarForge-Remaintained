package com.flansmod.warforge.api.vein;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VeinCodecTest {
    private static final int LENGTH = 10;

    @Test
    void chunksOfOneMegachunkShareAKeyAndOffsetsAreUnique() {
        for (int mx = -3; mx <= 3; mx++) {
            for (int mz = -3; mz <= 3; mz++) {
                long key = VeinCodec.megachunkKey(mx * LENGTH, mz * LENGTH, LENGTH);
                Set<Short> offsets = new HashSet<>();
                for (int dx = 0; dx < LENGTH; dx++) {
                    for (int dz = 0; dz < LENGTH; dz++) {
                        int x = mx * LENGTH + dx;
                        int z = mz * LENGTH + dz;
                        assertEquals(key, VeinCodec.megachunkKey(x, z, LENGTH), x + "," + z);
                        assertTrue(offsets.add(VeinCodec.chunkOffset(x, z, LENGTH)), x + "," + z);
                        assertArrayEquals(new int[]{x, z},
                                VeinCodec.chunkCoords(VeinCodec.chunkOffset(x, z, LENGTH), key, LENGTH));
                    }
                }
            }
        }
    }

    @Test
    void boundariesStartNewMegachunks() {
        assertEquals(VeinCodec.megachunkKey(0, 0, LENGTH), VeinCodec.megachunkKey(LENGTH - 1, LENGTH - 1, LENGTH));
        assertTrue(VeinCodec.megachunkKey(0, 0, LENGTH) != VeinCodec.megachunkKey(LENGTH, 0, LENGTH));
        assertEquals(VeinCodec.megachunkKey(-1, -1, LENGTH), VeinCodec.megachunkKey(-LENGTH, -LENGTH, LENGTH));
        assertTrue(VeinCodec.megachunkKey(-1, 0, LENGTH) != VeinCodec.megachunkKey(0, 0, LENGTH));
        assertTrue(VeinCodec.megachunkKey(5, -1, LENGTH) != VeinCodec.megachunkKey(5, 0, LENGTH));
    }

    @Test
    void largeOffsetsSurviveSignExtension() {
        int length = 180;
        int x = length - 1;
        int z = length - 2;
        long key = VeinCodec.megachunkKey(x, z, length);
        assertArrayEquals(new int[]{x, z}, VeinCodec.chunkCoords(VeinCodec.chunkOffset(x, z, length), key, length));
    }
}
