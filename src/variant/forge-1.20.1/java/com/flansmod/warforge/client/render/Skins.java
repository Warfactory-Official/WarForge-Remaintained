package com.flansmod.warforge.client.render;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Player skin textures. The skin API differs between versions. */
public final class Skins {
    private Skins() {}

    public static ResourceLocation texture(PlayerInfo info) {
        return info.getSkinLocation();
    }

    public static ResourceLocation defaultTexture(UUID uuid) {
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }
}
