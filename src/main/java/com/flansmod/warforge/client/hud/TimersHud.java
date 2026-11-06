package com.flansmod.warforge.client.hud;

import com.flansmod.warforge.client.ClientConfig;
import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.HudClock;
import com.flansmod.warforge.client.util.ScreenSpaceUtil;
import com.flansmod.warforge.client.util.ScreenSpaceUtil.ScreenPos;
import com.flansmod.warforge.common.WarForgeConfig;
import net.minecraft.client.Minecraft;

public final class TimersHud {
    private static final int PADDING = 4;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private static final TimeFormat.CachedTimer SIEGE = new TimeFormat.CachedTimer("warforge.client.timer.siege");
    private static final TimeFormat.CachedTimer YIELD = new TimeFormat.CachedTimer("warforge.client.timer.yield");

    private static long nextSiegeDayMs;
    private static long nextYieldDayMs;

    private TimersHud() {
    }

    public static void setDeadlines(long siegeDayMs, long yieldDayMs) {
        nextSiegeDayMs = siegeDayMs;
        nextYieldDayMs = yieldDayMs;
    }

    public static void reset() {
        nextSiegeDayMs = 0L;
        nextYieldDayMs = 0L;
    }

    public static void render(Minecraft mc, Gui2D graphics) {
        long now = HudClock.now();
        ScreenPos pos = ClientConfig.POS_TIMERS;
        int lineHeight = ScreenSpaceUtil.textHeight() + PADDING;

        // 0 = never synced (before the first time update, or a replay recorded without one): nothing to count down
        if ((!WarForgeConfig.SIEGE_ENABLE_NEW_TIMER || SiegeHud.isDebugEnabled()) && nextSiegeDayMs != 0L) {
            drawLine(mc, graphics, pos, lineHeight, SIEGE, nextSiegeDayMs - now);
        }
        if (nextYieldDayMs != 0L) {
            drawLine(mc, graphics, pos, lineHeight, YIELD, nextYieldDayMs - now);
        }
    }

    private static void drawLine(Minecraft mc, Gui2D graphics, ScreenPos pos, int lineHeight,
                                 TimeFormat.CachedTimer timer, long msRemaining) {
        String text = timer.get(msRemaining);
        int textWidth = timer.width(mc.font);
        int x = ScreenSpaceUtil.shouldCenterX(pos)
                ? ScreenSpaceUtil.centerX(ScreenSpaceUtil.screenWidth(), textWidth)
                : ScreenSpaceUtil.getX(pos, textWidth) + ScreenSpaceUtil.getXOffset(pos, PADDING);
        int y = ScreenSpaceUtil.currentY(pos) + ScreenSpaceUtil.getYOffset(pos, lineHeight);
        graphics.text(mc.font, text, x, y, TEXT_COLOR, true);
        ScreenSpaceUtil.incrementY(pos, lineHeight);
    }
}
