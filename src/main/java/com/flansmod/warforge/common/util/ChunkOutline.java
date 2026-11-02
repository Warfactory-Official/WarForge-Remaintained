package com.flansmod.warforge.common.util;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Hole-free boundary polygons of a chunk set. Pure geometry, no Minecraft types.
 * <ul>
 *   <li>Polygon = flat {@code x0,z0,x1,z1,...} in chunk-corner units, collinear vertices dropped.
 *   <li>Component = 4-connected. Diagonal pinch: exterior side wins, loop self-touches at the vertex.
 *   <li>Enclosed hole => component cut at the hole's min x into x &lt; cut / x &gt;= cut, recursed. Seam between pieces.
 * </ul>
 */
public final class ChunkOutline {
    private static final int E = 1, S = 2, W = 4, N = 8;

    private ChunkOutline() {
    }

    public static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int keyX(long key) {
        return (int) (key >> 32);
    }

    public static int keyZ(long key) {
        return (int) key;
    }

    public static List<int[]> outlines(LongSet chunks) {
        ArrayList<int[]> out = new ArrayList<>();
        for (LongOpenHashSet component : components(chunks)) {
            solve(component, out);
        }
        return out;
    }

    private static void solve(LongOpenHashSet component, List<int[]> out) {
        ArrayList<long[]> loops = trace(component);
        int cut = Integer.MIN_VALUE;
        boolean hasHole = false;
        ArrayList<long[]> outer = new ArrayList<>(1);
        for (long[] loop : loops) {
            if (area2(loop) < 0) {
                int minX = Integer.MAX_VALUE;
                for (long v : loop) {
                    minX = Math.min(minX, keyX(v));
                }
                cut = hasHole ? Math.min(cut, minX) : minX;
                hasHole = true;
            } else {
                outer.add(loop);
            }
        }
        if (!hasHole) {
            for (long[] loop : outer) {
                out.add(flatten(loop));
            }
            return;
        }
        LongOpenHashSet left = new LongOpenHashSet();
        LongOpenHashSet right = new LongOpenHashSet();
        for (LongIterator it = component.iterator(); it.hasNext(); ) {
            long c = it.nextLong();
            (keyX(c) < cut ? left : right).add(c);
        }
        for (LongOpenHashSet part : components(left)) {
            solve(part, out);
        }
        for (LongOpenHashSet part : components(right)) {
            solve(part, out);
        }
    }

    private static List<LongOpenHashSet> components(LongSet chunks) {
        ArrayList<LongOpenHashSet> result = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet(chunks.size());
        LongArrayList stack = new LongArrayList();
        for (LongIterator it = chunks.iterator(); it.hasNext(); ) {
            long start = it.nextLong();
            if (!seen.add(start)) {
                continue;
            }
            LongOpenHashSet component = new LongOpenHashSet();
            stack.add(start);
            while (!stack.isEmpty()) {
                long c = stack.removeLong(stack.size() - 1);
                component.add(c);
                int x = keyX(c);
                int z = keyZ(c);
                push(chunks, seen, stack, key(x + 1, z));
                push(chunks, seen, stack, key(x - 1, z));
                push(chunks, seen, stack, key(x, z + 1));
                push(chunks, seen, stack, key(x, z - 1));
            }
            result.add(component);
        }
        return result;
    }

    private static void push(LongSet chunks, LongOpenHashSet seen, LongArrayList stack, long neighbour) {
        if (chunks.contains(neighbour) && seen.add(neighbour)) {
            stack.add(neighbour);
        }
    }

    /** Boundary edges keep the filled cell on the right; at a vertex with two exits the right turn wins. */
    private static ArrayList<long[]> trace(LongOpenHashSet cells) {
        Long2IntOpenHashMap exits = new Long2IntOpenHashMap(cells.size() * 2);
        for (LongIterator it = cells.iterator(); it.hasNext(); ) {
            long c = it.nextLong();
            int x = keyX(c);
            int z = keyZ(c);
            if (!cells.contains(key(x, z - 1))) exits.merge(key(x, z), E, (a, b) -> a | b);
            if (!cells.contains(key(x + 1, z))) exits.merge(key(x + 1, z), S, (a, b) -> a | b);
            if (!cells.contains(key(x, z + 1))) exits.merge(key(x + 1, z + 1), W, (a, b) -> a | b);
            if (!cells.contains(key(x - 1, z))) exits.merge(key(x, z + 1), N, (a, b) -> a | b);
        }
        ArrayList<long[]> loops = new ArrayList<>();
        Long2IntOpenHashMap used = new Long2IntOpenHashMap(exits.size());
        LongArrayList vertices = new LongArrayList();
        for (Long2IntMap.Entry entry : exits.long2IntEntrySet()) {
            long start = entry.getLongKey();
            for (int startHeading = 1; startHeading <= N; startHeading <<= 1) {
                if ((entry.getIntValue() & startHeading) == 0 || (used.get(start) & startHeading) != 0) {
                    continue;
                }
                vertices.clear();
                long at = start;
                int heading = startHeading;
                do {
                    used.merge(at, heading, (a, b) -> a | b);
                    long next = step(at, heading);
                    int nextHeading = pick(exits.get(next), heading);
                    if (nextHeading != heading) {
                        vertices.add(next);
                    }
                    at = next;
                    heading = nextHeading;
                } while (at != start || heading != startHeading);
                loops.add(vertices.toLongArray());
            }
        }
        return loops;
    }

    private static long step(long v, int heading) {
        int x = keyX(v);
        int z = keyZ(v);
        return switch (heading) {
            case E -> key(x + 1, z);
            case S -> key(x, z + 1);
            case W -> key(x - 1, z);
            default -> key(x, z - 1);
        };
    }

    private static int pick(int available, int heading) {
        int right = rotate(heading, 1);
        int left = rotate(heading, 3);
        if ((available & right) != 0) return right;
        if ((available & heading) != 0) return heading;
        if ((available & left) != 0) return left;
        return 0;
    }

    private static int rotate(int heading, int quarterTurnsClockwise) {
        int index = Integer.numberOfTrailingZeros(heading);
        return 1 << ((index + quarterTurnsClockwise) & 3);
    }

    private static long area2(long[] loop) {
        long sum = 0;
        for (int i = 0; i < loop.length; i++) {
            long a = loop[i];
            long b = loop[(i + 1) % loop.length];
            sum += (long) keyX(a) * keyZ(b) - (long) keyX(b) * keyZ(a);
        }
        return sum;
    }

    private static int[] flatten(long[] loop) {
        int[] flat = new int[loop.length * 2];
        for (int i = 0; i < loop.length; i++) {
            flat[2 * i] = keyX(loop[i]);
            flat[2 * i + 1] = keyZ(loop[i]);
        }
        return flat;
    }
}
