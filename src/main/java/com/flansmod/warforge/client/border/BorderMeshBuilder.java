package com.flansmod.warforge.client.border;

import com.flansmod.warforge.client.render.BorderVertexSink;
import org.joml.Matrix4f;

/** One instance reuses scratch buffers: render thread only. */
public final class BorderMeshBuilder {
    public static final int NORTH = 1;
    public static final int EAST = 1 << 1;
    public static final int SOUTH = 1 << 2;
    public static final int WEST = 1 << 3;
    public static final int NORTH_WEST = 1 << 4;
    public static final int NORTH_EAST = 1 << 5;
    public static final int SOUTH_WEST = 1 << 6;
    public static final int SOUTH_EAST = 1 << 7;
    public static final int ALL_SIDES = 0xFF;

    private static final double ALIGNMENT = 0.25d;
    private static final double SMALLER_ALIGNMENT = ALIGNMENT - 0.125d;
    private static final double EDGE_LIFT = 0.001d;
    private static final float WALL_CAP_WIDTH = 2f;
    private static final float WALL_U_FAR = 64f;
    private static final float WALL_V_BOTTOM = 0.5f;
    private static final int Y_SCAN_MARGIN = 4;
    private static final int CHUNK = 16;

    private static final int EDGE_NORTH = 0;
    private static final int EDGE_SOUTH = 1;
    private static final int EDGE_WEST = 2;
    private static final int EDGE_EAST = 3;

    public interface Blocks {
        boolean isAir(int x, int y, int z);

        int surfaceHeight(int x, int z);
    }

    private boolean[] air = new boolean[0];
    private int sampledEdges;
    private int scanMin;
    private int scanMax;
    private int rows;

    private Blocks blocks;
    private int originX;
    private int originZ;
    private BorderVertexSink sink;
    private Matrix4f matrix;
    private int color;

    /** minY inclusive, maxY exclusive. */
    public void build(Blocks world, int minY, int maxY, int chunkMinX, int chunkMinZ, int argb, int sides,
                      BorderVertexSink out, Matrix4f outMatrix) {
        blocks = world;
        originX = chunkMinX;
        originZ = chunkMinZ;
        sink = out;
        matrix = outMatrix;
        color = argb;
        sampledEdges = 0;

        computeScanRange(minY, maxY);
        rows = scanMax - scanMin + 1;
        if (air.length < 4 * CHUNK * rows) {
            air = new boolean[4 * CHUNK * rows];
        }

        boolean north = (sides & NORTH) != 0;
        boolean east = (sides & EAST) != 0;
        boolean south = (sides & SOUTH) != 0;
        boolean west = (sides & WEST) != 0;

        if (north) {
            zWall((float) ALIGNMENT, west, east, minY, maxY);
        }
        if (south) {
            zWall((float) (CHUNK - ALIGNMENT), west, east, minY, maxY);
        }
        if (west) {
            xWall((float) ALIGNMENT, north, south, minY, maxY);
        }
        if (east) {
            xWall((float) (CHUNK - ALIGNMENT), north, south, minY, maxY);
        }

        if (north || south) {
            silhouetteAlongX(north, south, east, west);
        }
        if (east || west) {
            silhouetteAlongZ(north, south, east, west);
        }

        if ((sides & NORTH_EAST) != 0 && !north && !east) {
            corner(EDGE_NORTH, CHUNK - 1, 15, SMALLER_ALIGNMENT, CHUNK - SMALLER_ALIGNMENT, SMALLER_ALIGNMENT - 1);
        }
        if ((sides & NORTH_WEST) != 0 && !north && !west) {
            corner(EDGE_NORTH, 0, -1 + SMALLER_ALIGNMENT, SMALLER_ALIGNMENT, SMALLER_ALIGNMENT, SMALLER_ALIGNMENT - 1);
        }
        if ((sides & SOUTH_WEST) != 0 && !south && !west) {
            corner(EDGE_SOUTH, 0, -1 + SMALLER_ALIGNMENT, CHUNK - SMALLER_ALIGNMENT, SMALLER_ALIGNMENT, 15);
        }
        if ((sides & SOUTH_EAST) != 0 && !south && !east) {
            corner(EDGE_SOUTH, CHUNK - 1, 15, CHUNK - SMALLER_ALIGNMENT, CHUNK - SMALLER_ALIGNMENT, 15);
        }
    }

    /** Scans only the band around the perimeter surface; caves below it are invisible from outside. */
    private void computeScanRange(int minY, int maxY) {
        int surfMin = Integer.MAX_VALUE;
        int surfMax = Integer.MIN_VALUE;
        for (int i = 0; i < CHUNK; i++) {
            int hN = blocks.surfaceHeight(originX + i, originZ);
            int hS = blocks.surfaceHeight(originX + i, originZ + CHUNK - 1);
            int hW = blocks.surfaceHeight(originX, originZ + i);
            int hE = blocks.surfaceHeight(originX + CHUNK - 1, originZ + i);
            surfMin = Math.min(surfMin, Math.min(Math.min(hN, hS), Math.min(hW, hE)));
            surfMax = Math.max(surfMax, Math.max(Math.max(hN, hS), Math.max(hW, hE)));
        }
        scanMin = Math.max(minY, surfMin - Y_SCAN_MARGIN);
        scanMax = Math.min(maxY, surfMax + Y_SCAN_MARGIN);
    }

    private boolean air(int edge, int along, int y) {
        if ((sampledEdges & 1 << edge) == 0) {
            sampleEdge(edge);
        }
        return air[(edge * CHUNK + along) * rows + y - scanMin];
    }

    private void sampleEdge(int edge) {
        sampledEdges |= 1 << edge;
        for (int along = 0; along < CHUNK; along++) {
            int x = switch (edge) {
                case EDGE_WEST -> originX;
                case EDGE_EAST -> originX + CHUNK - 1;
                default -> originX + along;
            };
            int z = switch (edge) {
                case EDGE_NORTH -> originZ;
                case EDGE_SOUTH -> originZ + CHUNK - 1;
                default -> originZ + along;
            };
            int base = (edge * CHUNK + along) * rows;
            for (int y = scanMin; y <= scanMax; y++) {
                air[base + y - scanMin] = blocks.isAir(x, y, z);
            }
        }
    }

    private void silhouetteAlongX(boolean north, boolean south, boolean east, boolean west) {
        for (int x = 0; x < CHUNK; x++) {
            for (int y = scanMin; y < scanMax; y++) {
                if (x < CHUNK - 1) {
                    if (north) {
                        zEdge(x, y, SMALLER_ALIGNMENT + EDGE_LIFT, air(EDGE_NORTH, x, y), air(EDGE_NORTH, x + 1, y));
                    }
                    if (south) {
                        zEdge(x, y, CHUNK - SMALLER_ALIGNMENT + EDGE_LIFT, air(EDGE_SOUTH, x, y), air(EDGE_SOUTH, x + 1, y));
                    }
                }
                if (y < scanMax - 1) {
                    if (north) {
                        verticalAlongX(EDGE_NORTH, x, y, SMALLER_ALIGNMENT, east, west);
                    }
                    if (south) {
                        verticalAlongX(EDGE_SOUTH, x, y, CHUNK - SMALLER_ALIGNMENT, east, west);
                    }
                }
            }
        }
    }

    private void verticalAlongX(int edge, int x, int y, double z, boolean east, boolean west) {
        boolean air0 = air(edge, x, y);
        boolean air1 = air(edge, x, y + 1);
        if (x == CHUNK - 1 && east) {
            zVerticalCorner(x - SMALLER_ALIGNMENT, y, z, air0, air1, -SMALLER_ALIGNMENT);
        } else if (x == 0 && west) {
            zVerticalCorner(x, y, z, air0, air1, -SMALLER_ALIGNMENT);
        } else {
            zVerticalEdge(x, y, z, air0, air1);
        }
    }

    private void silhouetteAlongZ(boolean north, boolean south, boolean east, boolean west) {
        for (int z = 0; z < CHUNK; z++) {
            for (int y = scanMin; y < scanMax; y++) {
                if (z < CHUNK - 1) {
                    if (west) {
                        xEdge(y, z, SMALLER_ALIGNMENT + EDGE_LIFT, air(EDGE_WEST, z, y), air(EDGE_WEST, z + 1, y));
                    }
                    if (east) {
                        xEdge(y, z, CHUNK - SMALLER_ALIGNMENT + EDGE_LIFT, air(EDGE_EAST, z, y), air(EDGE_EAST, z + 1, y));
                    }
                }
                if (y < scanMax - 1) {
                    if (west) {
                        verticalAlongZ(EDGE_WEST, z, y, SMALLER_ALIGNMENT, north, south);
                    }
                    if (east) {
                        verticalAlongZ(EDGE_EAST, z, y, CHUNK - SMALLER_ALIGNMENT, north, south);
                    }
                }
            }
        }
    }

    private void verticalAlongZ(int edge, int z, int y, double x, boolean north, boolean south) {
        boolean air0 = air(edge, z, y);
        boolean air1 = air(edge, z, y + 1);
        if (z == CHUNK - 1 && south) {
            xVerticalCorner(x, y, z - SMALLER_ALIGNMENT, air0, air1, -SMALLER_ALIGNMENT);
        } else if (z == 0 && north) {
            xVerticalCorner(x, y, z, air0, air1, -SMALLER_ALIGNMENT);
        } else {
            xVerticalEdge(x, y, z, air0, air1);
        }
    }

    private void corner(int edge, int along, double zCornerX, double zCornerZ, double xCornerX, double xCornerZ) {
        for (int y = scanMin; y < scanMax; y++) {
            boolean air0 = air(edge, along, y);
            boolean air1 = air(edge, along, y + 1);
            zVerticalCorner(zCornerX, y, zCornerZ, air0, air1, SMALLER_ALIGNMENT - 1.0);
            xVerticalCorner(xCornerX, y, xCornerZ, air0, air1, SMALLER_ALIGNMENT - 1.0);
        }
    }

    private void zWall(float z, boolean west, boolean east, int minY, int maxY) {
        if (west) {
            wallQuadAlongX((float) ALIGNMENT, (float) ALIGNMENT + WALL_CAP_WIDTH, z, minY, maxY);
        }
        if (east) {
            wallQuadAlongX((float) (CHUNK - ALIGNMENT), (float) (CHUNK - ALIGNMENT) - WALL_CAP_WIDTH, z, minY, maxY);
        }
    }

    private void xWall(float x, boolean north, boolean south, int minY, int maxY) {
        if (north) {
            wallQuadAlongZ(x, (float) ALIGNMENT, (float) ALIGNMENT + WALL_CAP_WIDTH, minY, maxY);
        }
        if (south) {
            wallQuadAlongZ(x, (float) (CHUNK - ALIGNMENT), (float) (CHUNK - ALIGNMENT) - WALL_CAP_WIDTH, minY, maxY);
        }
    }

    private void wallQuadAlongX(float x0, float x1, float z, int minY, int maxY) {
        sink.vertex(matrix, x0, minY, z, color, WALL_U_FAR, WALL_V_BOTTOM);
        sink.vertex(matrix, x1, minY, z, color, WALL_U_FAR, 0f);
        sink.vertex(matrix, x1, maxY, z, color, 0f, 0f);
        sink.vertex(matrix, x0, maxY, z, color, 0f, WALL_V_BOTTOM);
    }

    private void wallQuadAlongZ(float x, float z0, float z1, int minY, int maxY) {
        sink.vertex(matrix, x, minY, z0, color, WALL_U_FAR, WALL_V_BOTTOM);
        sink.vertex(matrix, x, minY, z1, color, WALL_U_FAR, 0f);
        sink.vertex(matrix, x, maxY, z1, color, 0f, 0f);
        sink.vertex(matrix, x, maxY, z0, color, 0f, WALL_V_BOTTOM);
    }

    private void zEdge(int x, int y, double align, boolean air0, boolean air1) {
        if (!air0 && air1) {
            zSquare(x + 1, y, align, 0);
        }
        if (air0 && !air1) {
            zSquare(x, y, align, 2);
        }
    }

    private void xEdge(int y, int z, double align, boolean air0, boolean air1) {
        if (!air0 && air1) {
            xSquare(align, y, z + 1, 0);
        }
        if (air0 && !air1) {
            xSquare(align, y, z, 2);
        }
    }

    private void xVerticalEdge(double align, int y, int z, boolean air0, boolean air1) {
        if (!air0 && air1) {
            xSquare(align, y + 1, z, 3);
        }
        if (air0 && !air1) {
            xSquare(align, y, z, 1);
        }
    }

    private void zVerticalEdge(int x, int y, double align, boolean air0, boolean air1) {
        if (!air0 && air1) {
            zSquare(x, y + 1, align, 3);
        }
        if (air0 && !air1) {
            zSquare(x, y, align, 1);
        }
    }

    private void xVerticalCorner(double x, int y, double z, boolean air0, boolean air1, double width) {
        if (!air0 && air1) {
            xRectangle(x, y + 1, z, 3, width);
        }
        if (air0 && !air1) {
            xRectangle(x, y, z, 1, width);
        }
    }

    private void zVerticalCorner(double x, int y, double z, boolean air0, boolean air1, double width) {
        if (!air0 && air1) {
            zRectangle(x, y + 1, z, 3, width);
        }
        if (air0 && !air1) {
            zRectangle(x, y, z, 1, width);
        }
    }

    private void zSquare(int x, int y, double z, int ori) {
        sink.vertex(matrix, x, y, (float) z, color, uv(ori), uv(ori + 3));
        sink.vertex(matrix, x + 1, y, (float) z, color, uv(ori + 1), uv(ori));
        sink.vertex(matrix, x + 1, y + 1, (float) z, color, uv(ori + 2), uv(ori + 1));
        sink.vertex(matrix, x, y + 1, (float) z, color, uv(ori + 3), uv(ori + 2));
    }

    private void zRectangle(double x, int y, double z, int ori, double width) {
        sink.vertex(matrix, (float) (x - width), y, (float) z, color, uv(ori), uv(ori + 3));
        sink.vertex(matrix, (float) (x + 1), y, (float) z, color, uv(ori + 1), uv(ori));
        sink.vertex(matrix, (float) (x + 1), y + 1, (float) z, color, uv(ori + 2), uv(ori + 1));
        sink.vertex(matrix, (float) (x - width), y + 1, (float) z, color, uv(ori + 3), uv(ori + 2));
    }

    private void xSquare(double x, int y, int z, int ori) {
        sink.vertex(matrix, (float) x, y, z, color, uv(ori), uv(ori + 3));
        sink.vertex(matrix, (float) x, y, z + 1, color, uv(ori + 1), uv(ori));
        sink.vertex(matrix, (float) x, y + 1, z + 1, color, uv(ori + 2), uv(ori + 1));
        sink.vertex(matrix, (float) x, y + 1, z, color, uv(ori + 3), uv(ori + 2));
    }

    private void xRectangle(double x, int y, double z, int ori, double width) {
        sink.vertex(matrix, (float) x, y, (float) (z - width), color, uv(ori), uv(ori + 3));
        sink.vertex(matrix, (float) x, y, (float) (z + 1), color, uv(ori + 1), uv(ori));
        sink.vertex(matrix, (float) x, y + 1, (float) (z + 1), color, uv(ori + 2), uv(ori + 1));
        sink.vertex(matrix, (float) x, y + 1, (float) (z - width), color, uv(ori + 3), uv(ori + 2));
    }

    private static float uv(int ori) {
        return ori / 2 % 2;
    }
}
