package com.flansmod.warforge.client.util;

import com.flansmod.warforge.client.render.Skins;

import com.flansmod.warforge.client.render.Gui2D;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * System that parses and caches player's skins
 */
public class SkinUtil {
     /**
     * Resolves the full skin texture for a player UUID: the connection's {@link PlayerInfo} skin if
     * the player is known, else the default Steve/Alex skin for that UUID.
     */
    public static ResourceLocation getPlayerFace(UUID uuid) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(uuid);
            if (info != null) {
                return Skins.texture(info);
            }
        }
        return Skins.defaultTexture(uuid);
    }

    /** Draws the player's head face plus hat overlay at the given position. */
    public static void drawFace(Gui2D graphics, UUID uuid, int x, int y, int size) {
        // GuiGraphics.blit modulates by the RenderSystem shader color; a sibling widget (e.g. a tinted
        // draw in a roster row) can leave it non-white, which renders the face tinted or invisible.
        // Force it back to opaque white first, like other face draws in this mod do.
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.flush();
        graphics.playerFace(getPlayerFace(uuid), x, y, size);
        graphics.flush();
    }
}
