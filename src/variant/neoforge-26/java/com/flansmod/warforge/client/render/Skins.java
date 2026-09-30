package com.flansmod.warforge.client.render;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Player skin textures. The skin API differs between versions. */
public final class Skins {
    private Skins() {}

    public static Identifier texture(PlayerInfo info) {
        return info.getSkin().body().texturePath();
    }

    public static Identifier defaultTexture(UUID uuid) {
        return DefaultPlayerSkin.get(uuid).body().texturePath();
    }
}
