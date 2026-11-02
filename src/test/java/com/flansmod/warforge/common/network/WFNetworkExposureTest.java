package com.flansmod.warforge.common.network;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WFNetworkExposureTest {
    /** Reviewed: answers contain only data any player may see. Adding to this set is a deliberate review. */
    private static final Set<String> PUBLIC_ALLOWLIST = Set.of(
            "PacketRequestFactionInfo",
            "PacketRequestLeaderboardInfo",
            "PacketRequestNamePlate");

    @Test
    void everyServerboundPacketDeclaresAnExposure() {
        for (WFNetwork.Entry<?> entry : WFNetwork.PACKETS) {
            boolean serverbound = entry.direction() != WFNetwork.Direction.TO_CLIENT;
            if (serverbound) {
                assertNotNull(entry.exposure(), entry.type().getSimpleName() + " has no exposure");
                assertTrue(ServerboundPacket.class.isAssignableFrom(entry.type()), entry.type().getSimpleName());
            } else {
                assertNull(entry.exposure(), entry.type().getSimpleName());
            }
        }
    }

    @Test
    void publicPacketsMatchTheReviewedAllowlist() {
        Set<String> actual = new TreeSet<>();
        for (WFNetwork.Entry<?> entry : WFNetwork.PACKETS) {
            if (entry.exposure() != null && entry.exposure().kind() == Exposure.Kind.PUBLIC) {
                actual.add(entry.type().getSimpleName());
            }
        }
        assertEquals(new TreeSet<>(PUBLIC_ALLOWLIST), actual);
    }
}
