package com.flansmod.warforge.api;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Cache purged by age and size. Entries stay in insertion order; overwriting a key re-inserts it as newest.
 *
 * @param <K> Key
 * @param <V> Value
 * @author MrNorwood
 */
public class WarforgeCache<K, V> {

    private final LinkedHashMap<K, Entry<V>> cache = new LinkedHashMap<>();
    private final long maxAgeMillis; // 0 disables expiry
    private final int maxSize; // 0 is unbounded

    public WarforgeCache(long maxAgeMillis, int maxSize) {
        this.maxAgeMillis = maxAgeMillis;
        this.maxSize = maxSize;
    }

    public synchronized void put(K key, V value) {
        cache.remove(key);
        purgeExpired();
        if (maxSize > 0 && cache.size() >= maxSize) {
            Iterator<Entry<V>> oldest = cache.values().iterator();
            oldest.next();
            oldest.remove();
        }
        cache.put(key, new Entry<>(value, System.currentTimeMillis()));
    }

    public synchronized V get(K key) {
        purgeExpired();
        Entry<V> entry = cache.get(key);
        return entry != null ? entry.value : null;
    }

    public synchronized boolean contains(K key) {
        purgeExpired();
        return cache.containsKey(key);
    }

    public synchronized int size() {
        purgeExpired();
        return cache.size();
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized void remove(K key) {
        cache.remove(key);
    }

    private void purgeExpired() {
        if (maxAgeMillis <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Entry<V>> it = cache.values().iterator();
        while (it.hasNext() && now - it.next().timestamp > maxAgeMillis) {
            it.remove();
        }
    }

    private record Entry<V>(V value, long timestamp) {}
}
