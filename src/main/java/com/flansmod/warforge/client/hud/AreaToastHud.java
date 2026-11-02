package com.flansmod.warforge.client.hud;

import com.flansmod.warforge.client.ClientConfig;
import com.flansmod.warforge.client.ClientBorderCache;
import com.flansmod.warforge.client.ClientFlagRegistry;
import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.util.ScreenSpaceUtil;
import com.flansmod.warforge.client.util.ScreenSpaceUtil.ScreenPos;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class AreaToastHud {
    private static final int TOAST_HEIGHT = 24;
    private static final int TOP_ANCHOR_EXTRA = 24;
    private static final int BAR_GAP = 14;
    private static final int TEXT_X_OFFSET = 10;
    private static final int TEXT_Y = 11;
    private static final int TOP_LINE_MARGIN = 50;
    private static final int BOTTOM_LINE_MARGIN = 25;
    private static final int BOTTOM_LINE_Y = TOAST_HEIGHT - 1;
    private static final int FLAG_MAX_WIDTH = 64;
    private static final int FLAG_MAX_HEIGHT = 24;
    private static final int FLAG_GAP = 4;
    private static final float FADE_OUT_SPAN = 2.0f;
    private static final int RGB_MASK = 0xFFFFFF;

    private static float remainingTicks;
    private static String message = "";
    private static int messageWidth = -1;
    private static int messageColour = 0xFFFFFFFF;
    private static String flagId = "";

    private AreaToastHud() {
    }

    public static void reset() {
        remainingTicks = 0;
        message = "";
        messageWidth = -1;
        flagId = "";
    }

    public static void tick() {
        if (remainingTicks > 0.0f) {
            remainingTicks--;
        }
    }

    public static void onChunkChanged(ResourceKey<Level> fromDim, int fromX, int fromZ, ResourceKey<Level> toDim, int toX, int toZ) {
        if (ClientConfig.SHOW_NEW_AREA_TIMER <= 0.0f) {
            return;
        }
        ClaimChunkInfo pre = ClientBorderCache.get(fromDim, fromX, fromZ);
        ClaimChunkInfo post = ClientBorderCache.get(toDim, toX, toZ);
        boolean hadPre = pre != null && FactionIds.isValid(pre.factionId);
        boolean hasPost = post != null && FactionIds.isValid(post.factionId);

        if (!hadPre) {
            if (hasPost) {
                show(I18n.get("warforge.client.area.entering", post.factionName), post.colour, post.flagId);
            }
        } else if (!hasPost) {
            show(I18n.get("warforge.client.area.leaving", pre.factionName), pre.colour, "");
        } else if (!pre.factionId.equals(post.factionId)) {
            show(I18n.get("warforge.client.area.switching", pre.factionName, post.factionName), post.colour, post.flagId);
        }
    }

    private static void show(String text, int colour, String flag) {
        message = text;
        messageWidth = -1;
        messageColour = colour;
        flagId = flag;
        remainingTicks = ClientConfig.SHOW_NEW_AREA_TIMER;
    }

    public static void render(Minecraft mc, Gui2D graphics) {
        if (remainingTicks <= 0.0f) {
            return;
        }
        if (messageWidth < 0) {
            messageWidth = mc.font.width(message);
        }

        ScreenPos pos = ClientConfig.POS_TOAST_INDICATOR;
        boolean isTop = ScreenSpaceUtil.isTop(pos);
        int extraPadding = pos == ScreenPos.TOP ? TOP_ANCHOR_EXTRA : 0;
        int current = ScreenSpaceUtil.currentY(pos);
        int yText = isTop ? current + BAR_GAP + extraPadding : current - TOAST_HEIGHT - BAR_GAP;
        int xText = ScreenSpaceUtil.getX(pos, messageWidth) + ScreenSpaceUtil.getXOffset(pos, TEXT_X_OFFSET);

        float fadeOut = Math.min(FADE_OUT_SPAN * remainingTicks / ClientConfig.SHOW_NEW_AREA_TIMER, 1.0f);
        int alpha = (int) (fadeOut * 255f) << 24;
        int lineColour = alpha | RGB_MASK;

        graphics.fill(xText - TOP_LINE_MARGIN, yText, xText + messageWidth + TOP_LINE_MARGIN, yText + 1, lineColour);
        graphics.fill(xText - BOTTOM_LINE_MARGIN, yText + BOTTOM_LINE_Y, xText + messageWidth + BOTTOM_LINE_MARGIN,
                yText + TOAST_HEIGHT, lineColour);
        graphics.text(mc.font, message, xText, yText + TEXT_Y, messageColour & RGB_MASK | alpha, true);

        ResourceLocation flagTexture = ClientFlagRegistry.getFlagTexture(flagId);
        int[] dims = ClientFlagRegistry.getFlagDimensions(flagId);
        if (flagTexture != null && dims != null && dims[0] > 0 && dims[1] > 0) {
            float scale = Math.min(FLAG_MAX_WIDTH / (float) dims[0], FLAG_MAX_HEIGHT / (float) dims[1]);
            int drawWidth = Math.max(1, Math.round(dims[0] * scale));
            int drawHeight = Math.max(1, Math.round(dims[1] * scale));
            int flagX = ScreenSpaceUtil.getX(pos, drawWidth) + ScreenSpaceUtil.getXOffset(pos, 0);
            int flagY = yText + TOAST_HEIGHT + FLAG_GAP;

            graphics.setColor(1f, 1f, 1f, fadeOut);
            graphics.blit(flagTexture, flagX, flagY, drawWidth, drawHeight, 0, 0, dims[0], dims[1], dims[0], dims[1]);
            graphics.setColor(1f, 1f, 1f, 1f);
            ScreenSpaceUtil.incrementY(pos, drawHeight + FLAG_GAP);
        }

        ScreenSpaceUtil.incrementY(pos, TOAST_HEIGHT + BAR_GAP + extraPadding);
    }
}
