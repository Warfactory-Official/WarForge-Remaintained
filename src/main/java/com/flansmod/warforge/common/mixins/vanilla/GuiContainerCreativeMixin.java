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
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
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

@Mixin(GuiContainerCreative.class)
public abstract class GuiContainerCreativeMixin extends InventoryEffectRenderer {

    @Unique
    private static final int WARFORGE_CREATIVE_CLAIMS_BUTTON_ID = 0x57AA;
    @Unique
    private static final int WARFORGE_CREATIVE_MEMBERS_BUTTON_ID = 0x57AC;
    @Unique
    private static final int WARFORGE_CREATIVE_STATS_BUTTON_ID = 0x57AE;
    @Unique
    private static final int WARFORGE_CREATIVE_MOVE_CITADEL_BUTTON_ID = 0x57B0;

    @Unique
    private GuiButton warforge$claimsButton;
    @Unique
    private GuiButton warforge$membersButton;
    @Unique
    private GuiButton warforge$statsButton;
    @Unique
    private GuiButton warforge$moveCitadelButton;

    public GuiContainerCreativeMixin(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void warforge$addClaimsButton(CallbackInfo ci) {
        if (Minecraft.getMinecraft().player == null) {
            return;
        }

       warforge$claimsButton = new GuiButtonImage(
                WARFORGE_CREATIVE_CLAIMS_BUTTON_ID,
                60,
                0,
                18,
                18,
                0,
                0,
                0,
                new ResourceLocation(Tags.MODID, "gui/icon_claim.png")
        );
        warforge$updateClaimsButtonPos();
        buttonList.add(warforge$claimsButton);
        warforge$membersButton = new GuiButton(WARFORGE_CREATIVE_MEMBERS_BUTTON_ID, 60, 0, 18, 18, "M");
        warforge$updateClaimsButtonPos();
        buttonList.add(warforge$membersButton);
        warforge$statsButton = new GuiButton(WARFORGE_CREATIVE_STATS_BUTTON_ID, 60, 0, 18, 18, "F");
        warforge$updateClaimsButtonPos();
        buttonList.add(warforge$statsButton);
        warforge$moveCitadelButton = new GuiButton(WARFORGE_CREATIVE_MOVE_CITADEL_BUTTON_ID, 60, 0, 18, 18, "C");
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

        if (button.id == WARFORGE_CREATIVE_CLAIMS_BUTTON_ID) {
            ClaimManagerGuiFactory.INSTANCE.openClient(new DimChunkPos(player.dimension, player.getPosition()), WarForgeConfig.CLAIM_MANAGER_RADIUS, -1, -1);
            ci.cancel();
        } else if (button.id == WARFORGE_CREATIVE_MEMBERS_BUTTON_ID) {
            FactionMemberManagerGuiFactory.INSTANCE.openClient(FactionMemberManagerGuiData.Page.MEMBERS);
            ci.cancel();
        } else if (button.id == WARFORGE_CREATIVE_STATS_BUTTON_ID) {
            FactionStatsGuiFactory.INSTANCE.openClient(com.flansmod.warforge.server.Faction.nullUuid);
            ci.cancel();
        } else if (button.id == WARFORGE_CREATIVE_MOVE_CITADEL_BUTTON_ID) {
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
