package com.flansmod.warforge.client;

import com.flansmod.warforge.client.ui.model.ClaimMapModel;
import com.flansmod.warforge.client.ui.UiNavigation;
import com.flansmod.warforge.client.border.ClaimBorderRenderer;
import com.flansmod.warforge.client.hud.VeinHud;
import com.flansmod.warforge.client.render.KeyMaps;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

public final class ClientKeybinds {
    private static final String CATEGORY = "key.warforge.category";

    public static final KeyMapping TOGGLE_BORDERS = KeyMaps.create("key.warforge.showborders", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping CLAIM_MANAGER = KeyMaps.create("key.warforge.claimmanager", GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping TOGGLE_VEIN_OVERLAY = KeyMaps.create("key.warforge.toggleveinoverlay", GLFW.GLFW_KEY_O, CATEGORY);

    private ClientKeybinds() {
    }

    public static void tick(LocalPlayer player, ResourceKey<Level> dim, int chunkX, int chunkZ) {
        if (CLAIM_MANAGER.consumeClick()) {
            ClaimMapModel.resetSiegeState();
            UiNavigation.openClaimManager(new DimChunkPos(dim, chunkX, chunkZ), WarForgeConfig.CLAIM_MANAGER_RADIUS, -1, -1);
        }
        if (TOGGLE_BORDERS.consumeClick()) {
            boolean shown = ClaimBorderRenderer.toggleVisible();
            player.sendSystemMessage(Component.translatable(shown ? "warforge.client.borders.enabled" : "warforge.client.borders.disabled"));
        }
        if (TOGGLE_VEIN_OVERLAY.consumeClick()) {
            if (!WarForgeConfig.ENABLE_CHUNK_VEINS) {
                player.sendSystemMessage(Component.translatable("warforge.client.veins.disabled"));
                return;
            }
            boolean shown = VeinHud.toggleVisible();
            player.sendSystemMessage(Component.translatable(shown ? "warforge.info.vein.toggle.enabled" : "warforge.info.vein.toggle.disabled"));
        }
    }
}
