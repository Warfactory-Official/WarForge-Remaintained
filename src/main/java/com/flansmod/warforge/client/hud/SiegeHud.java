package com.flansmod.warforge.client.hud;

import com.flansmod.warforge.client.ClientConfig;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.ClientClaimChunkCache;
import com.flansmod.warforge.client.ClientSiegeState;
import com.flansmod.warforge.client.SiegeUiDebugCommand;
import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.HudClock;
import com.flansmod.warforge.client.util.ColorUtil;
import com.flansmod.warforge.client.util.ScreenSpaceUtil;
import com.flansmod.warforge.client.util.ScreenSpaceUtil.ScreenPos;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.compat.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class SiegeHud {
    private static final int STRIPE_W = 6;
    private static final int PAD = 6;
    private static final int PANEL_H = 35;
    private static final int NAMES_Y = 4;
    private static final int TRACK_Y = 14;
    private static final int TRACK_H = 8;
    private static final int LABELS_Y = 24;

    private static final int PX_PER_POINT = 18;
    private static final int TRACK_MIN_W = 300;
    private static final int TRACK_MAX_W = 470;
    private static final int TRACK_SCREEN_MARGIN = 80;
    private static final int ICON_SIZE = 10;
    private static final int ABANDON_H = 12;
    private static final int ABANDON_GAP = 2;
    private static final int ABANDON_DANGER_SECONDS = 10;
    private static final long TIMER_DANGER_MS = 60_000L;
    private static final long PULSE_PERIOD_MS = 300L;
    private static final float NOTCH_END_LIGHTEN = 0.15f;
    private static final float FILL_DARKEN = 0.15f;
    private static final float FILL_LIGHTEN = 0.30f;
    private static final float COUNTER_LIGHTEN = 0.25f;
    private static final int GLOW_ALPHA_BASE = 0x30;
    private static final int GLOW_ALPHA_RANGE = 0x50;
    private static final float MARKER_W = 3f;
    private static final float GLOW_PAD = 4f;
    private static final float MARKER_OVERHANG = 2f;
    private static final float CENTER_NOTCH_EXTRA = 4f;
    private static final float MINOR_NOTCH_INSET = 2f;

    private static final ResourceLocation SHIELD_ICON = Ids.of(Tags.MODID, "gui/icon_siege_shield.png");
    private static final ResourceLocation AXE_ICON = Ids.of(Tags.MODID, "gui/icon_siege_axe.png");

    private static final int SECTION_FILL = 0xEE20262B;
    private static final int SECTION_BORDER = 0xEE11161A;
    private static final int TRACK_FILL = ColorUtil.argb(0xFF, 0x0E1216);
    private static final int NOTCH_MINOR = ColorUtil.argb(0x77, 0xC7CCD1);
    private static final int CENTER_LINE = ColorUtil.argb(0xFF, 0xFFFFFF);
    private static final int MARKER_FILL = ColorUtil.argb(0xFF, 0xFFFFFF);
    private static final int TEXT_NEUTRAL = 0xFFFFFFFF;
    private static final int TEXT_DANGER = 0xFFFF5555;
    private static final int TEXT_ABANDON = 0xFFC79A3A;

    private static final Label TO_WIN = new Label();
    private static final Label TO_DEFEND = new Label();
    private static final Label STANDING = new Label();
    private static final Label VERSUS = new Label();
    private static final TimeFormat.CachedTimer END_TIMER = new TimeFormat.CachedTimer(null);
    private static final TimeFormat.CachedTimer ABANDON_TIMER = new TimeFormat.CachedTimer("warforge.client.siege.abandon");

    private static SiegeCampProgressInfo namedInfo;
    private static int defendNameWidth;
    private static int attackNameWidth;

    private SiegeHud() {
    }

    public static boolean isDebugEnabled() {
        return SiegeUiDebugCommand.isEnabled();
    }

    public static void render(Minecraft mc, Gui2D graphics) {
        LocalPlayer player = mc.player;
        SiegeCampProgressInfo info = SiegeUiDebugCommand.isEnabled()
                ? SiegeUiDebugCommand.debugSiege()
                : ClientSiegeState.closest(player);
        if (info != null) {
            draw(mc.font, graphics, info);
        }
    }

    private static void draw(Font font, Gui2D graphics, SiegeCampProgressInfo info) {
        int attackPts = Math.max(1, info.completionPoint);
        int defendPts = Math.max(1, SiegeUiDebugCommand.defenceThreshold());
        int span = attackPts + defendPts;

        int maxTrackW = Math.min(TRACK_MAX_W, Math.max(TRACK_MIN_W, ScreenSpaceUtil.screenWidth() - TRACK_SCREEN_MARGIN));
        int idealTrackW = span * PX_PER_POINT;
        boolean counterMode = idealTrackW > maxTrackW;
        int trackW = counterMode ? maxTrackW : Mth.clamp(idealTrackW, TRACK_MIN_W, maxTrackW);
        int panelW = trackW + (STRIPE_W + PAD) * 2;

        boolean showAbandon = info.attackerAbandonSeconds > 0
                && ClientClaimChunkCache.playerFactionId().equals(info.attackingFactionId);

        ScreenPos pos = ClientConfig.POS_SIEGE;
        int px = ScreenSpaceUtil.getX(pos, panelW);
        int py = ScreenSpaceUtil.getY(pos, PANEL_H + (showAbandon ? ABANDON_H : 0));

        int attackRGB = info.attackingColour & 0xFFFFFF;
        int defendRGB = info.defendingColour & 0xFFFFFF;

        int trackLeft = px + STRIPE_W + PAD;
        int trackRight = px + panelW - STRIPE_W - PAD;
        int trackTop = py + TRACK_Y;
        int trackWpx = trackRight - trackLeft;
        float slotW = (float) trackWpx / span;

        int prog = Mth.clamp(info.progress, -defendPts, attackPts);
        float centerX = pointX(trackLeft, slotW, defendPts, 0);
        float markerX = pointX(trackLeft, slotW, defendPts, prog);

        graphics.enableBlend();

        graphics.fill(px, py, px + panelW, py + PANEL_H, SECTION_FILL);
        graphics.border(px, py, px + panelW, py + PANEL_H, 1, SECTION_BORDER);
        graphics.fill(px, py, px + STRIPE_W, py + PANEL_H, ColorUtil.argb(0xFF, defendRGB));
        graphics.fill(px + panelW - STRIPE_W, py, px + panelW, py + PANEL_H, ColorUtil.argb(0xFF, attackRGB));
        graphics.fill(px + STRIPE_W, py, px + STRIPE_W + 1, py + PANEL_H, SECTION_BORDER);
        graphics.fill(px + panelW - STRIPE_W - 1, py, px + panelW - STRIPE_W, py + PANEL_H, SECTION_BORDER);

        graphics.fill(trackLeft, trackTop, trackLeft + trackWpx, trackTop + TRACK_H, TRACK_FILL);
        graphics.border(trackLeft, trackTop, trackLeft + trackWpx, trackTop + TRACK_H, 1, SECTION_BORDER);

        drawFill(graphics, prog, attackRGB, defendRGB, centerX, markerX, trackTop);

        if (counterMode) {
            drawNotch(graphics, centerX, trackTop, true, CENTER_LINE);
            drawNotch(graphics, pointX(trackLeft, slotW, defendPts, attackPts), trackTop, false,
                    ColorUtil.argb(0xFF, ColorUtil.lighten(attackRGB, NOTCH_END_LIGHTEN)));
            drawNotch(graphics, pointX(trackLeft, slotW, defendPts, -defendPts), trackTop, false,
                    ColorUtil.argb(0xFF, ColorUtil.lighten(defendRGB, NOTCH_END_LIGHTEN)));
        } else {
            drawNotches(graphics, trackLeft, trackTop, slotW, defendPts, attackPts, attackRGB, defendRGB);
        }

        drawMarker(graphics, info, markerX, trackTop, attackRGB, defendRGB);

        if (counterMode) {
            drawCounter(graphics, font, prog, attackPts, defendPts, px, panelW, trackTop, attackRGB, defendRGB);
        }

        if (info != namedInfo) {
            namedInfo = info;
            defendNameWidth = font.width(info.defendingName);
            attackNameWidth = font.width(info.attackingName);
        }
        drawNames(graphics, font, info, px, panelW, trackLeft, trackRight, py, attackRGB, defendRGB);
        drawLabels(graphics, font, info, prog, attackPts, defendPts, px, panelW, trackLeft, trackRight, py, attackRGB, defendRGB);
        if (showAbandon) {
            drawAbandonTimer(graphics, font, info, px, panelW, py);
        }

        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private static void drawFill(Gui2D graphics, int prog, int attackRGB, int defendRGB, float centerX, float markerX, int trackTop) {
        if (prog == 0) {
            return;
        }
        boolean attackerSide = prog > 0;
        int baseRGB = attackerSide ? attackRGB : defendRGB;
        int x0 = Math.round(Math.min(centerX, markerX));
        int x1 = x0 + Math.round(Math.abs(markerX - centerX));
        if (x1 <= x0) {
            return;
        }
        int dark = ColorUtil.argb(0xFF, ColorUtil.darken(baseRGB, FILL_DARKEN));
        int light = ColorUtil.argb(0xFF, ColorUtil.lighten(baseRGB, FILL_LIGHTEN));
        graphics.fillGradientH(x0, trackTop + 1, x1, trackTop + TRACK_H - 1, attackerSide ? dark : light, attackerSide ? light : dark);
    }

    private static void drawNotches(Gui2D graphics, int trackLeft, int trackTop, float slotW, int defendPts, int attackPts, int attackRGB, int defendRGB) {
        for (int p = -defendPts; p <= attackPts; p++) {
            float nx = pointX(trackLeft, slotW, defendPts, p);
            boolean center = p == 0;
            int col;
            if (p == attackPts) {
                col = ColorUtil.argb(0xFF, ColorUtil.lighten(attackRGB, NOTCH_END_LIGHTEN));
            } else if (p == -defendPts) {
                col = ColorUtil.argb(0xFF, ColorUtil.lighten(defendRGB, NOTCH_END_LIGHTEN));
            } else if (center) {
                col = CENTER_LINE;
            } else {
                col = NOTCH_MINOR;
            }
            drawNotch(graphics, nx, trackTop, center, col);
        }
    }

    private static void drawNotch(Gui2D graphics, float nx, int trackTop, boolean center, int col) {
        float nh = center ? TRACK_H + CENTER_NOTCH_EXTRA : TRACK_H - MINOR_NOTCH_INSET;
        float nw = center ? 2f : 1f;
        float ny = trackTop + (TRACK_H - nh) / 2f;
        int ix = Math.round(nx - nw / 2f);
        int iy = Math.round(ny);
        graphics.fill(ix, iy, ix + Math.round(nw), iy + Math.round(nh), col);
    }

    private static void drawMarker(Gui2D graphics, SiegeCampProgressInfo info, float markerX, int trackTop, int attackRGB, int defendRGB) {
        float glowW = MARKER_W + GLOW_PAD;
        float top = trackTop - MARKER_OVERHANG;
        float height = TRACK_H + 2 * MARKER_OVERHANG;

        float pulse = 0.5f + 0.5f * (float) Math.sin(HudClock.now() / (double) PULSE_PERIOD_MS);
        int leadRGB;
        if (info.progress > info.previousProgress) {
            leadRGB = attackRGB;
        } else if (info.progress < info.previousProgress) {
            leadRGB = defendRGB;
        } else {
            leadRGB = 0xFFFFFF;
        }
        int glowAlpha = (int) (GLOW_ALPHA_BASE + pulse * GLOW_ALPHA_RANGE);

        int gix = Math.round(markerX - glowW / 2f);
        int giy = Math.round(top);
        graphics.fill(gix, giy, gix + Math.round(glowW), giy + Math.round(height), ColorUtil.argb(glowAlpha, leadRGB));

        int mix = Math.round(markerX - MARKER_W / 2f);
        graphics.fill(mix, giy, mix + Math.round(MARKER_W), giy + Math.round(height), MARKER_FILL);
    }

    private static void drawCounter(Gui2D graphics, Font font, int prog, int attackPts, int defendPts, int px, int panelW, int trackTop, int attackRGB, int defendRGB) {
        int color;
        if (prog > 0) {
            color = ColorUtil.opaque(ColorUtil.lighten(attackRGB, COUNTER_LIGHTEN));
        } else if (prog < 0) {
            color = ColorUtil.opaque(ColorUtil.lighten(defendRGB, COUNTER_LIGHTEN));
        } else {
            color = TEXT_NEUTRAL;
        }
        long key = label3Key(prog, attackPts, defendPts);
        if (STANDING.stale(key)) {
            String text = prog > 0 ? prog + " / " + attackPts : prog < 0 ? (-prog) + " / " + defendPts : I18n.get("warforge.client.siege.even");
            STANDING.set(key, text, font);
        }
        int tx = (int) (px + panelW / 2f - STANDING.width / 2f);
        int ty = (int) (trackTop + (TRACK_H - font.lineHeight) / 2f + 1f);
        drawOutlinedString(graphics, font, STANDING.text, tx, ty, color);
    }

    private static void drawNames(Gui2D graphics, Font font, SiegeCampProgressInfo info, int px, int panelW, int trackLeft, int trackRight, int py, int attackRGB, int defendRGB) {
        int iconY = py + NAMES_Y - 1;

        drawIcon(graphics, SHIELD_ICON, trackLeft, iconY, 10, 11);
        drawOutlinedString(graphics, font, info.defendingName, trackLeft + ICON_SIZE + 3, py + NAMES_Y, ColorUtil.opaque(defendRGB));

        drawIcon(graphics, AXE_ICON, trackRight - ICON_SIZE, iconY, 12, 12);
        drawOutlinedString(graphics, font, info.attackingName, trackRight - ICON_SIZE - 3 - attackNameWidth, py + NAMES_Y, ColorUtil.opaque(attackRGB));

        if (VERSUS.stale(0)) {
            VERSUS.set(0, I18n.get("warforge.client.siege.versus"), font);
        }
        graphics.text(font, VERSUS.text, (int) (px + panelW / 2f - VERSUS.width / 2f), py + NAMES_Y, TEXT_NEUTRAL, true);
    }

    private static void drawLabels(Gui2D graphics, Font font, SiegeCampProgressInfo info, int prog, int attackPts, int defendPts, int px, int panelW, int trackLeft, int trackRight, int py, int attackRGB, int defendRGB) {
        long winKey = (long) prog << 32 | attackPts;
        if (TO_WIN.stale(winKey)) {
            TO_WIN.set(winKey, prog < attackPts
                    ? I18n.get("warforge.client.siege.to_win", attackPts - prog)
                    : I18n.get("warforge.client.siege.station_to_win"), font);
        }
        long defendKey = prog + defendPts;
        if (TO_DEFEND.stale(defendKey)) {
            TO_DEFEND.set(defendKey, I18n.get("warforge.client.siege.to_defend", prog + defendPts), font);
        }

        drawOutlinedString(graphics, font, TO_DEFEND.text, trackLeft, py + LABELS_Y, ColorUtil.opaque(defendRGB));
        drawOutlinedString(graphics, font, TO_WIN.text, trackRight - TO_WIN.width, py + LABELS_Y, ColorUtil.opaque(attackRGB));

        if (info.finished) {
            // the result holds where the timer was for the finished panel's linger
            String result = prog >= attackPts ? I18n.get("warforge.client.siege.result.captured", info.attackingName)
                    : prog <= -defendPts ? I18n.get("warforge.client.siege.result.held", info.defendingName)
                    : I18n.get("warforge.client.siege.result.ended");
            int color = prog >= attackPts ? ColorUtil.opaque(ColorUtil.lighten(attackRGB, COUNTER_LIGHTEN))
                    : prog <= -defendPts ? ColorUtil.opaque(ColorUtil.lighten(defendRGB, COUNTER_LIGHTEN)) : TEXT_NEUTRAL;
            drawOutlinedString(graphics, font, result, (int) (px + panelW / 2f - font.width(result) / 2f), py + LABELS_Y, color);
        } else if (WarForgeConfig.SIEGE_ENABLE_NEW_TIMER && info.endTimestamp != Long.MAX_VALUE) { // MAX = no end time sent
            long remain = info.endTimestamp - HudClock.now();
            String timer = END_TIMER.get(remain);
            int width = END_TIMER.width(font);
            graphics.text(font, timer, (int) (px + panelW / 2f - width / 2f), py + LABELS_Y,
                    remain < TIMER_DANGER_MS ? TEXT_DANGER : TEXT_NEUTRAL, true);
        }
    }

    private static void drawAbandonTimer(Gui2D graphics, Font font, SiegeCampProgressInfo info, int px, int panelW, int py) {
        String text = ABANDON_TIMER.get(info.attackerAbandonSeconds * 1000L);
        int width = ABANDON_TIMER.width(font);
        int color = info.attackerAbandonSeconds <= ABANDON_DANGER_SECONDS ? TEXT_DANGER : TEXT_ABANDON;
        graphics.text(font, text, (int) (px + panelW / 2f - width / 2f), py + PANEL_H + ABANDON_GAP, color, true);
    }

    private static void drawIcon(Gui2D graphics, ResourceLocation icon, int x, int y, int texW, int texH) {
        graphics.enableBlend();
        graphics.setColor(1f, 1f, 1f, 1f);
        graphics.blit(icon, x, y, ICON_SIZE, ICON_SIZE, 0f, 0f, texW, texH, texW, texH);
    }

    private static void drawOutlinedString(Gui2D graphics, Font font, String text, int x, int y, int color) {
        int outline = ColorUtil.isBright(color) ? 0xFF000000 : 0xFFFFFFFF;
        graphics.text(font, text, x - 1, y, outline, false);
        graphics.text(font, text, x + 1, y, outline, false);
        graphics.text(font, text, x, y - 1, outline, false);
        graphics.text(font, text, x, y + 1, outline, false);
        graphics.text(font, text, x, y, color, false);
    }

    private static float pointX(int trackLeft, float slotW, int defendPts, int point) {
        return trackLeft + (point + defendPts) * slotW;
    }

    private static long label3Key(int a, int b, int c) {
        return ((long) a * 31 + b) * 31 + c;
    }

    private static final class Label {
        private long key;
        private boolean valid;
        private String text = "";
        private int width;

        boolean stale(long newKey) {
            return !valid || key != newKey;
        }

        void set(long newKey, String newText, Font font) {
            key = newKey;
            valid = true;
            text = newText;
            width = font.width(newText);
        }
    }
}
