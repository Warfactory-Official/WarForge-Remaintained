package com.flansmod.warforge.compat.ftbteams;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.data.PartyTeam;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

final class FtbProfiles {
    private FtbProfiles() {}

    static void transferOwnership(PartyTeam party, MinecraftServer server, UUID newOwner) throws CommandSyntaxException {
        party.transferOwnership(server.createCommandSourceStack(), new GameProfile(newOwner, ""));
    }
}
