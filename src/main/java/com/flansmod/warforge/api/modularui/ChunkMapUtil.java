package com.flansmod.warforge.api.modularui;

import com.flansmod.warforge.common.network.SiegeCampAttackInfo;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class ChunkMapUtil {
    public static long key(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFF_FFFFL);
    }

    public static void computeAdjacency(List<? extends SiegeCampAttackInfo> list, int radius, boolean[][] retArr) {
        int size = 2 * radius + 1;
        int total = size * size;

        IntStream.range(0, total).parallel().forEach(i -> {
            SiegeCampAttackInfo current = list.get(i);
            UUID currentFaction = current.mFactionUUID;

            int x = i % size;
            int z = i / size;

            if (z - 1 >= 0) {
                SiegeCampAttackInfo neighbor = list.get(i - size);
                retArr[i][0] = !currentFaction.equals(neighbor.mFactionUUID);
            }
            if (x + 1 < size) {
                SiegeCampAttackInfo neighbor = list.get(i + 1);
                retArr[i][1] = !currentFaction.equals(neighbor.mFactionUUID);
            }
            if (z + 1 < size) {
                SiegeCampAttackInfo neighbor = list.get(i + size);
                retArr[i][2] = !currentFaction.equals(neighbor.mFactionUUID);
            }
            if (x - 1 >= 0) {
                SiegeCampAttackInfo neighbor = list.get(i - 1);
                retArr[i][3] = !currentFaction.equals(neighbor.mFactionUUID);
            }
        });
    }
}
