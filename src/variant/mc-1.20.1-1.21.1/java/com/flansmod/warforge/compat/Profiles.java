package com.flansmod.warforge.compat;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/** Player name/UUID lookups and op checks. The server's profile cache API differs between versions. */
public final class Profiles {
    private Profiles() {}

    public static Optional<PlayerRef> get(MinecraftServer server, UUID id) {
        GameProfileCache cache = server.getProfileCache();
        return cache == null ? Optional.empty() : cache.get(id).map(Profiles::ref);
    }

    public static Optional<PlayerRef> get(MinecraftServer server, String name) {
        GameProfileCache cache = server.getProfileCache();
        return cache == null ? Optional.empty() : cache.get(name).map(Profiles::ref);
    }

    public static boolean isOp(MinecraftServer server, Player player) {
        return server.getPlayerList().isOp(player.getGameProfile());
    }

    private static PlayerRef ref(GameProfile profile) {
        return new PlayerRef(profile.getId(), profile.getName());
    }
}
