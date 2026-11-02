package com.flansmod.warforge.client;

import com.flansmod.warforge.api.Color4i;
import com.flansmod.warforge.api.WarforgeCache;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRequestNamePlate;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;

public class PlayerNametagCache {
    public static final PlayerNametagCache INSTANCE = new PlayerNametagCache(60_000, 200);

    private static final float DARKER_BRIGHTNESS = 0.25f;
    private static final NamePlateData AWAITING_DATA = new NamePlateData("<awaiting>", 0xFFFFFF, 0xFFFFFF);
    private static final NamePlateData NO_PLATE = new NamePlateData("", 0xFFFFFF, 0xFFFFFF);

    private final WarforgeCache<String, NamePlateData> cache;

    public PlayerNametagCache(long maxAgeMillis, int maxSize) {
        cache = new WarforgeCache<>(maxAgeMillis, maxSize);
    }

    public void purge() {
        cache.clear();
    }

    public void add(String name, String faction, int color) {
        cache.put(name, faction.isEmpty() ? NO_PLATE
                : new NamePlateData(faction, color, Color4i.fromRGB(color).withBrightness(DARKER_BRIGHTNESS).toRGB()));
    }

    public void remove(String name) {
        cache.remove(name);
    }

    public @Nullable NamePlateData requestIfAbsent(String player) {
        NamePlateData data = cache.get(player);
        if (data == null) {
            PacketRequestNamePlate packet = new PacketRequestNamePlate();
            packet.name = player;
            WarForgeMod.NETWORK.sendToServer(packet);
            cache.put(player, AWAITING_DATA);
            return null;
        }
        return data == AWAITING_DATA || data == NO_PLATE ? null : data;
    }

    @RequiredArgsConstructor
    public static class NamePlateData {
        public final String name;
        public final int color;
        public final int darkerColor;
    }
}
