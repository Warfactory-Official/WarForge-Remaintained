package com.flansmod.warforge.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;

/** Command permission checks. Permission levels became permission sets in 1.21.11. */
public final class Perms {
    private Perms() {}

    /** Permission level 2 ("game master": most cheat commands). */
    public static boolean isGameMaster(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
