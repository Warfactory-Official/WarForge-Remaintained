package com.flansmod.warforge.client;

import com.flansmod.warforge.common.item.ItemMatcher;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Map;

/** Citadel level data as last synced from the server; never the server's own table (integrated server shares statics). */
public final class ClientUpgradeTable {
    private record Level(Map<ItemMatcher, Integer> requirements, int claimLimit, int insuranceSlots, int loadedChunks) {
    }

    private static final Int2ObjectOpenHashMap<Level> LEVELS = new Int2ObjectOpenHashMap<>();

    private ClientUpgradeTable() {
    }

    public static void set(int level, Map<ItemMatcher, Integer> requirements, int claimLimit, int insuranceSlots, int loadedChunks) {
        LEVELS.put(level, new Level(Map.copyOf(requirements), claimLimit, insuranceSlots, loadedChunks));
    }

    public static void clear() {
        LEVELS.clear();
    }

    /** @return null for levels not synced */
    public static Map<ItemMatcher, Integer> getRequirementsFor(int level) {
        Level entry = LEVELS.get(level);
        return entry == null ? null : entry.requirements();
    }

    /** @return -1 (unlimited) for levels not synced */
    public static int getClaimLimitForLevel(int level) {
        Level entry = LEVELS.get(level);
        return entry == null ? -1 : entry.claimLimit();
    }

    public static int getInsuranceSlotsForLevel(int level) {
        Level entry = LEVELS.get(level);
        return entry == null ? 0 : entry.insuranceSlots();
    }
}
