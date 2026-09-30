package com.flansmod.warforge.client.util;

import com.flansmod.warforge.client.PlayerNametagCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

public class FullColorNameplate {
    /** A faction plate recorded during extraction, with the entity facts that the drawing needs. */
    public record Plate(PlayerNametagCache.NamePlateData faction, float bbHeight, boolean sneaking,
                        EntityRenderer<?, ?> renderer) {
    }

    /** The plate is drawn this many text pixels above the name tag, as before 26.x. */
    private static final int VERTICAL_SHIFT = -10;

    /**
     * Submits a faction nameplate with the same transform and text passes as the 1.21.1 renderer: at the entity
     * position plus render offset, lifted to head height, turned to the camera and scaled to text size.
     */
    public static void submit(Plate plate, EntityRenderState state, CameraRenderState camera, PoseStack pose,
                              SubmitNodeCollector collector) {
        Font font = Minecraft.getInstance().font;
        Component name = Component.literal(plate.faction().name);
        Vec3 offset = renderOffset(plate.renderer(), state);

        pose.pushPose();
        pose.translate(state.x - camera.pos.x + offset.x, state.y - camera.pos.y + offset.y, state.z - camera.pos.z + offset.z);
        pose.translate(0.0F, plate.bbHeight() + 0.5F, 0.0F);
        pose.mulPose(camera.orientation);
        pose.scale(-0.025F, -0.025F, 0.025F);

        float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int bgColor = (int) (backgroundOpacity * 255.0F) << 24;
        FormattedCharSequence text = name.getVisualOrderText();
        float xOffset = -font.width(name) / 2.0F;
        int color = textColor(plate.faction().color);
        int darker = textColor(plate.faction().darkerColor);

        if (plate.sneaking()) {
            collector.submitText(pose, xOffset, VERTICAL_SHIFT, text, false, Font.DisplayMode.NORMAL, state.lightCoords, darker, bgColor, 0);
        } else {
            // First pass behind blocks (see-through), second pass on top, matching vanilla.
            collector.submitText(pose, xOffset, VERTICAL_SHIFT, text, false, Font.DisplayMode.SEE_THROUGH, state.lightCoords, darker, bgColor, 0);
            collector.submitText(pose, xOffset, VERTICAL_SHIFT, text, false, Font.DisplayMode.NORMAL, state.lightCoords, color, 0, 0);
        }

        pose.popPose();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Vec3 renderOffset(EntityRenderer<?, ?> renderer, EntityRenderState state) {
        return ((EntityRenderer) renderer).getRenderOffset(state);
    }

    /** Before 1.21.6 a text colour with an alpha below 4 was drawn opaque. 26.x draws it invisible. */
    private static int textColor(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}
