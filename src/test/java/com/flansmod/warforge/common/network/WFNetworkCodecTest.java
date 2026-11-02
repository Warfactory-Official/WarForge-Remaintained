package com.flansmod.warforge.common.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WFNetworkCodecTest {
    /**
     * Default instance round trip; catches width mismatches (writeVarInt vs readInt) on counts.
     * Default instance unencodable (required field null, registries absent) => skipped; needs its own round trip.
     */
    @Test
    void everyPacketDecodesExactlyWhatItEncodes() {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (WFNetwork.Entry<?> entry : WFNetwork.PACKETS) {
            String name = entry.type().getSimpleName();
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                entry.factory().get().encodeInto(buf);
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            checked++;
            int written = buf.readableBytes();
            try {
                entry.factory().get().decodeInto(buf);
                if (buf.readableBytes() != 0) {
                    failures.add(name + ": " + buf.readableBytes() + " of " + written + " bytes unread");
                }
            } catch (RuntimeException | LinkageError e) {
                failures.add(name + ": " + e);
            }
        }
        assertTrue(checked > WFNetwork.PACKETS.size() / 2, "only " + checked + " packets encodable");
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void claimMapDeltaRoundTrip() {
        PacketClaimMapDelta out = new PacketClaimMapDelta();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        out.sets.add(new PacketClaimMapDelta.Set(Level.OVERWORLD, 1, 2, a, 0x112233));
        out.sets.add(new PacketClaimMapDelta.Set(Level.OVERWORLD, 3, 4, b, 0x445566));
        out.sets.add(new PacketClaimMapDelta.Set(Level.OVERWORLD, 5, 6, a, 0x112233));
        out.removes.add(new PacketClaimMapDelta.Remove(Level.OVERWORLD, 7, 8));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        out.encodeInto(buf);
        PacketClaimMapDelta in = new PacketClaimMapDelta();
        in.decodeInto(buf);
        assertEquals(0, buf.readableBytes());
        assertEquals(out.sets, in.sets);
        assertEquals(out.removes, in.removes);
    }
}
