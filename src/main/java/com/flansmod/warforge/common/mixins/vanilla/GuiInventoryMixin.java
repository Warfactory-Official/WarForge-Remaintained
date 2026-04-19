package com.flansmod.warforge.common.mixins.vanilla;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.factories.ClaimManagerGuiFactory;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiFactory;
import com.flansmod.warforge.common.factories.FactionStatsGuiFactory;
import com.flansmod.warforge.common.network.PacketMoveCitadel;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GuiInventory.class)
public abstract class GuiInventoryMixin extends InventoryEffectRenderer {
    @Unique
    private static final ResourceLocation WARFORGE_BUTTONS_TEXTURE = new ResourceLocation(Tags.MODID, "gui/icon_claim.png");
    @Unique
    private static final int WARFORGE_BUTTON_SIZE = 18;
    @Unique
    private static final int WARFORGE_MEMBERS_BUTTON_TEX_X = WARFORGE_BUTTON_SIZE;
    @Unique
    private static final int WARFORGE_STATS_BUTTON_TEX_X = WARFORGE_BUTTON_SIZE * 2;
    @Unique
    private static final int WARFORGE_MOVE_CITADEL_BUTTON_TEX_X = WARFORGE_BUTTON_SIZE * 3;

    @Unique
    private static final int WARFORGE_CLAIMS_BUTTON_ID = 0x57A9;
    @Unique
    private static final int WARFORGE_MEMBERS_BUTTON_ID = 0x57AB;
    @Unique
    private static final int WARFORGE_STATS_BUTTON_ID = 0x57AD;
    @Unique
    private static final int WARFORGE_MOVE_CITADEL_BUTTON_ID = 0x57AF;

    @Unique
    private GuiButton warforge$claimsButton;
    @Unique
    private GuiButton warforge$membersButton;
    @Unique
    private GuiButton warforge$statsButton;
    @Unique
    private GuiButton warforge$moveCitadelButton;

    public GuiInventoryMixin(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void warforge$addClaimsButton(CallbackInfo ci) {
        if (Minecraft.getMinecraft().player == null) {
            return;
        }

        warforge$claimsButton = new GuiButtonImage(
                WARFORGE_CLAIMS_BUTTON_ID,
                60,
                0,
                WARFORGE_BUTTON_SIZE,
                WARFORGE_BUTTON_SIZE,
                0,
                0,
                0,
                WARFORGE_BUTTONS_TEXTURE
        );
        warforge$updateClaimsButtonPos();
        buttonList.add(warforge$claimsButton);
        warforge$membersButton = warforge$createImageButton(WARFORGE_MEMBERS_BUTTON_ID, WARFORGE_MEMBERS_BUTTON_TEX_X);
        warforge$updateClaimsButtonPos();
        buttonList.add(warforge$membersButton);
        warforge$statsButton = warforge$createImageButton(WARFORGE_STATS_BUTTON_ID, WARFORGE_STATS_BUTTON_TEX_X);
        warforge$updateClaimsButtonPos();
        buttonList.add(warforge$statsButton);
        warforge$moveCitadelButton = warforge$createImageButton(WARFORGE_MOVE_CITADEL_BUTTON_ID, WARFORGE_MOVE_CITADEL_BUTTON_TEX_X);
        warforge$updateClaimsButtonPos();
        buttonList.add(warforge$moveCitadelButton);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void warforge$updateButtonPosition(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        warforge$updateClaimsButtonPos();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void warforge$onActionPerformed(GuiButton button, CallbackInfo ci) throws IOException {
        if (button == null) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }

        if (button.id == WARFORGE_CLAIMS_BUTTON_ID) {
            ClaimManagerGuiFactory.INSTANCE.openClient(new DimChunkPos(player.dimension, player.getPosition()), WarForgeConfig.CLAIM_MANAGER_RADIUS, -1, -1);
            ci.cancel();
        } else if (button.id == WARFORGE_MEMBERS_BUTTON_ID) {
            FactionMemberManagerGuiFactory.INSTANCE.openClient(FactionMemberManagerGuiData.Page.MEMBERS);
            ci.cancel();
        } else if (button.id == WARFORGE_STATS_BUTTON_ID) {
            FactionStatsGuiFactory.INSTANCE.openClient(com.flansmod.warforge.server.Faction.nullUuid);
            ci.cancel();
        } else if (button.id == WARFORGE_MOVE_CITADEL_BUTTON_ID) {
            RayTraceResult hit = player.rayTrace(10.0d, 1.0F);
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && hit.sideHit != null) {
                PacketMoveCitadel packet = new PacketMoveCitadel();
                packet.pos = new com.flansmod.warforge.common.util.DimBlockPos(player.dimension, hit.getBlockPos().offset(hit.sideHit));
                com.flansmod.warforge.common.WarForgeMod.NETWORK.sendToServer(packet);
            }
            ci.cancel();
        }
    }

    @Unique
    private GuiButtonImage warforge$createImageButton(int id, int textureX) {
        return new GuiButtonImage(
                id,
                60,
                0,
                WARFORGE_BUTTON_SIZE,
                WARFORGE_BUTTON_SIZE,
                textureX,
                0,
                0,
                WARFORGE_BUTTONS_TEXTURE
        );
    }

    @Unique
    private void warforge$updateClaimsButtonPos() {
        if (warforge$claimsButton == null) {
            return;
        }

        warforge$claimsButton.x = guiLeft + xSize  + 4 ;
        warforge$claimsButton.y = guiTop + 4;
        if (warforge$membersButton != null) {
            warforge$membersButton.x = guiLeft + xSize + 4;
            warforge$membersButton.y = guiTop + 26;
        }
        if (warforge$statsButton != null) {
            warforge$statsButton.x = guiLeft + xSize + 4;
            warforge$statsButton.y = guiTop + 48;
        }
        if (warforge$moveCitadelButton != null) {
            warforge$moveCitadelButton.x = guiLeft + xSize + 4;
            warforge$moveCitadelButton.y = guiTop + 70;
        }

    }
}
