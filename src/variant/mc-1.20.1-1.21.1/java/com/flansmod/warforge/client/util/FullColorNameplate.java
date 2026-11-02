package com.flansmod.warforge.client.util;

import java.util.HashMap;
import java.util.Map;
import com.flansmod.warforge.api.client.NameplatePolicy;
import com.flansmod.warforge.api.client.WarforgeClientApi;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public class FullColorNameplate {
    private static final Map<String, Component> NAMES = new HashMap<>();

    public static Component nameComponent(String name) {
        Component component = NAMES.get(name);
        if (component == null) {
            if (NAMES.size() > 256) {
                NAMES.clear();
            }
            component = Component.literal(name);
            NAMES.put(name, component);
        }
        return component;
    }

    /** Forge posts RenderNameTagEvent before the head-height lift and billboard => applies the full vanilla transform itself, else text renders flat at the feet. */
    public static void drawNameplate(Font font, Component name, Entity entity, PoseStack pose, MultiBufferSource buffers, int verticalShift, boolean isSneaking, int color, int darker, int packedLight) {
        NameplatePolicy.Mode mode = WarforgeClientApi.nameplateMode(entity);
        if (mode == NameplatePolicy.Mode.HIDDEN) {
            return;
        }
        Font.DisplayMode behind = mode == NameplatePolicy.Mode.SEE_THROUGH ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
        pose.pushPose();
        pose.translate(0.0F, entity.getBbHeight() + 0.5F, 0.0F);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix = pose.last().pose();
        float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int bgColor = (int) (backgroundOpacity * 255.0F) << 24;
        float xOffset = -font.width(name) / 2.0F;
        float yOffset = verticalShift;

        if (isSneaking) {
            font.drawInBatch(name, xOffset, yOffset, darker, false, matrix, buffers, Font.DisplayMode.NORMAL, bgColor, packedLight);
        } else {
            font.drawInBatch(name, xOffset, yOffset, darker, false, matrix, buffers, behind, bgColor, packedLight);
            font.drawInBatch(name, xOffset, yOffset, color, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, packedLight);
        }

        pose.popPose();
    }
}
