package com.flansmod.warforge.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * System that parses and caches player's skins
 */
public class SkinUtil {
    /** Normalized (0..1) UV bounds of the 8x8 head face within the 64x64 skin sheet. */
    public static final float FACE_U0 = PlayerFaceRenderer.SKIN_HEAD_U / (float) PlayerFaceRenderer.SKIN_TEX_WIDTH;
    public static final float FACE_V0 = PlayerFaceRenderer.SKIN_HEAD_V / (float) PlayerFaceRenderer.SKIN_TEX_HEIGHT;
    public static final float FACE_U1 = (PlayerFaceRenderer.SKIN_HEAD_U + PlayerFaceRenderer.SKIN_HEAD_WIDTH) / (float) PlayerFaceRenderer.SKIN_TEX_WIDTH;
    public static final float FACE_V1 = (PlayerFaceRenderer.SKIN_HEAD_V + PlayerFaceRenderer.SKIN_HEAD_HEIGHT) / (float) PlayerFaceRenderer.SKIN_TEX_HEIGHT;

    /** Normalized (0..1) UV bounds of the 8x8 hat overlay within the 64x64 skin sheet. */
    public static final float HAT_U0 = PlayerFaceRenderer.SKIN_HAT_U / (float) PlayerFaceRenderer.SKIN_TEX_WIDTH;
    public static final float HAT_V0 = PlayerFaceRenderer.SKIN_HAT_V / (float) PlayerFaceRenderer.SKIN_TEX_HEIGHT;
    public static final float HAT_U1 = (PlayerFaceRenderer.SKIN_HAT_U + PlayerFaceRenderer.SKIN_HAT_WIDTH) / (float) PlayerFaceRenderer.SKIN_TEX_WIDTH;
    public static final float HAT_V1 = (PlayerFaceRenderer.SKIN_HAT_V + PlayerFaceRenderer.SKIN_HAT_HEIGHT) / (float) PlayerFaceRenderer.SKIN_TEX_HEIGHT;

    /**
     * Resolves the full skin texture for a player UUID: the connection's {@link PlayerInfo} skin if
     * the player is known, else the default Steve/Alex skin for that UUID.
     */
    public static ResourceLocation getPlayerFace(UUID uuid) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(uuid);
            if (info != null) {
                return info.getSkinLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }

    /** Draws the player's head face plus hat overlay at the given position via {@link PlayerFaceRenderer}. */
    public static void drawFace(GuiGraphics graphics, UUID uuid, int x, int y, int size) {
        // GuiGraphics.blit modulates by the RenderSystem shader color; a sibling widget (e.g. a tinted
        // draw in a roster row) can leave it non-white, which renders the face tinted or invisible.
        // Force it back to opaque white first, like other face draws in this mod do.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        PlayerFaceRenderer.draw(graphics, getPlayerFace(uuid), x, y, size);
    }
}
