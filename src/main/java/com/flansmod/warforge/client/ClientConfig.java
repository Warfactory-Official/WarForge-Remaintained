package com.flansmod.warforge.client;

import com.flansmod.warforge.client.util.ScreenSpaceUtil.ScreenPos;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player rendering and HUD preferences; the CLIENT config file, never synced from a server. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static boolean SHOW_OPPONENT_BORDERS = true;
    public static boolean SHOW_ALLY_BORDERS = true;
    public static float HUD_VERT_CUTOFF_PERCENT = 0.40f;
    public static float SHOW_NEW_AREA_TIMER = 200.0f;
    public static boolean SHOW_YIELD_TIMERS = true;
    public static boolean SHOW_HUD_IN_REPLAYS = false;
    public static boolean SHOW_VEIN_HUD_IN_REPLAYS = false;
    public static long VEIN_MEMBER_DISPLAY_TIME_MS = 1000;
    public static int BORDER_RENDER_DISTANCE = 0;
    public static ScreenPos POS_TIMERS = ScreenPos.BOTTOM_RIGHT;
    public static ScreenPos POS_VEIN_INDICATOR = ScreenPos.TOP;
    public static ScreenPos POS_TOAST_INDICATOR = ScreenPos.TOP;
    public static ScreenPos POS_SIEGE = ScreenPos.TOP;

    private static final ModConfigSpec.BooleanValue SHOW_OPPONENT_BORDERS_V;
    private static final ModConfigSpec.BooleanValue SHOW_ALLY_BORDERS_V;
    private static final ModConfigSpec.DoubleValue SHOW_NEW_AREA_TIMER_V;
    private static final ModConfigSpec.BooleanValue SHOW_YIELD_TIMERS_V;
    private static final ModConfigSpec.BooleanValue SHOW_HUD_IN_REPLAYS_V;
    private static final ModConfigSpec.BooleanValue SHOW_VEIN_HUD_IN_REPLAYS_V;
    private static final ModConfigSpec.IntValue VEIN_MEMBER_DISPLAY_TIME_MS_V;
    private static final ModConfigSpec.DoubleValue HUD_VERT_CUTOFF_PERCENT_V;
    private static final ModConfigSpec.ConfigValue<String> POS_TIMERS_V;
    private static final ModConfigSpec.ConfigValue<String> POS_SIEGE_V;
    private static final ModConfigSpec.ConfigValue<String> POS_TOAST_INDICATOR_V;
    private static final ModConfigSpec.ConfigValue<String> POS_VEIN_INDICATOR_V;
    private static final ModConfigSpec.IntValue BORDER_RENDER_DISTANCE_V;

    static {
        ModConfigSpec.Builder cfg = new ModConfigSpec.Builder();
        SHOW_OPPONENT_BORDERS_V = cfg.comment("Turns the in-world border rendering on/off for opponent chunks.").define("Show Opponent Chunk Borders", SHOW_OPPONENT_BORDERS);
        SHOW_ALLY_BORDERS_V = cfg.comment("Turns the in-world border rendering on/off for ally chunks.").define("Show Ally Chunk Borders", SHOW_ALLY_BORDERS);
        SHOW_NEW_AREA_TIMER_V = cfg.comment("How many in-game ticks to show the 'You have entered {faction}' message for.").defineInRange("New Area Timer", (double) SHOW_NEW_AREA_TIMER, 0.0d, 1000d);
        SHOW_YIELD_TIMERS_V = cfg.comment("Whether to show a readout of the time until the next yield / siege in top left of your screen").define("Show yield timers", SHOW_YIELD_TIMERS);
        SHOW_HUD_IN_REPLAYS_V = cfg.comment("Draw the WarForge HUD (siege status, timers, toasts) in Flashback replays and exports, for footage capture. Timers follow replay time.").define("Show HUD in replays", SHOW_HUD_IN_REPLAYS);
        SHOW_VEIN_HUD_IN_REPLAYS_V = cfg.comment("Also draw the vein indicator for the replay camera's chunk (from the vein answers recorded with the replay). Footage capture only.").define("Show vein HUD in replays", SHOW_VEIN_HUD_IN_REPLAYS);
        VEIN_MEMBER_DISPLAY_TIME_MS_V = cfg.comment("The time in milliseconds for which each member of a vein will be displayed when it is being cycled through, to the precision allowed by the client tick system.").defineInRange("Vein Member Display Time", (int) VEIN_MEMBER_DISPLAY_TIME_MS, 100, Integer.MAX_VALUE);
        HUD_VERT_CUTOFF_PERCENT_V = cfg.comment("What percent of the entire screen resolution from the top must certain displays (such as vein info) be before they stop rendering. Set to 0.0 to disable all relevant displays, or 1.0 to turn this off.").defineInRange("HUD Vertical cutoff", (double) HUD_VERT_CUTOFF_PERCENT, 0.0d, 1.0d);
        POS_TIMERS_V = cfg.comment("Position of the yield timers").define("Yield timer position", "BOTTOM_RIGHT");
        POS_SIEGE_V = cfg.comment("Position of the siege status").define("Siege status position", "TOP");
        POS_TOAST_INDICATOR_V = cfg.comment("Position of the toast indicator").define("Toast indicator position", "TOP");
        POS_VEIN_INDICATOR_V = cfg.comment("Position of the chunk vein indicator").define("Chunk vein indicator position", "TOP");
        BORDER_RENDER_DISTANCE_V = cfg.comment("Maximum distance in chunks from the camera at which claim borders are rendered. 0 (default) follows the client's render distance so borders never extend into unloaded terrain; a positive value overrides it.").defineInRange("Border Render Distance", BORDER_RENDER_DISTANCE, 0, 256);
        SPEC = cfg.build();
    }

    private ClientConfig() {}

    public static void bake() {
        SHOW_OPPONENT_BORDERS = SHOW_OPPONENT_BORDERS_V.get();
        SHOW_ALLY_BORDERS = SHOW_ALLY_BORDERS_V.get();
        SHOW_NEW_AREA_TIMER = SHOW_NEW_AREA_TIMER_V.get().floatValue();
        SHOW_YIELD_TIMERS = SHOW_YIELD_TIMERS_V.get();
        SHOW_HUD_IN_REPLAYS = SHOW_HUD_IN_REPLAYS_V.get();
        SHOW_VEIN_HUD_IN_REPLAYS = SHOW_VEIN_HUD_IN_REPLAYS_V.get();
        VEIN_MEMBER_DISPLAY_TIME_MS = VEIN_MEMBER_DISPLAY_TIME_MS_V.get();
        HUD_VERT_CUTOFF_PERCENT = HUD_VERT_CUTOFF_PERCENT_V.get().floatValue();
        POS_TIMERS = ScreenPos.fromString(POS_TIMERS_V.get());
        POS_SIEGE = ScreenPos.fromString(POS_SIEGE_V.get());
        POS_TOAST_INDICATOR = ScreenPos.fromString(POS_TOAST_INDICATOR_V.get());
        POS_VEIN_INDICATOR = ScreenPos.fromString(POS_VEIN_INDICATOR_V.get());
        BORDER_RENDER_DISTANCE = BORDER_RENDER_DISTANCE_V.get();
    }
}
