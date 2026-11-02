package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.Role;

import java.util.function.IntSupplier;

// minRole: OWN_FACTION_ROLE; radius (chunks): IN_RANGE. Domain rules stay in handlers.
public record Exposure(Kind kind, Role minRole, IntSupplier radius) {
    public enum Kind {
        /** Answers contain only data any player may see. Reviewed allowlist in the regression test. */
        PUBLIC,
        /** Acts on or reads the sender's own state; the sender needs no faction. */
        SELF,
        /** Sender must be in a faction; the answer is that faction's data. */
        OWN_FACTION,
        /** Sender must be in a faction with at least the role (ops pass). */
        OWN_FACTION_ROLE,
        /** The packet addresses a chunk, which must be in the sender's dimension within the radius of the sender. */
        IN_RANGE
    }

    public static final Exposure PUBLIC = new Exposure(Kind.PUBLIC, null, null);
    public static final Exposure SELF = new Exposure(Kind.SELF, null, null);
    public static final Exposure OWN_FACTION = new Exposure(Kind.OWN_FACTION, null, null);

    public static Exposure ownFactionRole(Role minRole) {
        return new Exposure(Kind.OWN_FACTION_ROLE, minRole, null);
    }

    public static Exposure inRange(IntSupplier radiusChunks) {
        return new Exposure(Kind.IN_RANGE, null, radiusChunks);
    }
}
