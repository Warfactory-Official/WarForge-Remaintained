package com.flansmod.warforge.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;

/** Permission levels are permission sets from 1.21.11. */
public final class Perms {
    private Perms() {}

    public static boolean isGameMaster(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
