package com.flansmod.warforge.client.util;

import com.flansmod.warforge.client.render.Skins;

import com.flansmod.warforge.client.render.Gui2D;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class SkinUtil {
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

    public static void drawFace(Gui2D graphics, UUID uuid, int x, int y, int size) {
        // Sibling tinted draws leave the shader colour non-white: face renders tinted or invisible.
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.flush();
        graphics.playerFace(getPlayerFace(uuid), x, y, size);
        graphics.flush();
    }
}
