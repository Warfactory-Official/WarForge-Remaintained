package com.flansmod.warforge.server;

import java.util.Collection;
import java.util.UUID;

/** Hook for mirroring factions into another mod's team model; no-op until a {@link Link} is installed. */
public final class FactionTeams {
    public interface Link {
        void sync(Faction faction);

        void dissolve(UUID factionId, Collection<UUID> formerMembers);

        void playerLoggedIn(UUID player);
    }

    private static Link link;

    private FactionTeams() {}

    public static void install(Link installed) {
        link = installed;
    }

    static void sync(Faction faction) {
        if (link != null) link.sync(faction);
    }

    static void playerLoggedIn(UUID player) {
        if (link != null) link.playerLoggedIn(player);
    }

    static void dissolve(UUID factionId, Collection<UUID> formerMembers) {
        if (link != null) link.dissolve(factionId, formerMembers);
    }
}
