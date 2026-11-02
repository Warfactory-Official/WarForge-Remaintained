package com.flansmod.warforge.client.ui.model;

import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.PacketBase;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public final class DataRevision {
    private static final FriendlyByteBuf SCRATCH = new FriendlyByteBuf(Unpooled.buffer(1024));

    private byte[] last = new byte[0];
    private byte[] next = new byte[0];
    private int lastLength = -1;
    private @Nullable Object screen;

    public boolean unchanged(@Nullable Object currentScreen, PacketBase data) {
        SCRATCH.clear();
        data.encodeInto(SCRATCH);
        int length = SCRATCH.readableBytes();
        if (next.length < length) {
            next = new byte[Math.max(length, next.length * 2)];
        }
        SCRATCH.getBytes(0, next, 0, length);
        boolean same = screen != null && screen == currentScreen && length == lastLength
                && Arrays.equals(next, 0, length, last, 0, length);
        byte[] swap = last;
        last = next;
        next = swap;
        lastLength = length;
        return same;
    }

    public boolean unchanged(@Nullable Object currentScreen, FactionDisplayInfo info) {
        PacketFactionInfo packet = new PacketFactionInfo();
        packet.info = info;
        return unchanged(currentScreen, packet);
    }

    public void shown(Object builtScreen) {
        screen = builtScreen;
    }
}
