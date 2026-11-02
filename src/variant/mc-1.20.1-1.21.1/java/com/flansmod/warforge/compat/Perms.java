package com.flansmod.warforge.compat;

import net.minecraft.commands.CommandSourceStack;

/** Permission levels became permission sets in 1.21.11. */
public final class Perms {
    private Perms() {}

    public static boolean isGameMaster(CommandSourceStack source) {
        return source.hasPermission(2);
    }
}
