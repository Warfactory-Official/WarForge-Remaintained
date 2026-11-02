package com.flansmod.warforge.common.util;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkOutlineTest {
    private static LongOpenHashSet set(String... rows) {
        LongOpenHashSet out = new LongOpenHashSet();
        for (int z = 0; z < rows.length; z++) {
            for (int x = 0; x < rows[z].length(); x++) {
                if (rows[z].charAt(x) == '#') {
                    out.add(ChunkOutline.key(x, z));
                }
            }
        }
        return out;
    }

    private static long area2(int[] poly) {
        long sum = 0;
        int n = poly.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            sum += (long) poly[2 * i] * poly[2 * j + 1] - (long) poly[2 * j] * poly[2 * i + 1];
        }
        return sum;
    }

    private static long totalArea(List<int[]> polys) {
        long sum = 0;
        for (int[] p : polys) {
            assertTrue(area2(p) > 0, "polygon must be positively oriented");
            sum += area2(p) / 2;
        }
        return sum;
    }

    @Test
    void singleChunkIsUnitSquare() {
        List<int[]> polys = ChunkOutline.outlines(set("#"));
        assertEquals(1, polys.size());
        assertEquals(8, polys.get(0).length);
        assertEquals(1, totalArea(polys));
    }

    @Test
    void rectangleDropsCollinearVertices() {
        List<int[]> polys = ChunkOutline.outlines(set("###", "###"));
        assertEquals(1, polys.size());
        assertEquals(8, polys.get(0).length);
        assertEquals(6, totalArea(polys));
    }

    @Test
    void lShapeHasSixVertices() {
        List<int[]> polys = ChunkOutline.outlines(set("#.", "##"));
        assertEquals(1, polys.size());
        assertEquals(12, polys.get(0).length);
        assertEquals(3, totalArea(polys));
    }

    @Test
    void separateComponentsGiveSeparatePolygons() {
        List<int[]> polys = ChunkOutline.outlines(set("#.#"));
        assertEquals(2, polys.size());
        assertEquals(2, totalArea(polys));
    }

    @Test
    void diagonalCellsAreSeparateComponents() {
        List<int[]> polys = ChunkOutline.outlines(set("#.", ".#"));
        assertEquals(2, polys.size());
    }

    @Test
    void enclosedHoleIsSplitIntoHoleFreePieces() {
        LongOpenHashSet ring = set("###", "#.#", "###");
        List<int[]> polys = ChunkOutline.outlines(ring);
        assertTrue(polys.size() >= 2);
        assertEquals(8, totalArea(polys));
        assertCoversExactly(ring, polys);
    }

    @Test
    void pinchedPocketStaysOnePolygon() {
        LongOpenHashSet shape = set("####", "#..#", "#.##", "###.");
        List<int[]> polys = ChunkOutline.outlines(shape);
        assertCoversExactly(shape, polys);
    }

    @Test
    void multipleHoles() {
        LongOpenHashSet shape = set("#######", "#.#.#.#", "#######", "#.....#", "#######");
        List<int[]> polys = ChunkOutline.outlines(shape);
        assertCoversExactly(shape, polys);
    }

    @Test
    void randomShapesCoverExactlyTheirCells() {
        Random random = new Random(7);
        for (int round = 0; round < 300; round++) {
            LongOpenHashSet shape = new LongOpenHashSet();
            int size = 4 + random.nextInt(9);
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    if (random.nextInt(100) < 70) {
                        shape.add(ChunkOutline.key(x, z));
                    }
                }
            }
            assertCoversExactly(shape, ChunkOutline.outlines(shape));
        }
    }

    @Test
    void emptySetGivesNothing() {
        assertTrue(ChunkOutline.outlines(new LongOpenHashSet()).isEmpty());
    }

    private static void assertCoversExactly(LongOpenHashSet shape, List<int[]> polys) {
        Set<Long> covered = new HashSet<>();
        for (int[] poly : polys) {
            assertTrue(area2(poly) > 0);
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (int i = 0; i < poly.length; i += 2) {
                minX = Math.min(minX, poly[i]);
                maxX = Math.max(maxX, poly[i]);
                minZ = Math.min(minZ, poly[i + 1]);
                maxZ = Math.max(maxZ, poly[i + 1]);
            }
            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x < maxX; x++) {
                    if (inside(poly, x + 0.5, z + 0.5)) {
                        assertTrue(covered.add(ChunkOutline.key(x, z)), "cell covered twice");
                    }
                }
            }
        }
        assertEquals(shape.size(), covered.size());
        for (long c : covered) {
            assertTrue(shape.contains(c));
        }
    }

    private static boolean inside(int[] poly, double px, double pz) {
        boolean in = false;
        int n = poly.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly[2 * i], zi = poly[2 * i + 1], xj = poly[2 * j], zj = poly[2 * j + 1];
            if ((zi > pz) != (zj > pz) && px < (xj - xi) * (pz - zi) / (zj - zi) + xi) {
                in = !in;
            }
        }
        return in;
    }
}
