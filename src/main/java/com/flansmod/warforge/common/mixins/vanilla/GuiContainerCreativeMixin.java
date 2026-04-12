package com.flansmod.warforge.common.mixins.vanilla;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRequestClaimChunks;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GuiContainerCreative.class)
public abstract class GuiContainerCreativeMixin extends InventoryEffectRenderer {

    @Unique
    private static final int WARFORGE_CREATIVE_CLAIMS_BUTTON_ID = 0x57AA;

    @Unique
    private GuiButton warforge$claimsButton;

    public GuiContainerCreativeMixin(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void warforge$addClaimsButton(CallbackInfo ci) {
        if (Minecraft.getMinecraft().player == null) {
            return;
        }

        warforge$claimsButton = new GuiButton(
                WARFORGE_CREATIVE_CLAIMS_BUTTON_ID,
                0,
                0,
                58,
                20,
                I18n.format("gui.warforge.claims")
        );
        warforge$updateClaimsButtonPos();
        buttonList.add(warforge$claimsButton);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void warforge$updateButtonPosition(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        warforge$updateClaimsButtonPos();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void warforge$onActionPerformed(GuiButton button, CallbackInfo ci) throws IOException {
        if (button == null || button.id != WARFORGE_CREATIVE_CLAIMS_BUTTON_ID) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }

        PacketRequestClaimChunks packet = new PacketRequestClaimChunks();
        packet.center = new DimChunkPos(player.dimension, player.getPosition());
        packet.radius = WarForgeConfig.CLAIM_MANAGER_RADIUS;
        packet.openUi = true;
        WarForgeMod.NETWORK.sendToServer(packet);
        ci.cancel();
    }

    @Unique
    private void warforge$updateClaimsButtonPos() {
        if (warforge$claimsButton == null) {
            return;
        }

        warforge$claimsButton.x = guiLeft + xSize - warforge$claimsButton.width - 4;
        warforge$claimsButton.y = guiTop + 4;
    }
}
