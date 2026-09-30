package com.flansmod.warforge.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/** Player name/UUID lookups and op checks. The server's profile cache API differs between versions. */
public final class Profiles {
    private Profiles() {}

    public static Optional<PlayerRef> get(MinecraftServer server, UUID id) {
        return server.services().nameToIdCache().get(id).map(Profiles::ref);
    }

    public static Optional<PlayerRef> get(MinecraftServer server, String name) {
        return server.services().nameToIdCache().get(name).map(Profiles::ref);
    }

    public static boolean isOp(MinecraftServer server, Player player) {
        return server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));
    }

    private static PlayerRef ref(NameAndId entry) {
        return new PlayerRef(entry.id(), entry.name());
    }
}
