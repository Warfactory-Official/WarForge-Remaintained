package com.flansmod.warforge.client;

import com.flansmod.warforge.common.blocks.TileEntityClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ClaimFlagRenderer {
    private static final double RENDER_DISTANCE_SQ = 64.0 * 64.0;
    private static final double HALF_WIDTH = 0.45;
    private static final double BANNER_BOTTOM = 1.5;
    private static final double BANNER_TOP = 2.1;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            return;
        }

        float partialTicks = event.getPartialTicks();
        EntityPlayer player = mc.player;
        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        boolean begun = false;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        for (TileEntity tile : mc.world.loadedTileEntityList) {
            if (!(tile instanceof TileEntityClaim)) {
                continue;
            }
            TileEntityClaim claim = (TileEntityClaim) tile;
            if (claim.factionFlagId == null || claim.factionFlagId.isEmpty()) {
                continue;
            }
            BlockPos pos = claim.getPos();
            double dx = pos.getX() + 0.5 - camX;
            double dy = pos.getY() - camY;
            double dz = pos.getZ() + 0.5 - camZ;
            if (dx * dx + dy * dy + dz * dz > RENDER_DISTANCE_SQ) {
                continue;
            }
            ResourceLocation texture = ClientFlagRegistry.getFlagTexture(claim.factionFlagId);
            if (texture == null) {
                continue;
            }

            if (!begun) {
                begin();
                begun = true;
            }

            mc.getTextureManager().bindTexture(texture);
            GlStateManager.pushMatrix();
            GlStateManager.translate(dx, dy, dz);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(-HALF_WIDTH, BANNER_TOP, 0.0).tex(0.0, 0.0).endVertex();
            buffer.pos(-HALF_WIDTH, BANNER_BOTTOM, 0.0).tex(0.0, 1.0).endVertex();
            buffer.pos(HALF_WIDTH, BANNER_BOTTOM, 0.0).tex(1.0, 1.0).endVertex();
            buffer.pos(HALF_WIDTH, BANNER_TOP, 0.0).tex(1.0, 0.0).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        if (begun) {
            end();
        }
    }

    private void begin() {
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void end() {
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
