package com.flansmod.warforge.common.util;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.UUID;

/**
 * Helpers for showing a player's faction as a coloured prefix in chat and the tab list.
 * <p>
 * Chat is rewritten through {@code ServerChatEvent}; the tab list display name is provided by a mixin
 * on {@code EntityPlayerMP#getTabListDisplayName} and pushed to clients with {@link #refreshTabName}.
 */
public final class FactionDisplay {
    private FactionDisplay() {
    }

    // The 16 vanilla chat colours and their foreground RGB values, used to approximate a faction's
    // RGB colour (1.12.2 chat/tab text can only use these named colours).
    private static final TextFormatting[] COLOURS = {
            TextFormatting.BLACK, TextFormatting.DARK_BLUE, TextFormatting.DARK_GREEN, TextFormatting.DARK_AQUA,
            TextFormatting.DARK_RED, TextFormatting.DARK_PURPLE, TextFormatting.GOLD, TextFormatting.GRAY,
            TextFormatting.DARK_GRAY, TextFormatting.BLUE, TextFormatting.GREEN, TextFormatting.AQUA,
            TextFormatting.RED, TextFormatting.LIGHT_PURPLE, TextFormatting.YELLOW, TextFormatting.WHITE
    };
    private static final int[] COLOUR_RGB = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    public static TextFormatting nearestColour(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int best = COLOURS.length - 1;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < COLOUR_RGB.length; i++) {
            int cr = (COLOUR_RGB[i] >> 16) & 0xFF, cg = (COLOUR_RGB[i] >> 8) & 0xFF, cb = COLOUR_RGB[i] & 0xFF;
            long d = (long) (r - cr) * (r - cr) + (long) (g - cg) * (g - cg) + (long) (b - cb) * (b - cb);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return COLOURS[best];
    }

    /** The "[FactionName] " prefix coloured to the faction's colour, or {@code null} if the player has no faction. */
    public static ITextComponent factionPrefix(Faction faction) {
        if (faction == null || faction.uuid.equals(Faction.nullUuid) || faction.name == null || faction.name.isEmpty()) {
            return null;
        }
        TextComponentString prefix = new TextComponentString("[" + faction.name + "] ");
        prefix.getStyle().setColor(nearestColour(faction.colour));
        return prefix;
    }

    /** Returns {@code original} with the faction prefix prepended, or {@code original} unchanged if no faction. */
    public static ITextComponent withChatPrefix(Faction faction, ITextComponent original) {
        ITextComponent prefix = factionPrefix(faction);
        if (prefix == null) {
            return original;
        }
        return new TextComponentString("").appendSibling(prefix).appendSibling(original);
    }

    /** The tab-list display name "[FactionName] PlayerName", or {@code null} to fall back to the vanilla name. */
    public static ITextComponent tabName(Faction faction, String playerName) {
        ITextComponent prefix = factionPrefix(faction);
        if (prefix == null) {
            return null;
        }
        return new TextComponentString("").appendSibling(prefix).appendSibling(new TextComponentString(playerName));
    }

    /** Re-send a player's tab-list display name to everyone so faction prefix changes show up live. */
    public static void refreshTabName(UUID playerId) {
        // When the tab prefix is disabled the display name is always the vanilla name, so there is
        // nothing to push and we must not reveal any association to clients.
        if (!WarForgeConfig.FACTION_PREFIX_IN_TABLIST) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || playerId == null) {
            return;
        }
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(playerId);
        if (player != null) {
            server.getPlayerList().sendPacketToAllPlayers(
                    new SPacketPlayerListItem(SPacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, player));
        }
    }

    /** Convenience: refresh the tab names of every online member of a faction (e.g. after a rename). */
    public static void refreshFactionTabNames(Faction faction) {
        if (faction == null) {
            return;
        }
        for (UUID memberId : faction.members.keySet()) {
            refreshTabName(memberId);
        }
    }
}
