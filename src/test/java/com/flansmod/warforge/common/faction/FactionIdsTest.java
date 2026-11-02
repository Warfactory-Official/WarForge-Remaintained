package com.flansmod.warforge.common.faction;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionIdsTest {
    @Test
    void zonesAreNeutralAndDistinct() {
        assertTrue(FactionIds.isNeutralZone(FactionIds.SAFE_ZONE));
        assertTrue(FactionIds.isNeutralZone(FactionIds.WAR_ZONE));
        assertNotEquals(FactionIds.SAFE_ZONE, FactionIds.WAR_ZONE);
        assertFalse(FactionIds.isNeutralZone(FactionIds.createUUID("someone")));
    }

    @Test
    void idsAreStablePerName() {
        assertEquals(FactionIds.createUUID("abc"), FactionIds.createUUID("abc"));
        assertNotEquals(FactionIds.createUUID("abc"), FactionIds.createUUID("abd"));
    }

    @Test
    void noneIsNotValid() {
        assertFalse(FactionIds.isValid(null));
        assertFalse(FactionIds.isValid(FactionIds.NONE));
        assertTrue(FactionIds.isValid(UUID.randomUUID()));
    }

    @Test
    void roleRankFollowsDeclarationOrder() {
        assertTrue(Role.LEADER.atLeast(Role.OFFICER));
        assertTrue(Role.OFFICER.atLeast(Role.OFFICER));
        assertFalse(Role.MEMBER.atLeast(Role.OFFICER));
    }
}
