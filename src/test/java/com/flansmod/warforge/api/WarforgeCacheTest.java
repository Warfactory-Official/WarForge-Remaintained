package com.flansmod.warforge.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WarforgeCacheTest {
    @Test
    void overwriteKeepsOneEntryAndLatestValue() {
        WarforgeCache<String, Integer> cache = new WarforgeCache<>(0, 2);
        cache.put("a", 1);
        cache.put("a", 2);
        cache.put("b", 3);
        assertEquals(2, cache.size());
        assertEquals(2, cache.get("a"));
        cache.put("c", 4);
        assertNull(cache.get("a"));
        assertEquals(3, cache.get("b"));
        assertEquals(4, cache.get("c"));
    }

    @Test
    void overwrittenKeyIsNotEvictedBeforeOlderKeys() {
        WarforgeCache<String, Integer> cache = new WarforgeCache<>(0, 2);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("a", 3);
        cache.put("c", 4);
        assertNull(cache.get("b"));
        assertEquals(3, cache.get("a"));
    }
}
