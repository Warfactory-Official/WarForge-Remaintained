package com.flansmod.warforge.common;

import com.flansmod.warforge.compat.Regs;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.vein.Quality;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import com.flansmod.warforge.platform.Platform;
import com.flansmod.warforge.server.fob.FobWarpQueue;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;

import static com.flansmod.warforge.common.WarForgeMod.VEIN_HANDLER;
import com.flansmod.warforge.compat.Ids;

public class WarForgeConfig {
    public static final String CATEGORY_CLAIMS = "Claims";

    public static final String CATEGORY_YIELDS = "Yields";
    public static final String CATEGORY_SIEGES = "Sieges";

    public static final String CATEGORY_NOTORIETY = "Notoriety";
    public static final String CATEGORY_LEGACY = "Legacy";
    public static final String CATEGORY_WARPS = "Warps";
    public static final String CATEGORY_DISPLAY = "Display";
    public static final String CATEGORY_ALLIANCES = "Alliances";
    public static final String CATEGORY_GENERAL = "General";

    public static final ModConfigSpec SPEC;

    public static int YIELD_DAY_LENGTH = 3600; // s
    public static boolean TICK_YIELDS = false;
    public static boolean TICK_SIEGES = false;
    public static boolean TICK_MOMENTUM = false;
    public static boolean TICK_OFFLINE_PROTECTION = false;
    public static boolean TICK_SIEGE_GRACE = false;
    public static boolean TICK_CITADEL_MOVE = false;
    public static boolean TICK_TRUCES = false;
    public static boolean TICK_FACTION_REJOIN = false;
    public static float POOR_QUAL_MULT = 0.5f;
    public static float FAIR_QUAL_MULT = 1f;
    public static float RICH_QUAL_MULT = 2f;

    public static boolean ENABLE_CITADEL_UPGRADES = true;
    public static String[] CLAIM_DIM_WHITELIST = new String[]{"minecraft:overworld"};
    public static int CLAIM_STRENGTH_CITADEL = 15;
    public static int CLAIM_STRENGTH_REINFORCED = 10;
    public static int CLAIM_STRENGTH_BASIC = 5;
    public static int SUPPORT_STRENGTH_CITADEL = 3;

    public static int SUPPORT_STRENGTH_REINFORCED = 2;
    public static int SUPPORT_STRENGTH_BASIC = 1;
    public static int FORCE_LOADED_CHUNKS_TOTAL = 8;
    public static int MAX_CLAIMS_PER_FACTION = -1;
    public static int MAX_FOBS = 3;
    public static int FOB_TICKET_LIMIT = 5;
    public static int FOB_TICKET_REGEN_PER_SIEGE_TICK = 1;
    public static int FOB_WARP_TICKS = 200;
    public static boolean FOB_BLOCK_BREAKABLE = false;
    public static int FOB_CLAIM_EXCLUSION_RADIUS = 1;
    public static int MIN_DISTANCE_BETWEEN_FACTIONS = 1;
    public static int CLAIM_MANAGER_RADIUS = 4;
    public static boolean CLAIM_MAP_WHOLE_WORLD = true;
    public static int ISLAND_COLLECTOR_SLOTS = 100;
    public static boolean ENABLE_OFFLINE_RAID_PROTECTION = true;
    public static int OFFLINE_RAID_PROTECTION_HOURS = 24;
    public static boolean ENABLE_SIEGE_GRACE_PERIOD = true;
    public static int SIEGE_GRACE_PERIOD_HOURS = 24;
    public static int FACTION_REJOIN_COOLDOWN_MINUTES = 10;
    public static int ATTACK_STRENGTH_SIEGE_CAMP = 1;
    public static boolean ENABLE_ISOLATED_CLAIMS = true;
    public static String[] INSURANCE_BLACKLIST_IDS = new String[]{"minecraft:*shulker_box", "appliedenergistics2:*cell*"};
    public static String[] DEFAULT_FLAG_IDS = new String[]{
            "white", "light_gray", "gray", "black", "red", "orange", "yellow", "lime",
            "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink", "brown"
    };
    public static String[] CUSTOM_FLAG_ALLOWLIST = new String[]{"*"};
    public static String[] FLAG_WHITELIST = new String[]{};
    public static boolean UNIQUE_FLAGS = true;

    public static boolean SIEGE_ENABLE_NEW_TIMER = true;
    public static int SIEGE_DEFENCE_THRESHOLD = 5;
    public static boolean SIEGE_END_ON_GOAL_REACHED = true;
    public static int SIEGE_STALL_ESCALATION_THRESHOLD = 10;
    public static int SIEGE_MOMENTUM_DURATION = 60; // min
    public static int SIEGE_SWING_PER_DEFENDER_DEATH = 1;
    public static int SIEGE_SWING_PER_ATTACKER_DEATH = 1;
    public static int SIEGE_SWING_PER_DAY_ELAPSED_BASE = 1;
    public static int SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS = 1;
    public static int SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS = 1;
    public static float SIEGE_DAY_LENGTH = 24.0f; // h
    public static float SIEGE_INFO_RADIUS = 200f;
    public static int SIEGE_DIFF_PER_MEMBER = 1;
    public static boolean SIEGE_CAPTURE = false;
    public static int SIEGE_COOLDOWN_FAIL = 30; // min
    public static int MAX_SIEGES = 3;
    public static int MAX_INCOMING_SIEGES = 3;
    public static int ATTACKER_DESERTION_TIMER = 180; // s
    public static int DEFENDER_DESERTION_TIMER = 300; // s
    public static int ATTACKER_CONQUERED_CHUNK_PERIOD = 3600000; // ms
    public static int DEFENDER_CONQUERED_CHUNK_PERIOD = 7200000; // ms
    public static int COMBAT_LOG_THRESHOLD = 10000; // ms
    public static int LIVE_QUIT_TIMER = 900000; // ms
    public static int MAX_OFFLINE_PLAYER_COUNT_MINIMUM = 0; // only when negative: players online before dropping to 0 counts as live quit
    public static float MAX_OFFLINE_PLAYER_PERCENT = 0.5f; // fraction of members online at some point before live-quit penalties apply
    public static int VERTICAL_SIEGE_DIST = 40; // blocks, inclusive
    public static int SIEGE_BATTLE_RADIUS = 2; // chunks
    public static int SIEGE_SIEGED_RADIUS = 1; // chunks
    public static SiegeKillDetectionMode SIEGE_KILL_DETECTION_MODE = SiegeKillDetectionMode.PRECISE;

    public enum SiegeKillDetectionMode {
        PRECISE,
        SIMPLE
    }
    public static int SIEGE_ATTACKER_RADIUS = 1; // chunks
    public static int SIEGE_DEFENDER_RADIUS = 15;

    public static boolean SIEGE_ALLOW_UI_DECLARE = true;
    public static int SIEGE_DECLARE_MAX_RANGE = 4; // chebyshev chunks from start-from chunk
    public static boolean SIEGE_DECLARE_REQUIRE_PRESENCE = true;

    public static int ALLIANCE_TRUCE_DURATION_MINUTES = 60; // min; 0 disables
    public static int MAX_ALLIES = 2; // -1 unlimited
    public static int NOTORIETY_PER_PLAYER_KILL = 1;
    public static int NOTORIETY_KILL_CAP_PER_PLAYER = 3;
    public static int NOTORIETY_PER_SIEGE_ATTACK_SUCCESS = 7;
    public static int NOTORIETY_PER_SIEGE_DEFEND_SUCCESS = 10;
    public static int LEGACY_PER_DAY = 3;
    public static boolean LEGACY_USES_YIELD_TIMER = true;

    public static int INVITE_DECAY_TIME = 5;
    /** Hard cap (chunks) on the sparse border-outline sync radius the server serves. */
    public static final int BORDER_SYNC_MAX_RADIUS = 48;
    public static int FACTION_NAME_LENGTH_MAX = 32;
    public static String[] FACTION_NAME_BANLIST = new String[]{"admin", "mod", "staff"};
    public static int CITADEL_MOVE_COOLDOWN_SECONDS = 25200; // s
    public static int CITADEL_MIN_Y = 63;

    public static boolean FACTION_PREFIX_IN_CHAT = true;
    public static boolean FACTION_PREFIX_IN_TABLIST = true;

    public static final int JM_MODE_DISABLED = 0;
    public static final int JM_MODE_AUTO = 1;
    public static final int JM_MODE_PLAYER = 2;
    public static int JOURNEYMAP_VEIN_MODE = JM_MODE_DISABLED;
    public static int JOURNEYMAP_VEIN_AUTO_RADIUS = 24;

    public static int parseJourneyMapMode(String value) {
        if (value == null) return JM_MODE_DISABLED;
        switch (value.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "AUTO": return JM_MODE_AUTO;
            case "PLAYER": return JM_MODE_PLAYER;
            default: return JM_MODE_DISABLED;
        }
    }

    public static boolean ENABLE_F_HOME_COMMAND = true;
    public static boolean ALLOW_F_HOME_BETWEEN_DIMENSIONS = false;
    public static int NUM_TICKS_FOR_WARP_COMMANDS = 20 * 20;
    public static boolean ENABLE_TPA_COMMAND = true;
    public static int TPA_REQUEST_TIMEOUT_SECONDS = 60;
    public static boolean ENABLE_SPAWN_COMMAND = true;
    public static boolean ALLOW_SPAWN_BETWEEN_DIMENSIONS = false;
    public static boolean SPAWN_AT_CITADEL = false;
    public static boolean ALLOW_BED_SPAWN_IN_CLAIMS = false;

    public static ProtectionConfig UNCLAIMED = new ProtectionConfig();
    public static ProtectionConfig SAFE_ZONE = new ProtectionConfig();
    public static ProtectionConfig WAR_ZONE = new ProtectionConfig();
    public static ProtectionConfig CITADEL_FRIEND = new ProtectionConfig();
    public static ProtectionConfig CITADEL_FOE = new ProtectionConfig();
    public static ProtectionConfig CLAIM_FRIEND = new ProtectionConfig();
    public static ProtectionConfig CLAIM_ALLY = new ProtectionConfig();
    public static ProtectionConfig CLAIM_FOE = new ProtectionConfig();
    public static ProtectionConfig CLAIM_DEFENDED = new ProtectionConfig();
    // SIEGED/WAR: Friend = defending faction member
    public static ProtectionConfig SIEGED_FRIEND = new ProtectionConfig();
    public static ProtectionConfig SIEGED_FOE = new ProtectionConfig();
    public static ProtectionConfig WAR_FRIEND = new ProtectionConfig();
    public static ProtectionConfig WAR_FOE = new ProtectionConfig();

    static {
        SAFE_ZONE.BREAK_BLOCKS = false;
        SAFE_ZONE.PLACE_BLOCKS = false;
        SAFE_ZONE.INTERACT = false;
        SAFE_ZONE.USE_ITEM = false;
        SAFE_ZONE.PLAYER_TAKE_DAMAGE_FROM_MOB = false;
        SAFE_ZONE.PLAYER_TAKE_DAMAGE_FROM_PLAYER = false;
        SAFE_ZONE.PLAYER_TAKE_DAMAGE_FROM_OTHER = false;
        SAFE_ZONE.PLAYER_DEAL_DAMAGE = false;
        SAFE_ZONE.BLOCK_REMOVAL = false;
        SAFE_ZONE.EXPLOSION_DAMAGE = false;
        SAFE_ZONE.BLOCK_BREAK_WHITELIST_IDS = new String[]{};
        SAFE_ZONE.BLOCK_PLACE_WHITELIST_IDS = new String[]{};
        SAFE_ZONE.BLOCK_INTERACT_WHITELIST_IDS = new String[]{"minecraft:ender_chest", "minecraft:lever", "#minecraft:buttons", "warforge:leaderboard"};
        SAFE_ZONE.ALLOW_MOB_SPAWNS = false;
        SAFE_ZONE.ALLOW_MOB_ENTRY = false;
        SAFE_ZONE.ALLOW_MOUNT_ENTITY = false;
        SAFE_ZONE.ALLOW_DISMOUNT_ENTITY = false;
        SAFE_ZONE.ALLOW_FOREIGN_FLUID_INFLOW = false;
        SAFE_ZONE.ALLOW_FOREIGN_PISTON_PUSH = false;

        WAR_ZONE.BREAK_BLOCKS = false;
        WAR_ZONE.PLACE_BLOCKS = false;
        WAR_ZONE.INTERACT = true;
        WAR_ZONE.USE_ITEM = true;
        WAR_ZONE.ALLOW_FOREIGN_FLUID_INFLOW = false;
        WAR_ZONE.ALLOW_FOREIGN_PISTON_PUSH = false;
        WAR_ZONE.PLAYER_TAKE_DAMAGE_FROM_MOB = true;
        WAR_ZONE.PLAYER_TAKE_DAMAGE_FROM_PLAYER = true;
        WAR_ZONE.PLAYER_TAKE_DAMAGE_FROM_OTHER = true;
        WAR_ZONE.PLAYER_DEAL_DAMAGE = true;
        WAR_ZONE.BLOCK_REMOVAL = false;
        WAR_ZONE.EXPLOSION_DAMAGE = false;
        WAR_ZONE.BLOCK_BREAK_WHITELIST_IDS = new String[]{"minecraft:cobweb", "minecraft:tnt", "minecraft:end_crystal"};
        WAR_ZONE.BLOCK_PLACE_WHITELIST_IDS = new String[]{"minecraft:cobweb", "minecraft:tnt", "minecraft:end_crystal"};
        WAR_ZONE.BLOCK_INTERACT_WHITELIST_IDS = new String[]{"minecraft:ender_chest", "minecraft:lever", "#minecraft:buttons", "warforge:leaderboard"};

        CITADEL_FOE.BREAK_BLOCKS = false;
        CITADEL_FOE.PLACE_BLOCKS = false;
        CITADEL_FOE.INTERACT = false;
        CITADEL_FOE.USE_ITEM = false;
        CLAIM_FOE.BREAK_BLOCKS = false;
        CLAIM_FOE.PLACE_BLOCKS = false;
        CLAIM_FOE.INTERACT = false;
        CLAIM_FOE.USE_ITEM = false;
        CITADEL_FOE.ALLOW_MOUNT_ENTITY = false;
        CITADEL_FOE.ALLOW_DISMOUNT_ENTITY = false;
        CITADEL_FOE.ALLOW_FOREIGN_FLUID_INFLOW = false;
        CITADEL_FOE.ALLOW_FOREIGN_PISTON_PUSH = false;
        CLAIM_FOE.ALLOW_MOUNT_ENTITY = false;
        CLAIM_FOE.ALLOW_DISMOUNT_ENTITY = false;
        CLAIM_FOE.ALLOW_FOREIGN_FLUID_INFLOW = false;
        CLAIM_FOE.ALLOW_FOREIGN_PISTON_PUSH = false;

        CLAIM_ALLY.BREAK_BLOCKS = false;
        CLAIM_ALLY.PLACE_BLOCKS = false;

        UNCLAIMED.EXPLOSION_DAMAGE = true;

        CLAIM_DEFENDED.BREAK_BLOCKS = true;

        SIEGED_FRIEND.BREAK_BLOCKS = true;
        SIEGED_FRIEND.PLACE_BLOCKS = true;
        SIEGED_FRIEND.INTERACT = true;
        SIEGED_FRIEND.USE_ITEM = true;
        SIEGED_FRIEND.BLOCK_REMOVAL = true;
        SIEGED_FRIEND.EXPLOSION_DAMAGE = true;
        SIEGED_FOE.BREAK_BLOCKS = true;
        SIEGED_FOE.PLACE_BLOCKS = true;
        SIEGED_FOE.INTERACT = true;
        SIEGED_FOE.USE_ITEM = true;
        SIEGED_FOE.BLOCK_REMOVAL = true;
        SIEGED_FOE.EXPLOSION_DAMAGE = true;

        WAR_FRIEND.BREAK_BLOCKS = true;
        WAR_FRIEND.PLACE_BLOCKS = true;
        WAR_FRIEND.INTERACT = true;
        WAR_FRIEND.USE_ITEM = true;
        WAR_FRIEND.BLOCK_REMOVAL = true;
        WAR_FOE.BREAK_BLOCKS = false;
        WAR_FOE.PLACE_BLOCKS = false;
        WAR_FOE.INTERACT = false;
        WAR_FOE.USE_ITEM = true;
        WAR_FOE.ALLOW_FOREIGN_FLUID_INFLOW = false;
        WAR_FOE.ALLOW_FOREIGN_PISTON_PUSH = false;
        WAR_FOE.EXPLOSION_DAMAGE = false;

        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        define(builder);
        SPEC = builder.build();
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> CLAIM_DIM_WHITELIST_V;
    private static ModConfigSpec.IntValue CLAIM_STRENGTH_CITADEL_V;
    private static ModConfigSpec.IntValue CLAIM_STRENGTH_REINFORCED_V;
    private static ModConfigSpec.IntValue CLAIM_STRENGTH_BASIC_V;
    private static ModConfigSpec.IntValue SUPPORT_STRENGTH_CITADEL_V;
    private static ModConfigSpec.IntValue SUPPORT_STRENGTH_REINFORCED_V;
    private static ModConfigSpec.IntValue SUPPORT_STRENGTH_BASIC_V;
    private static ModConfigSpec.IntValue FORCE_LOADED_CHUNKS_TOTAL_V;
    private static ModConfigSpec.IntValue MAX_CLAIMS_PER_FACTION_V;
    private static ModConfigSpec.IntValue MAX_FOBS_V;
    private static ModConfigSpec.IntValue FOB_TICKET_LIMIT_V;
    private static ModConfigSpec.IntValue FOB_TICKET_REGEN_PER_SIEGE_TICK_V;
    private static ModConfigSpec.IntValue FOB_WARP_TICKS_V;
    private static ModConfigSpec.BooleanValue FOB_BLOCK_BREAKABLE_V;
    private static ModConfigSpec.IntValue FOB_CLAIM_EXCLUSION_RADIUS_V;
    private static ModConfigSpec.ConfigValue<List<? extends String>> FOB_VEHICLE_TICKET_COST_V;
    private static ModConfigSpec.IntValue MIN_DISTANCE_BETWEEN_FACTIONS_V;
    private static ModConfigSpec.IntValue CLAIM_MANAGER_RADIUS_V;
    private static ModConfigSpec.BooleanValue CLAIM_MAP_WHOLE_WORLD_V;
    private static ModConfigSpec.IntValue ISLAND_COLLECTOR_SLOTS_V;
    private static ModConfigSpec.BooleanValue ENABLE_OFFLINE_RAID_PROTECTION_V;
    private static ModConfigSpec.IntValue OFFLINE_RAID_PROTECTION_HOURS_V;
    private static ModConfigSpec.BooleanValue ENABLE_SIEGE_GRACE_PERIOD_V;
    private static ModConfigSpec.IntValue SIEGE_GRACE_PERIOD_HOURS_V;
    private static ModConfigSpec.IntValue FACTION_REJOIN_COOLDOWN_MINUTES_V;
    private static ModConfigSpec.IntValue CITADEL_MOVE_COOLDOWN_SECONDS_V;
    private static ModConfigSpec.IntValue CITADEL_MIN_Y_V;
    private static ModConfigSpec.BooleanValue ENABLE_CITADEL_UPGRADES_V;
    private static ModConfigSpec.BooleanValue ENABLE_ISOLATED_CLAIMS_V;
    private static ModConfigSpec.ConfigValue<List<? extends String>> INSURANCE_BLACKLIST_IDS_V;
    private static ModConfigSpec.ConfigValue<List<? extends String>> DEFAULT_FLAG_IDS_V;
    private static ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_FLAG_ALLOWLIST_V;
    private static ModConfigSpec.ConfigValue<List<? extends String>> FLAG_WHITELIST_V;
    private static ModConfigSpec.BooleanValue UNIQUE_FLAGS_V;

    private static ModConfigSpec.IntValue ATTACK_STRENGTH_SIEGE_CAMP_V;
    private static ModConfigSpec.IntValue MAX_SIEGES_V;
    private static ModConfigSpec.IntValue MAX_INCOMING_SIEGES_V;
    private static ModConfigSpec.IntValue ATTACKER_DESERTION_TIMER_V;
    private static ModConfigSpec.IntValue DEFENDER_DESERTION_TIMER_V;
    private static ModConfigSpec.IntValue ATTACKER_CONQUERED_CHUNK_PERIOD_V;
    private static ModConfigSpec.IntValue DEFENDER_CONQUERED_CHUNK_PERIOD_V;
    private static ModConfigSpec.IntValue COMBAT_LOG_THRESHOLD_V;
    private static ModConfigSpec.IntValue LIVE_QUIT_TIMER_V;
    private static ModConfigSpec.BooleanValue SIEGE_ALLOW_UI_DECLARE_V;
    private static ModConfigSpec.IntValue SIEGE_DECLARE_MAX_RANGE_V;
    private static ModConfigSpec.BooleanValue SIEGE_DECLARE_REQUIRE_PRESENCE_V;
    private static ModConfigSpec.IntValue MAX_OFFLINE_PLAYER_COUNT_MINIMUM_V;
    private static ModConfigSpec.DoubleValue MAX_OFFLINE_PLAYER_PERCENT_V;
    private static ModConfigSpec.IntValue VERTICAL_SIEGE_DIST_V;
    private static ModConfigSpec.IntValue SIEGE_BATTLE_RADIUS_V;
    private static ModConfigSpec.IntValue SIEGE_SIEGED_RADIUS_V;
    private static ModConfigSpec.EnumValue<SiegeKillDetectionMode> SIEGE_KILL_DETECTION_MODE_V;
    private static ModConfigSpec.IntValue SIEGE_ATTACKER_RADIUS_V;
    private static ModConfigSpec.IntValue SIEGE_DEFENDER_RADIUS_V;
    private static ModConfigSpec.IntValue SIEGE_SWING_PER_DEFENDER_DEATH_V;
    private static ModConfigSpec.IntValue SIEGE_SWING_PER_ATTACKER_DEATH_V;
    private static ModConfigSpec.IntValue SIEGE_STALL_ESCALATION_THRESHOLD_V;
    private static ModConfigSpec.IntValue SIEGE_SWING_PER_DAY_ELAPSED_BASE_V;
    private static ModConfigSpec.IntValue SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS_V;
    private static ModConfigSpec.IntValue SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS_V;
    private static ModConfigSpec.DoubleValue SIEGE_DAY_LENGTH_V;
    private static ModConfigSpec.IntValue SIEGE_DEFENCE_THRESHOLD_V;
    private static ModConfigSpec.BooleanValue SIEGE_END_ON_GOAL_REACHED_V;
    private static ModConfigSpec.DoubleValue SIEGE_INFO_RADIUS_V;
    private static ModConfigSpec.IntValue SIEGE_COOLDOWN_FAIL_V;
    private static ModConfigSpec.IntValue SIEGE_DIFF_PER_MEMBER_V;
    private static ModConfigSpec.BooleanValue SIEGE_CAPTURE_V;
    private static ModConfigSpec.BooleanValue SIEGE_ENABLE_NEW_TIMER_V;
    private static ModConfigSpec.IntValue SIEGE_MOMENTUM_DURATION_V;
    private static ModConfigSpec.ConfigValue<List<? extends String>> SIEGE_MOMENTUM_MULTIPLIERS_V;

    private static ModConfigSpec.IntValue ALLIANCE_TRUCE_DURATION_MINUTES_V;
    private static ModConfigSpec.IntValue MAX_ALLIES_V;

    private static ModConfigSpec.IntValue YIELD_DAY_LENGTH_V;
    private static ModConfigSpec.BooleanValue TICK_YIELDS_V;
    private static ModConfigSpec.BooleanValue TICK_SIEGES_V;
    private static ModConfigSpec.BooleanValue TICK_MOMENTUM_V;
    private static ModConfigSpec.BooleanValue TICK_OFFLINE_PROTECTION_V;
    private static ModConfigSpec.BooleanValue TICK_SIEGE_GRACE_V;
    private static ModConfigSpec.BooleanValue TICK_CITADEL_MOVE_V;
    private static ModConfigSpec.BooleanValue TICK_TRUCES_V;
    private static ModConfigSpec.BooleanValue TICK_FACTION_REJOIN_V;
    private static ModConfigSpec.DoubleValue POOR_QUAL_MULT_V;
    private static ModConfigSpec.DoubleValue FAIR_QUAL_MULT_V;
    private static ModConfigSpec.DoubleValue RICH_QUAL_MULT_V;

    private static ModConfigSpec.IntValue NOTORIETY_PER_PLAYER_KILL_V;
    private static ModConfigSpec.IntValue NOTORIETY_PER_SIEGE_ATTACK_SUCCESS_V;
    private static ModConfigSpec.IntValue NOTORIETY_PER_SIEGE_DEFEND_SUCCESS_V;
    private static ModConfigSpec.IntValue NOTORIETY_KILL_CAP_PER_PLAYER_V;

    private static ModConfigSpec.IntValue LEGACY_PER_DAY_V;
    private static ModConfigSpec.BooleanValue LEGACY_USES_YIELD_TIMER_V;

    private static ModConfigSpec.IntValue FACTION_NAME_LENGTH_MAX_V;
    private static ModConfigSpec.ConfigValue<List<? extends String>> FACTION_NAME_BANLIST_V;
    private static ModConfigSpec.BooleanValue FACTION_PREFIX_IN_CHAT_V;
    private static ModConfigSpec.BooleanValue FACTION_PREFIX_IN_TABLIST_V;
    private static ModConfigSpec.ConfigValue<String> JOURNEYMAP_VEIN_MODE_V;
    private static ModConfigSpec.IntValue JOURNEYMAP_VEIN_AUTO_RADIUS_V;

    private static ModConfigSpec.BooleanValue ENABLE_F_HOME_COMMAND_V;
    private static ModConfigSpec.BooleanValue ALLOW_F_HOME_BETWEEN_DIMENSIONS_V;
    private static ModConfigSpec.BooleanValue ENABLE_SPAWN_COMMAND_V;
    private static ModConfigSpec.BooleanValue ALLOW_SPAWN_BETWEEN_DIMENSIONS_V;
    private static ModConfigSpec.BooleanValue SPAWN_AT_CITADEL_V;
    private static ModConfigSpec.BooleanValue ALLOW_BED_SPAWN_IN_CLAIMS_V;
    private static ModConfigSpec.IntValue NUM_TICKS_FOR_WARP_COMMANDS_V;
    private static ModConfigSpec.BooleanValue ENABLE_TPA_COMMAND_V;
    private static ModConfigSpec.IntValue TPA_REQUEST_TIMEOUT_SECONDS_V;

    private static List<String> asList(String[] arr) {
        return new ArrayList<>(Arrays.asList(arr));
    }

    private static void define(ModConfigSpec.Builder cfg) {
        UNCLAIMED.define(cfg, "Unclaimed", "Unclaimed Chunks");
        SAFE_ZONE.define(cfg, "SafeZone", "Safe Zone");
        WAR_ZONE.define(cfg, "WarZone", "War Zone");
        CITADEL_FRIEND.define(cfg, "CitadelFriend", "Citadels of their Faction");
        CITADEL_FOE.define(cfg, "CitadelFoe", "Citadels of other Factions");
        CLAIM_FRIEND.define(cfg, "ClaimFriend", "Claims of their Faction");
        CLAIM_ALLY.define(cfg, "ClaimAlly", "Claims of allied Factions (only applies when the owning faction enables ally interaction)");
        CLAIM_FOE.define(cfg, "ClaimFoe", "Claims of other Factions");
        CLAIM_DEFENDED.define(cfg, "ClaimDefended", "Claims of a faction under siege that are outside the War/Sieged zones");
        SIEGED_FRIEND.define(cfg, "SiegedFriend", "Inner sieged zone, member of the defending faction");
        SIEGED_FOE.define(cfg, "SiegedFoe", "Inner sieged zone, attacker or other faction (chunk protection disabled)");
        WAR_FRIEND.define(cfg, "WarFriend", "Outer war zone, member of the defending faction");
        WAR_FOE.define(cfg, "WarFoe", "Outer war zone, attacker or other faction (kills count, cannot break blocks)");

        cfg.push(CATEGORY_CLAIMS);
        CLAIM_DIM_WHITELIST_V = cfg.comment("In which dimensions should player be able to claim chunks")
                .defineList("Claim Dimension Whitelist", java.util.Arrays.asList(CLAIM_DIM_WHITELIST), o -> o instanceof String);
        CLAIM_STRENGTH_CITADEL_V = cfg.comment("The strength of citadel claims").defineInRange("Citadel Claim Strength", CLAIM_STRENGTH_CITADEL, 1, 1024);
        CLAIM_STRENGTH_REINFORCED_V = cfg.comment("The strength of reinforced claims").defineInRange("Reinforced Claim Strength", CLAIM_STRENGTH_REINFORCED, 1, 1024);
        CLAIM_STRENGTH_BASIC_V = cfg.comment("The strength of basic claims").defineInRange("Basic Claim Strength", CLAIM_STRENGTH_BASIC, 1, 1024);
        SUPPORT_STRENGTH_CITADEL_V = cfg.comment("The support strength a citadel gives to adjacent claims").defineInRange("Citadel Support Strength", SUPPORT_STRENGTH_CITADEL, 1, 1024);
        SUPPORT_STRENGTH_REINFORCED_V = cfg.comment("The support strength a reinforced claim gives to adjacent claims").defineInRange("Reinforced Support Strength", SUPPORT_STRENGTH_REINFORCED, 1, 1024);
        SUPPORT_STRENGTH_BASIC_V = cfg.comment("The support strength a basic claim gives to adjacent claims").defineInRange("Basic Support Strength", SUPPORT_STRENGTH_BASIC, 1, 1024);
        FORCE_LOADED_CHUNKS_TOTAL_V = cfg.comment("Total chunks each faction can force-load. Ignored when the citadel upgrade system is enabled; the per-level 'loaded_chunks' value from upgrade_levels.toml is used instead.").defineInRange("Force-loaded Chunks Total", FORCE_LOADED_CHUNKS_TOTAL, 0, 1024);
        MAX_CLAIMS_PER_FACTION_V = cfg.comment("Maximum number of chunks a single faction may claim. Set to -1 for unlimited. When the citadel upgrade system is enabled, the per-level limit is applied in addition to this cap.").defineInRange("Max Claims Per Faction", MAX_CLAIMS_PER_FACTION, -1, 1000000);
        MAX_FOBS_V = cfg.comment("Maximum number of forward operating bases (FOBs) a single faction may establish. Ignored when the citadel upgrade system is enabled; the per-level 'max_fobs' value from upgrade_levels.toml is used instead.").defineInRange("Max FOBs Per Faction", MAX_FOBS, 0, 1024);
        FOB_TICKET_LIMIT_V = cfg.comment("Maximum warp tickets a FOB can hold. Ignored when the citadel upgrade system is enabled; the per-level 'fob_ticket_limit' value from upgrade_levels.toml is used instead.").defineInRange("FOB Ticket Limit", FOB_TICKET_LIMIT, 0, 1024);
        FOB_TICKET_REGEN_PER_SIEGE_TICK_V = cfg.comment("Warp tickets restored to a FOB each time a siege timer resets. Ignored when the citadel upgrade system is enabled; the per-level 'fob_ticket_regen' value from upgrade_levels.toml is used instead.").defineInRange("FOB Ticket Regen Per Siege Tick", FOB_TICKET_REGEN_PER_SIEGE_TICK, 0, 1024);
        FOB_WARP_TICKS_V = cfg.comment("Number of ticks a player must wait before a FOB warp fires.").defineInRange("FOB Warp Ticks", FOB_WARP_TICKS, 0, 1000000);
        FOB_BLOCK_BREAKABLE_V = cfg.comment("If true, the central FOB block can be mined (very slowly, at a fixed rate independent of tool tier); otherwise it is only removed programmatically.").define("FOB Block Breakable", FOB_BLOCK_BREAKABLE);
        FOB_CLAIM_EXCLUSION_RADIUS_V = cfg.comment("Buffer, in chunks, that must separate any new claim (including a founding citadel) from an active (faction-owned) FOB. A value of N forbids claiming within N chunks (square/Chebyshev radius) of any established FOB. This applies to everyone, including the FOB's own faction. Set to 0 to forbid only the exact FOB chunk.").defineInRange("FOB Claim Exclusion Radius", FOB_CLAIM_EXCLUSION_RADIUS, 0, 64);
        FOB_VEHICLE_TICKET_COST_V = cfg.comment("Extra warp ticket cost per vehicle entity, in the form 'namespace:path=extraCost'. A warp with a vehicle costs 1 plus this value.").defineList("FOB Vehicle Ticket Cost", asList(new String[]{}), o -> o instanceof String);
        MIN_DISTANCE_BETWEEN_FACTIONS_V = cfg.comment("Minimum gap, in chunks, that must separate a new claim from an opposing (non-allied) faction's claims. A value of N forbids claiming within N chunks (square radius) of an opposing faction; allied factions are exempt. Set to 0 to disable.").defineInRange("Minimum Distance Between Opposing Factions", MIN_DISTANCE_BETWEEN_FACTIONS, 0, 64);
        CLAIM_MANAGER_RADIUS_V = cfg.comment("Square radius in chunks shown in the claim manager UI.").defineInRange("Claim Manager Radius", CLAIM_MANAGER_RADIUS, 1, 12);
        CLAIM_MAP_WHOLE_WORLD_V = cfg.comment("Map overlays (JourneyMap, Xaero) show the owner colour of every claim in the world. If false they only show claims around the player and ones seen earlier.").define("Whole World Claim Map", CLAIM_MAP_WHOLE_WORLD);
        ISLAND_COLLECTOR_SLOTS_V = cfg.comment("Number of pull-only storage slots in the faction yield collector block. Shrinking this on an existing world relocates any items that no longer fit into remaining slots. Only partly hot-reloadable: after /war reloadConfig each already-placed collector keeps its current size until its chunk next reloads.").defineInRange("Island Collector Slot Count", ISLAND_COLLECTOR_SLOTS, 1, 1024);
        ENABLE_OFFLINE_RAID_PROTECTION_V = cfg.comment("If enabled, factions cannot be sieged for a limited period after all their members go offline.").define("Enable Offline Raid Protection", ENABLE_OFFLINE_RAID_PROTECTION);
        OFFLINE_RAID_PROTECTION_HOURS_V = cfg.comment("How many hours a faction remains protected from new sieges after the last member goes offline.").defineInRange("Offline Raid Protection Hours", OFFLINE_RAID_PROTECTION_HOURS, 0, 168);
        ENABLE_SIEGE_GRACE_PERIOD_V = cfg.comment("If enabled, freshly created factions cannot be sieged for a grace period. If a graced faction starts a siege of its own, it forfeits its grace instantly.").define("Enable New Faction Siege Grace", ENABLE_SIEGE_GRACE_PERIOD);
        SIEGE_GRACE_PERIOD_HOURS_V = cfg.comment("How many hours a newly created faction stays unsiegeable. Disabling the feature above removes grace from all existing factions immediately.").defineInRange("New Faction Siege Grace Hours", SIEGE_GRACE_PERIOD_HOURS, 0, 8760);
        FACTION_REJOIN_COOLDOWN_MINUTES_V = cfg.comment("How many minutes a player must wait after leaving a faction before they may join any faction again, including the one they just left. Stops faction hopping / join spam. Set to 0 to disable. Being kicked, or losing your faction to a disband, does not start the cooldown.").defineInRange("Faction Rejoin Cooldown Minutes", FACTION_REJOIN_COOLDOWN_MINUTES, 0, 100000);
        CITADEL_MOVE_COOLDOWN_SECONDS_V = cfg.comment("How many real-world seconds a faction has to wait before moving their citadel across chunks again.").defineInRange("Citadel Move Cooldown Seconds", CITADEL_MOVE_COOLDOWN_SECONDS, 0, Integer.MAX_VALUE);
        CITADEL_MIN_Y_V = cfg.comment("The minimum Y level a citadel can be placed or moved to. Placement below this is rejected. Defaults to sea level (63). Set at or below the world floor to disable.").defineInRange("Citadel Minimum Y", CITADEL_MIN_Y, -2048, 2048);
        ENABLE_CITADEL_UPGRADES_V = cfg.comment("Applies claim limits that require upgrading to extend your faction's claim limit").define("Enable Citadel Upgrade System", ENABLE_CITADEL_UPGRADES);
        ENABLE_ISOLATED_CLAIMS_V = cfg.comment("If true, forces all newly placed claim blocks, excluding siege blocks and citadels, to be directly adjacent to a pre-existing claim.").define("Enabled Isolated Claims", ENABLE_ISOLATED_CLAIMS);
        INSURANCE_BLACKLIST_IDS_V = cfg.comment("Registry-id patterns blocked from the faction insurance stash. Supports '*' wildcards, for example 'minecraft:*shulker_box' or 'appliedenergistics2:*cell*'.").defineList("Insurance Blacklist", asList(INSURANCE_BLACKLIST_IDS), o -> o instanceof String);
        DEFAULT_FLAG_IDS_V = cfg.comment("Default built-in flags that can be chosen by factions. Each id is rendered client-side as a solid colour square/rectangle. Use a vanilla dye colour name (e.g. red, light_blue) or a 6-digit hex colour (e.g. ff8800).").defineList("Available Default Flags", asList(DEFAULT_FLAG_IDS), o -> o instanceof String);
        CUSTOM_FLAG_ALLOWLIST_V = cfg.comment("Custom server-side flags allowed from resources/warforge/flags. Use '*' to allow all validated custom flags or list exact ids without extension.").defineList("Available Custom Flags", asList(CUSTOM_FLAG_ALLOWLIST), o -> o instanceof String);
        FLAG_WHITELIST_V = cfg.comment("Restrict specific flags to specific players so factions can have exclusive flags. Each entry is 'flagId=uuid1,uuid2' where flagId matches the ids used elsewhere (e.g. 'default:red' or 'custom:myflag') and each uuid is a player's UUID. A flag listed here can only be selected by the listed players; flags not listed stay available to everyone. Multiple UUIDs per flag are supported and duplicates are ignored.").defineList("Flag Whitelist", asList(FLAG_WHITELIST), o -> o instanceof String);
        UNIQUE_FLAGS_V = cfg.comment("If true, each flag can only be used by one faction at a time: a faction cannot select a flag another faction has already taken. Admin flag overrides ('/f flag <faction> set <flagId>') bypass this check.").define("Unique Flags", UNIQUE_FLAGS);
        cfg.pop();

        cfg.push(CATEGORY_SIEGES);
        ATTACK_STRENGTH_SIEGE_CAMP_V = cfg.comment("How much attack pressure a siege camp exerts on adjacent enemy claims").defineInRange("Siege Camp Attack Strength", ATTACK_STRENGTH_SIEGE_CAMP, 1, 1024);
        MAX_SIEGES_V = cfg.comment("How many concurrent sieges each faction is allowed to open as the attacker, with any additional siege camps being unable to be placed or started by members. Set to 0 for unlimited.").defineInRange("Siege Camp Max Count Per Faction", MAX_SIEGES, 0, 1000);
        MAX_INCOMING_SIEGES_V = cfg.comment("How many concurrent sieges each faction can be the defender of at once, with any additional sieges against them being blocked. Set to 0 for unlimited.").defineInRange("Incoming Siege Max Count Per Faction", MAX_INCOMING_SIEGES, 0, 1000);
        ATTACKER_DESERTION_TIMER_V = cfg.comment("The number of seconds a siege can idle with no attackers in it before any action occurs. Setting to 0 results in checks being run every tick.").defineInRange("Attacker Desertion Timer [s]", ATTACKER_DESERTION_TIMER, 0, Integer.MAX_VALUE);
        DEFENDER_DESERTION_TIMER_V = cfg.comment("The number of seconds a siege can be undefended before any action occurs. Setting to 0 results in checks being run every tick.").defineInRange("Defender Desertion Timer [s]", DEFENDER_DESERTION_TIMER, 0, Integer.MAX_VALUE);
        ATTACKER_CONQUERED_CHUNK_PERIOD_V = cfg.comment("Milliseconds a chunk stays conquered no-man's-land after attackers win a siege on it: unclaimable by everyone and unprotected (free-build wilderness) until it reverts to normal wilderness. Setting to 0 makes won chunks immediately claimable.").defineInRange("Attacker Conquered Chunk Grace Period [ms]", ATTACKER_CONQUERED_CHUNK_PERIOD, 0, Integer.MAX_VALUE);
        DEFENDER_CONQUERED_CHUNK_PERIOD_V = cfg.comment("The number of milliseconds to deny sieging or claiming in previously sieged chunk in which the siege was won by the defenders. Setting to 0 results in no grace period.").defineInRange("Defender Conquered Chunk Grace Period [ms]", DEFENDER_CONQUERED_CHUNK_PERIOD, 0, Integer.MAX_VALUE);
        COMBAT_LOG_THRESHOLD_V = cfg.comment("The number of milliseconds before enforcement action is taken when a player leaves during a siege on any of their claims.").defineInRange("Time to Combat Log Action [ms]", COMBAT_LOG_THRESHOLD, 0, Integer.MAX_VALUE);
        LIVE_QUIT_TIMER_V = cfg.comment("The number of milliseconds before a defending team which went offline after a siege against them has begun is considered to have quit, forfeiting the siege.").defineInRange("Time to Live Quit Siege [ms]", LIVE_QUIT_TIMER, 0, Integer.MAX_VALUE);
        SIEGE_ALLOW_UI_DECLARE_V = cfg.comment("If enabled, officers can declare a siege directly from the claim map UI (consuming a siege camp block item) without physically placing a siege camp.").define("Allow UI Siege Declaration", SIEGE_ALLOW_UI_DECLARE);
        SIEGE_DECLARE_MAX_RANGE_V = cfg.comment("When declaring a siege from the claim map UI, the maximum chunk distance (chebyshev) allowed between the chosen start-from chunk and the target chunk.").defineInRange("UI Siege Declaration Max Range", SIEGE_DECLARE_MAX_RANGE, 1, 64);
        SIEGE_DECLARE_REQUIRE_PRESENCE_V = cfg.comment("If enabled, a UI-declared (camp-less) siege fails if no attacking faction member stays within the attacker radius of the start-from chunk for the attacker desertion timer, mirroring physical siege camps.").define("UI Siege Requires Attacker Presence", SIEGE_DECLARE_REQUIRE_PRESENCE);
        MAX_OFFLINE_PLAYER_COUNT_MINIMUM_V = cfg.comment("A static minimum for the maximum number of players which can have been online at some point during a siege before the faction online player count dropping to 0 indicates a live quit. Negative values override the percent").defineInRange("Max Players Before Online Status", MAX_OFFLINE_PLAYER_COUNT_MINIMUM, Integer.MIN_VALUE, Integer.MAX_VALUE);
        MAX_OFFLINE_PLAYER_PERCENT_V = cfg.comment("The maximum percent of players in a faction which can be online at some point during a siege before the online count dropping to 0 indicates a live quit.").defineInRange("Max Player % Before Online Status", (double) MAX_OFFLINE_PLAYER_PERCENT, 0d, 1.0d);
        VERTICAL_SIEGE_DIST_V = cfg.comment("The number of blocks up or down a siege block can be placed from a potential target, inclusively. Sieges may also only be started on targets within this vertical radius.").defineInRange("Maximum Vertical Siege Radius [Inclusive]", VERTICAL_SIEGE_DIST, 0, Integer.MAX_VALUE);
        SIEGE_BATTLE_RADIUS_V = cfg.comment("Outer 'War' zone: chunks (square radius) from each active siege camp where kills count toward the siege and foes cannot break/place blocks (WarFriend/WarFoe profiles). Should be >= the Sieged radius.").defineInRange("Battle Square Chunk Radius From Siege", SIEGE_BATTLE_RADIUS, 0, Integer.MAX_VALUE);
        SIEGE_SIEGED_RADIUS_V = cfg.comment("Inner 'Sieged' zone: chunks (square radius) from each active siege camp where chunk protection is fully disabled - anyone may breach (SiegedFriend/SiegedFoe profiles). Kills also count here. Typically smaller than the War radius.").defineInRange("Sieged Square Chunk Radius From Siege", SIEGE_SIEGED_RADIUS, 0, Integer.MAX_VALUE);
        SIEGE_KILL_DETECTION_MODE_V = cfg.comment("How siege kills are detected. PRECISE (default): only a confirmed kill by an opposing player inside the War/Sieged zone counts (an attacker must kill a defender, or a defender an attacker). SIMPLE: any participant death inside the zone counts toward the siege, including environmental/mob/fall deaths - e.g. a defender dying to fall damage awards the attackers a point.").defineEnum("Kill Detection Mode", SIEGE_KILL_DETECTION_MODE);
        SIEGE_ATTACKER_RADIUS_V = cfg.comment("The number of chunks in any direction from the siege block that an attacker can be in to prevent siege abandon.").defineInRange("Attacker Square Chunk Radius From Siege", SIEGE_ATTACKER_RADIUS, 0, Integer.MAX_VALUE);
        SIEGE_DEFENDER_RADIUS_V = cfg.comment("The number of chunks in any direction from the siege block that a defender can be in to prevent siege abandon.").defineInRange("Defender Square Chunk Radius From Siege", SIEGE_DEFENDER_RADIUS, 0, Integer.MAX_VALUE);
        SIEGE_SWING_PER_DEFENDER_DEATH_V = cfg.comment("How much a siege progress swings when a defender dies in the siege").defineInRange("Siege Swing Per Defender Death", SIEGE_SWING_PER_DEFENDER_DEATH, 0, 1024);
        SIEGE_STALL_ESCALATION_THRESHOLD_V = cfg.comment("Anti-stall: once the defenders' lead exceeds this many points, each consecutive siege-timer tick with no kills doubles the attackers' swing (reset by any kill). Only reachable when Siege Defence Threshold is higher than this. Set very high to effectively disable.").defineInRange("Siege Stall Escalation Threshold", SIEGE_STALL_ESCALATION_THRESHOLD, 0, 1024);
        SIEGE_SWING_PER_ATTACKER_DEATH_V = cfg.comment("How much a siege progress swings when an attacker dies in the siege").defineInRange("Siege Swing Per Attacker Death", SIEGE_SWING_PER_ATTACKER_DEATH, 0, 1024);
        SIEGE_SWING_PER_DAY_ELAPSED_BASE_V = cfg.comment("How much a siege progress swings each day (see below). This happens regardless of logins").defineInRange("Siege Swing Per Day", SIEGE_SWING_PER_DAY_ELAPSED_BASE, 0, 1024);
        SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS_V = cfg.comment("How much a siege progress swings when no attackers have logged on for a day (see below)").defineInRange("Siege Swing Per Day Without Attacker Logins", SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS, 0, 1024);
        SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS_V = cfg.comment("How much a siege progress swings when no defenders have logged on for a day (see below)").defineInRange("Siege Swing Per Day Without Defender Logins", SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS, 0, 1024);
        SIEGE_DAY_LENGTH_V = cfg.comment("The length of a day for siege login purposes, in real-world hours.").defineInRange("Siege Day Length", (double) SIEGE_DAY_LENGTH, 0.0001d, 100000d);
        SIEGE_DEFENCE_THRESHOLD_V = cfg.comment("How many points the defenders must swing the siege in their favour to repel it (attackers win at their own difficulty threshold).").defineInRange("Siege Defence Threshold", SIEGE_DEFENCE_THRESHOLD, 1, 1024);
        SIEGE_END_ON_GOAL_REACHED_V = cfg.comment("If true, a siege concludes the instant a side reaches its point goal (e.g. via a kill), even mid-timer. If false, reaching the goal does not end the siege early; it only concludes when the siege timer next elapses.").define("Siege Ends On Goal Reached", SIEGE_END_ON_GOAL_REACHED);
        SIEGE_INFO_RADIUS_V = cfg.comment("The range at which you see siege information. (Capped by the server setting)").defineInRange("Siege Info Radius", (double) SIEGE_INFO_RADIUS, 1d, 1000d);
        SIEGE_COOLDOWN_FAIL_V = cfg.comment("Cooldown between sieges, in minutes").defineInRange("Cooldown between sieges after failure", SIEGE_COOLDOWN_FAIL, 0, 100000);
        SIEGE_DIFF_PER_MEMBER_V = cfg.comment("How much having a defender flag at a base reinforces the difficulty of the siege for the attackers").defineInRange("Siege Difficulty Reinforcement Per Defender Flag", SIEGE_DIFF_PER_MEMBER, 0, 1024);
        SIEGE_CAPTURE_V = cfg.comment("Does a successful siege convert the claim").define("Siege Captures", SIEGE_CAPTURE);
        SIEGE_ENABLE_NEW_TIMER_V = cfg.comment("Enable new per siege time system, instead of a central siege day, each siege has its own timer, starting with the siege").define("Enable Per-Siege timer", SIEGE_ENABLE_NEW_TIMER);
        SIEGE_MOMENTUM_DURATION_V = cfg.comment("Time the momentum lasts").defineInRange("Siege momentum duration", 60, 1, Integer.MAX_VALUE);
        SIEGE_MOMENTUM_MULTIPLIERS_V = cfg.comment("List of momentum multipliers in the form level=multiplier").defineList("SiegeMomentumMultipliers",
                asList(new String[]{"0=15:00", "1=10:00", "2=8:30", "3=5:00", "4=2:30"}), o -> o instanceof String);
        cfg.pop();

        cfg.push(CATEGORY_ALLIANCES);
        ALLIANCE_TRUCE_DURATION_MINUTES_V = cfg.comment("When an alliance is broken, both factions enter a truce during which they cannot siege or harm each other. Length in minutes; 0 disables the truce.").defineInRange("Alliance Truce Duration [min]", ALLIANCE_TRUCE_DURATION_MINUTES, 0, 525600);
        MAX_ALLIES_V = cfg.comment("Maximum number of simultaneous alliances a faction may hold. Set to -1 for unlimited.").defineInRange("Max Allies Per Faction", MAX_ALLIES, -1, 1000);
        cfg.pop();

        cfg.push(CATEGORY_GENERAL);
        FACTION_NAME_LENGTH_MAX_V = cfg.comment("How many characters long can a faction name be.").defineInRange("Max Faction Name Length", FACTION_NAME_LENGTH_MAX, 3, 128);
        FACTION_NAME_BANLIST_V = cfg.comment("Case-insensitive substrings that disallow faction names from being created or renamed.").defineList("Faction Name Banlist", asList(FACTION_NAME_BANLIST), o -> o instanceof String);
        cfg.pop();

        cfg.push(CATEGORY_YIELDS);
        String qualityText = "The global multiplier for %s quality veins which all veins fall back to if they do not have an override.";
        YIELD_DAY_LENGTH_V = cfg.comment("The length of time between yields, in real-world seconds.").defineInRange("Yield Day Length", YIELD_DAY_LENGTH, 1, Integer.MAX_VALUE);
        String tickComment = "If true, this timer advances on server ticks (game time that pauses while the server is stopped or not ticking) instead of real-world wall-clock time. Each timer is independent. Do not change on an existing world that has active factions; stored timestamps are not converted. NOT safely hot-reloadable: /war reloadConfig will read the new value, but flipping the timebase on a live world mixes tick-time and wall-clock timestamps and corrupts active timers, so only change this with the server stopped.";
        TICK_YIELDS_V = cfg.comment("Passive yield cycle. " + tickComment).define("Tick-Based Yield Timer", TICK_YIELDS);
        TICK_SIEGES_V = cfg.comment("Siege day advance, siege end-timer countdown, siege start cooldown, and conquered-chunk revert. " + tickComment).define("Tick-Based Siege Timers", TICK_SIEGES);
        TICK_MOMENTUM_V = cfg.comment("Siege momentum expiry. " + tickComment).define("Tick-Based Siege Momentum", TICK_MOMENTUM);
        TICK_OFFLINE_PROTECTION_V = cfg.comment("Offline raid protection window. " + tickComment).define("Tick-Based Offline Raid Protection", TICK_OFFLINE_PROTECTION);
        TICK_SIEGE_GRACE_V = cfg.comment("New-faction siege grace period. " + tickComment).define("Tick-Based Siege Grace", TICK_SIEGE_GRACE);
        TICK_CITADEL_MOVE_V = cfg.comment("Citadel move cooldown. " + tickComment).define("Tick-Based Citadel Move Cooldown", TICK_CITADEL_MOVE);
        TICK_TRUCES_V = cfg.comment("Truce durations. " + tickComment).define("Tick-Based Truces", TICK_TRUCES);
        TICK_FACTION_REJOIN_V = cfg.comment("Faction rejoin cooldown. " + tickComment).define("Tick-Based Faction Rejoin Cooldown", TICK_FACTION_REJOIN);
        POOR_QUAL_MULT_V = cfg.comment(String.format(qualityText, Quality.POOR)).defineInRange("Global Poor Quality Multiplier", (double) POOR_QUAL_MULT, 0d, 512d);
        FAIR_QUAL_MULT_V = cfg.comment(String.format(qualityText, Quality.FAIR)).defineInRange("Global Fair Quality Multiplier", (double) FAIR_QUAL_MULT, 0d, 512d);
        RICH_QUAL_MULT_V = cfg.comment(String.format(qualityText, Quality.RICH)).defineInRange("Global Rich Quality Multiplier", (double) RICH_QUAL_MULT, 0d, 512d);
        cfg.pop();

        cfg.push(CATEGORY_NOTORIETY);
        NOTORIETY_PER_PLAYER_KILL_V = cfg.comment("How much notoriety a player earns for their faction when killing another player").defineInRange("Notoriety gain per PVP kill", NOTORIETY_PER_PLAYER_KILL, 0, 1024);
        NOTORIETY_PER_SIEGE_ATTACK_SUCCESS_V = cfg.comment("How much notoriety a faction earns when successfully winning an siege as attacker").defineInRange("Notoriety gain per siege attack win", NOTORIETY_PER_SIEGE_ATTACK_SUCCESS, 0, 1024);
        NOTORIETY_PER_SIEGE_DEFEND_SUCCESS_V = cfg.comment("How much notoriety a faction earns when successfully defending a siege").defineInRange("Notoriety gain per siege defend win", NOTORIETY_PER_SIEGE_DEFEND_SUCCESS, 0, 1024);
        NOTORIETY_KILL_CAP_PER_PLAYER_V = cfg.comment("How many times a faction can kill the same player and still get points").defineInRange("Max # kills per player", NOTORIETY_KILL_CAP_PER_PLAYER, 0, 1024);
        cfg.pop();

        cfg.push(CATEGORY_LEGACY);
        LEGACY_PER_DAY_V = cfg.comment("How much legacy a faction gets for having at least one player on").defineInRange("Legacy gain per day", LEGACY_PER_DAY, 0, 1024);
        LEGACY_USES_YIELD_TIMER_V = cfg.comment("If true, legacy triggers every yield timer. Otherwise, every siege timer").define("Legacy uses yield timer", LEGACY_USES_YIELD_TIMER);
        cfg.pop();

        cfg.push(CATEGORY_DISPLAY);
        FACTION_PREFIX_IN_CHAT_V = cfg.comment("If enabled, a player's faction name is shown as a coloured prefix before their name in chat.").define("Faction Prefix In Chat", FACTION_PREFIX_IN_CHAT);
        FACTION_PREFIX_IN_TABLIST_V = cfg.comment("If enabled, a player's faction name is shown as a coloured prefix before their name in the tab player list.").define("Faction Prefix In Tab List", FACTION_PREFIX_IN_TABLIST);
        JOURNEYMAP_VEIN_MODE_V = cfg.comment("Draws each chunk's ore vein as a small item icon on JourneyMap, if it is installed. Server-authoritative.\n"
                        + "DISABLED = no overlay.\n"
                        + "AUTO = veins for a wide radius around each player are sent automatically as they move; no need to enter each chunk.\n"
                        + "PLAYER = a chunk's vein only appears once the player has walked into and loaded that chunk; unobserved veins are never sent to the client.")
                .define("JourneyMap Vein Display Mode", "DISABLED", o -> isJourneyMapMode(o));
        JOURNEYMAP_VEIN_AUTO_RADIUS_V = cfg.comment("In AUTO vein mode, how many chunks around each player to reveal veins for. Larger values reveal more of the map but cause the server to generate and store veins over a wider area.").defineInRange("JourneyMap Vein Auto Radius", JOURNEYMAP_VEIN_AUTO_RADIUS, 1, 128);
        cfg.pop();

        cfg.push(CATEGORY_WARPS);
        ENABLE_F_HOME_COMMAND_V = cfg.comment("Allow players to use /f home to teleport to their citadel").define("Enable /f home Command", ENABLE_F_HOME_COMMAND);
        ALLOW_F_HOME_BETWEEN_DIMENSIONS_V = cfg.comment("Allow players to use /f home when in a different dimension to their citadel").define("Allow /f home across dimensions", ALLOW_F_HOME_BETWEEN_DIMENSIONS);
        ENABLE_SPAWN_COMMAND_V = cfg.comment("Allow players to use /spawn to teleport to the world spawn").define("Enable /spawn Command", ENABLE_SPAWN_COMMAND);
        ALLOW_SPAWN_BETWEEN_DIMENSIONS_V = cfg.comment("Allow players to use /spawn when in a different dimension to the world spawn").define("Allow /spawn across dimensions", ALLOW_SPAWN_BETWEEN_DIMENSIONS);
        SPAWN_AT_CITADEL_V = cfg.comment("If enabled, bed and respawn-anchor spawn points are disabled and players respawn in their faction citadel's chunk, at the citadel's Y level. Players not in a faction fall back to the world spawn.").define("Respawn At Citadel", SPAWN_AT_CITADEL);
        ALLOW_BED_SPAWN_IN_CLAIMS_V = cfg.comment("Only used when Respawn At Citadel is enabled. If enabled, players may set bed and respawn-anchor spawn points, but only inside chunks claimed by their own faction. They then respawn at that spawn point; if the bed is broken or the chunk is no longer claimed by their faction, they respawn at their citadel instead.").define("Allow Bed Spawn In Claims", ALLOW_BED_SPAWN_IN_CLAIMS);
        NUM_TICKS_FOR_WARP_COMMANDS_V = cfg.comment("How many ticks must the player stand still for a warp command to take effect").defineInRange("Num Ticks for Warps", NUM_TICKS_FOR_WARP_COMMANDS, 0, 20 * 60 * 5);
        ENABLE_TPA_COMMAND_V = cfg.comment("Allow players to use /f tpa <player> to request a teleport to another player").define("Enable /f tpa Command", ENABLE_TPA_COMMAND);
        TPA_REQUEST_TIMEOUT_SECONDS_V = cfg.comment("How many seconds a pending teleport request lasts before it expires").defineInRange("TPA Request Timeout Seconds", TPA_REQUEST_TIMEOUT_SECONDS, 1, 60 * 30);
        cfg.pop();
    }

    private static boolean isJourneyMapMode(Object o) {
        return o instanceof String s && (s.equals("DISABLED") || s.equals("AUTO") || s.equals("PLAYER"));
    }

    private static List<Integer> intList(int[] arr) {
        ArrayList<Integer> out = new ArrayList<>(arr.length);
        for (int i : arr) out.add(i);
        return out;
    }

    private static int[] toIntArray(List<? extends Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i);
        return out;
    }

    private static String[] toStringArray(List<? extends String> list) {
        return list.toArray(new String[0]);
    }

    private static Path configPath() {
        return Platform.configDir().resolve(Tags.MODID + "-common.toml");
    }

    /** Server-side reload of the toml file; never touches the loaded spec. */
    public static void reloadFromDisk() {
        CommentedFileConfig file = CommentedFileConfig.builder(configPath())
                .sync()
                .preserveInsertionOrder()
                .writingMode(WritingMode.REPLACE)
                .build();
        try {
            file.load();
            SPEC.correct(file);
            bakeFrom(new Overlay(file));
        } finally {
            file.close();
        }
    }

    public record Snapshot(String toml, short megachunkLength) {}

    public static Snapshot snapshot() {
        CommentedFileConfig file = CommentedFileConfig.builder(configPath()).sync().build();
        try {
            file.load();
            StringWriter out = new StringWriter();
            new TomlWriter().write(Config.copy(file), out);
            return new Snapshot(out.toString(), VEIN_HANDLER.megachunkLength);
        } finally {
            file.close();
        }
    }

    /** Client side: local file with the server's values merged over it; baked into the statics, spec untouched. */
    public static void apply(Snapshot snapshot) {
        CommentedConfig serverValues = new TomlParser().parse(new StringReader(snapshot.toml()));

        CommentedConfig merged;
        CommentedFileConfig local = CommentedFileConfig.builder(configPath()).sync().build();
        try {
            local.load();
            merged = CommentedConfig.copy(local);
        } finally {
            local.close();
        }

        deepMerge(serverValues, merged);
        SPEC.correct(merged);
        bakeFrom(new Overlay(merged));
    }

    private static void deepMerge(UnmodifiableConfig from, CommentedConfig into) {
        for (UnmodifiableConfig.Entry entry : from.entrySet()) {
            List<String> path = Collections.singletonList(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig sub) {
                Object existing = into.get(path);
                if (existing instanceof CommentedConfig existingSub) {
                    deepMerge(sub, existingSub);
                } else {
                    into.set(path, CommentedConfig.copy(sub));
                }
            } else {
                into.set(path, value);
            }
        }
    }

    private static volatile boolean registriesReady;

    /** Registry ids resolve only once registration is done; later bakes resolve inline. */
    public static void onRegistriesReady() {
        registriesReady = true;
        resolveRegistryEntries();
    }

    private static void resolveRegistryEntries() {
        UNCLAIMED.findBlocks();
        SAFE_ZONE.findBlocks();
        WAR_ZONE.findBlocks();
        CITADEL_FRIEND.findBlocks();
        CITADEL_FOE.findBlocks();
        CLAIM_FRIEND.findBlocks();
        CLAIM_ALLY.findBlocks();
        CLAIM_FOE.findBlocks();
        CLAIM_DEFENDED.findBlocks();
        SIEGED_FRIEND.findBlocks();
        SIEGED_FOE.findBlocks();
        WAR_FRIEND.findBlocks();
        WAR_FOE.findBlocks();
    }

    private interface Source {
        int integer(ModConfigSpec.IntValue v);

        boolean bool(ModConfigSpec.BooleanValue v);

        double dbl(ModConfigSpec.DoubleValue v);

        <E extends Enum<E>> E enumeration(ModConfigSpec.EnumValue<E> v);

        List<String> strings(ModConfigSpec.ConfigValue<List<? extends String>> v);

        String string(ModConfigSpec.ConfigValue<String> v);
    }

    private static final Source LIVE = new Source() {
        public int integer(ModConfigSpec.IntValue v) { return v.get(); }

        public boolean bool(ModConfigSpec.BooleanValue v) { return v.get(); }

        public double dbl(ModConfigSpec.DoubleValue v) { return v.get(); }

        public <E extends Enum<E>> E enumeration(ModConfigSpec.EnumValue<E> v) { return v.get(); }

        public List<String> strings(ModConfigSpec.ConfigValue<List<? extends String>> v) { return List.copyOf(v.get()); }

        public String string(ModConfigSpec.ConfigValue<String> v) { return v.get(); }
    };

    /** Reads corrected toml values; the config must have gone through SPEC.correct. */
    private record Overlay(UnmodifiableConfig config) implements Source {
        public int integer(ModConfigSpec.IntValue v) { return ((Number) config.get(v.getPath())).intValue(); }

        public boolean bool(ModConfigSpec.BooleanValue v) { return config.get(v.getPath()); }

        public double dbl(ModConfigSpec.DoubleValue v) { return ((Number) config.get(v.getPath())).doubleValue(); }

        public <E extends Enum<E>> E enumeration(ModConfigSpec.EnumValue<E> v) {
            Object raw = config.get(v.getPath());
            return raw instanceof Enum<?> ? v.getDefault().getDeclaringClass().cast(raw) : Enum.valueOf(v.getDefault().getDeclaringClass(), raw.toString());
        }

        public List<String> strings(ModConfigSpec.ConfigValue<List<? extends String>> v) {
            List<?> raw = config.get(v.getPath());
            ArrayList<String> out = new ArrayList<>(raw.size());
            for (Object o : raw) {
                out.add((String) o);
            }
            return out;
        }

        public String string(ModConfigSpec.ConfigValue<String> v) { return config.get(v.getPath()); }
    }

    public static void bake() {
        bakeFrom(LIVE);
    }

    private static void bakeFrom(Source src) {
        UNCLAIMED.bake(src);
        SAFE_ZONE.bake(src);
        WAR_ZONE.bake(src);
        CITADEL_FRIEND.bake(src);
        CITADEL_FOE.bake(src);
        CLAIM_FRIEND.bake(src);
        CLAIM_ALLY.bake(src);
        CLAIM_FOE.bake(src);
        CLAIM_DEFENDED.bake(src);
        SIEGED_FRIEND.bake(src);
        SIEGED_FOE.bake(src);
        WAR_FRIEND.bake(src);
        WAR_FOE.bake(src);

        CLAIM_DIM_WHITELIST = toStringArray(src.strings(CLAIM_DIM_WHITELIST_V));
        CLAIM_STRENGTH_CITADEL = src.integer(CLAIM_STRENGTH_CITADEL_V);
        CLAIM_STRENGTH_REINFORCED = src.integer(CLAIM_STRENGTH_REINFORCED_V);
        CLAIM_STRENGTH_BASIC = src.integer(CLAIM_STRENGTH_BASIC_V);
        SUPPORT_STRENGTH_CITADEL = src.integer(SUPPORT_STRENGTH_CITADEL_V);
        SUPPORT_STRENGTH_REINFORCED = src.integer(SUPPORT_STRENGTH_REINFORCED_V);
        SUPPORT_STRENGTH_BASIC = src.integer(SUPPORT_STRENGTH_BASIC_V);
        FORCE_LOADED_CHUNKS_TOTAL = src.integer(FORCE_LOADED_CHUNKS_TOTAL_V);
        MAX_CLAIMS_PER_FACTION = src.integer(MAX_CLAIMS_PER_FACTION_V);
        MAX_FOBS = src.integer(MAX_FOBS_V);
        FOB_TICKET_LIMIT = src.integer(FOB_TICKET_LIMIT_V);
        FOB_TICKET_REGEN_PER_SIEGE_TICK = src.integer(FOB_TICKET_REGEN_PER_SIEGE_TICK_V);
        FOB_WARP_TICKS = src.integer(FOB_WARP_TICKS_V);
        FOB_BLOCK_BREAKABLE = src.bool(FOB_BLOCK_BREAKABLE_V);
        FOB_CLAIM_EXCLUSION_RADIUS = src.integer(FOB_CLAIM_EXCLUSION_RADIUS_V);

        FobWarpQueue.configureVehicleCost(src.strings(FOB_VEHICLE_TICKET_COST_V));
        MIN_DISTANCE_BETWEEN_FACTIONS = src.integer(MIN_DISTANCE_BETWEEN_FACTIONS_V);
        CLAIM_MANAGER_RADIUS = src.integer(CLAIM_MANAGER_RADIUS_V);
        CLAIM_MAP_WHOLE_WORLD = src.bool(CLAIM_MAP_WHOLE_WORLD_V);
        ISLAND_COLLECTOR_SLOTS = src.integer(ISLAND_COLLECTOR_SLOTS_V);
        ENABLE_OFFLINE_RAID_PROTECTION = src.bool(ENABLE_OFFLINE_RAID_PROTECTION_V);
        OFFLINE_RAID_PROTECTION_HOURS = src.integer(OFFLINE_RAID_PROTECTION_HOURS_V);
        ENABLE_SIEGE_GRACE_PERIOD = src.bool(ENABLE_SIEGE_GRACE_PERIOD_V);
        SIEGE_GRACE_PERIOD_HOURS = src.integer(SIEGE_GRACE_PERIOD_HOURS_V);
        FACTION_REJOIN_COOLDOWN_MINUTES = src.integer(FACTION_REJOIN_COOLDOWN_MINUTES_V);
        CITADEL_MOVE_COOLDOWN_SECONDS = src.integer(CITADEL_MOVE_COOLDOWN_SECONDS_V);
        CITADEL_MIN_Y = src.integer(CITADEL_MIN_Y_V);
        ENABLE_CITADEL_UPGRADES = src.bool(ENABLE_CITADEL_UPGRADES_V);
        ENABLE_ISOLATED_CLAIMS = src.bool(ENABLE_ISOLATED_CLAIMS_V);
        INSURANCE_BLACKLIST_IDS = toStringArray(src.strings(INSURANCE_BLACKLIST_IDS_V));
        DEFAULT_FLAG_IDS = toStringArray(src.strings(DEFAULT_FLAG_IDS_V));
        CUSTOM_FLAG_ALLOWLIST = toStringArray(src.strings(CUSTOM_FLAG_ALLOWLIST_V));
        FLAG_WHITELIST = toStringArray(src.strings(FLAG_WHITELIST_V));
        UNIQUE_FLAGS = src.bool(UNIQUE_FLAGS_V);

        ATTACK_STRENGTH_SIEGE_CAMP = src.integer(ATTACK_STRENGTH_SIEGE_CAMP_V);
        MAX_SIEGES = src.integer(MAX_SIEGES_V);
        MAX_INCOMING_SIEGES = src.integer(MAX_INCOMING_SIEGES_V);
        ATTACKER_DESERTION_TIMER = src.integer(ATTACKER_DESERTION_TIMER_V);
        DEFENDER_DESERTION_TIMER = src.integer(DEFENDER_DESERTION_TIMER_V);
        ATTACKER_CONQUERED_CHUNK_PERIOD = src.integer(ATTACKER_CONQUERED_CHUNK_PERIOD_V);
        DEFENDER_CONQUERED_CHUNK_PERIOD = src.integer(DEFENDER_CONQUERED_CHUNK_PERIOD_V);
        COMBAT_LOG_THRESHOLD = src.integer(COMBAT_LOG_THRESHOLD_V);
        LIVE_QUIT_TIMER = src.integer(LIVE_QUIT_TIMER_V);
        SIEGE_ALLOW_UI_DECLARE = src.bool(SIEGE_ALLOW_UI_DECLARE_V);
        SIEGE_DECLARE_MAX_RANGE = src.integer(SIEGE_DECLARE_MAX_RANGE_V);
        SIEGE_DECLARE_REQUIRE_PRESENCE = src.bool(SIEGE_DECLARE_REQUIRE_PRESENCE_V);
        MAX_OFFLINE_PLAYER_COUNT_MINIMUM = src.integer(MAX_OFFLINE_PLAYER_COUNT_MINIMUM_V);
        MAX_OFFLINE_PLAYER_PERCENT = (float) src.dbl(MAX_OFFLINE_PLAYER_PERCENT_V);
        VERTICAL_SIEGE_DIST = src.integer(VERTICAL_SIEGE_DIST_V);
        SIEGE_BATTLE_RADIUS = src.integer(SIEGE_BATTLE_RADIUS_V);
        SIEGE_SIEGED_RADIUS = src.integer(SIEGE_SIEGED_RADIUS_V);
        SIEGE_KILL_DETECTION_MODE = src.enumeration(SIEGE_KILL_DETECTION_MODE_V);
        SIEGE_STALL_ESCALATION_THRESHOLD = src.integer(SIEGE_STALL_ESCALATION_THRESHOLD_V);
        SIEGE_ATTACKER_RADIUS = src.integer(SIEGE_ATTACKER_RADIUS_V);
        SIEGE_DEFENDER_RADIUS = src.integer(SIEGE_DEFENDER_RADIUS_V);
        SIEGE_SWING_PER_DEFENDER_DEATH = src.integer(SIEGE_SWING_PER_DEFENDER_DEATH_V);
        SIEGE_SWING_PER_ATTACKER_DEATH = src.integer(SIEGE_SWING_PER_ATTACKER_DEATH_V);
        SIEGE_SWING_PER_DAY_ELAPSED_BASE = src.integer(SIEGE_SWING_PER_DAY_ELAPSED_BASE_V);
        SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS = src.integer(SIEGE_SWING_PER_DAY_ELAPSED_NO_ATTACKER_LOGINS_V);
        SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS = src.integer(SIEGE_SWING_PER_DAY_ELAPSED_NO_DEFENDER_LOGINS_V);
        SIEGE_DAY_LENGTH = (float) src.dbl(SIEGE_DAY_LENGTH_V);
        SIEGE_DEFENCE_THRESHOLD = src.integer(SIEGE_DEFENCE_THRESHOLD_V);
        SIEGE_END_ON_GOAL_REACHED = src.bool(SIEGE_END_ON_GOAL_REACHED_V);
        SIEGE_INFO_RADIUS = (float) src.dbl(SIEGE_INFO_RADIUS_V);
        SIEGE_COOLDOWN_FAIL = src.integer(SIEGE_COOLDOWN_FAIL_V);
        SIEGE_DIFF_PER_MEMBER = src.integer(SIEGE_DIFF_PER_MEMBER_V);
        SIEGE_CAPTURE = src.bool(SIEGE_CAPTURE_V);
        SIEGE_ENABLE_NEW_TIMER = src.bool(SIEGE_ENABLE_NEW_TIMER_V);
        SIEGE_MOMENTUM_DURATION = src.integer(SIEGE_MOMENTUM_DURATION_V);

        SiegeMomentum.configure(src.strings(SIEGE_MOMENTUM_MULTIPLIERS_V));

        ALLIANCE_TRUCE_DURATION_MINUTES = src.integer(ALLIANCE_TRUCE_DURATION_MINUTES_V);
        MAX_ALLIES = src.integer(MAX_ALLIES_V);

        YIELD_DAY_LENGTH = src.integer(YIELD_DAY_LENGTH_V);
        TICK_YIELDS = src.bool(TICK_YIELDS_V);
        TICK_SIEGES = src.bool(TICK_SIEGES_V);
        TICK_MOMENTUM = src.bool(TICK_MOMENTUM_V);
        TICK_OFFLINE_PROTECTION = src.bool(TICK_OFFLINE_PROTECTION_V);
        TICK_SIEGE_GRACE = src.bool(TICK_SIEGE_GRACE_V);
        TICK_CITADEL_MOVE = src.bool(TICK_CITADEL_MOVE_V);
        TICK_TRUCES = src.bool(TICK_TRUCES_V);
        TICK_FACTION_REJOIN = src.bool(TICK_FACTION_REJOIN_V);
        POOR_QUAL_MULT = (float) src.dbl(POOR_QUAL_MULT_V);
        FAIR_QUAL_MULT = (float) src.dbl(FAIR_QUAL_MULT_V);
        RICH_QUAL_MULT = (float) src.dbl(RICH_QUAL_MULT_V);

        NOTORIETY_PER_PLAYER_KILL = src.integer(NOTORIETY_PER_PLAYER_KILL_V);
        NOTORIETY_PER_SIEGE_ATTACK_SUCCESS = src.integer(NOTORIETY_PER_SIEGE_ATTACK_SUCCESS_V);
        NOTORIETY_PER_SIEGE_DEFEND_SUCCESS = src.integer(NOTORIETY_PER_SIEGE_DEFEND_SUCCESS_V);
        NOTORIETY_KILL_CAP_PER_PLAYER = src.integer(NOTORIETY_KILL_CAP_PER_PLAYER_V);

        LEGACY_PER_DAY = src.integer(LEGACY_PER_DAY_V);
        LEGACY_USES_YIELD_TIMER = src.bool(LEGACY_USES_YIELD_TIMER_V);

        FACTION_NAME_LENGTH_MAX = src.integer(FACTION_NAME_LENGTH_MAX_V);
        FACTION_NAME_BANLIST = toStringArray(src.strings(FACTION_NAME_BANLIST_V));

        FACTION_PREFIX_IN_CHAT = src.bool(FACTION_PREFIX_IN_CHAT_V);
        FACTION_PREFIX_IN_TABLIST = src.bool(FACTION_PREFIX_IN_TABLIST_V);
        JOURNEYMAP_VEIN_MODE = parseJourneyMapMode(src.string(JOURNEYMAP_VEIN_MODE_V));
        JOURNEYMAP_VEIN_AUTO_RADIUS = src.integer(JOURNEYMAP_VEIN_AUTO_RADIUS_V);

        ENABLE_F_HOME_COMMAND = src.bool(ENABLE_F_HOME_COMMAND_V);
        ALLOW_F_HOME_BETWEEN_DIMENSIONS = src.bool(ALLOW_F_HOME_BETWEEN_DIMENSIONS_V);
        ENABLE_SPAWN_COMMAND = src.bool(ENABLE_SPAWN_COMMAND_V);
        ALLOW_SPAWN_BETWEEN_DIMENSIONS = src.bool(ALLOW_SPAWN_BETWEEN_DIMENSIONS_V);
        SPAWN_AT_CITADEL = src.bool(SPAWN_AT_CITADEL_V);
        ALLOW_BED_SPAWN_IN_CLAIMS = src.bool(ALLOW_BED_SPAWN_IN_CLAIMS_V);
        NUM_TICKS_FOR_WARP_COMMANDS = src.integer(NUM_TICKS_FOR_WARP_COMMANDS_V);
        ENABLE_TPA_COMMAND = src.bool(ENABLE_TPA_COMMAND_V);
        TPA_REQUEST_TIMEOUT_SECONDS = src.integer(TPA_REQUEST_TIMEOUT_SECONDS_V);

        if (registriesReady) {
            resolveRegistryEntries();
        }
    }

    public static class ProtectionConfig {
        public boolean BREAK_BLOCKS = true;
        public boolean PLACE_BLOCKS = true;
        public boolean INTERACT = true;
        public boolean USE_ITEM = true;
        public boolean BLOCK_REMOVAL = true;
        public boolean EXPLOSION_DAMAGE = false;
        public boolean PLAYER_TAKE_DAMAGE_FROM_MOB = true;
        public boolean PLAYER_TAKE_DAMAGE_FROM_PLAYER = true;
        public boolean PLAYER_TAKE_DAMAGE_FROM_OTHER = true;
        public boolean PLAYER_DEAL_DAMAGE = true;
        public boolean ALLOW_MOB_SPAWNS = true;
        public boolean ALLOW_MOB_ENTRY = true;
        public boolean ALLOW_MOUNT_ENTITY = true;
        public boolean ALLOW_DISMOUNT_ENTITY = true;
        public boolean ALLOW_FOREIGN_FLUID_INFLOW = true;
        public boolean ALLOW_FOREIGN_PISTON_PUSH = true;

        public BlockSet BLOCK_PLACE_WHITELIST;
        public BlockSet BLOCK_BREAK_WHITELIST;
        public BlockSet BLOCK_INTERACT_WHITELIST;
        public Set<Item> ITEM_USE_WHITELIST;

        public BlockSet BLOCK_PLACE_BLACKLIST;
        public BlockSet BLOCK_BREAK_BLACKLIST;
        public BlockSet BLOCK_INTERACT_BLACKLIST;
        public Set<Item> ITEM_USE_BLACKLIST;

        private String[] BLOCK_PLACE_BLACKLIST_IDS = new String[]{};
        private String[] BLOCK_BREAK_BLACKLIST_IDS = new String[]{};
        private String[] BLOCK_INTERACT_BLACKLIST_IDS = new String[]{};
        private String[] ITEM_USE_BLACKLIST_IDS = new String[]{};

        private String[] BLOCK_PLACE_WHITELIST_IDS = new String[]{"minecraft:torch"};
        private String[] BLOCK_BREAK_WHITELIST_IDS = new String[]{"minecraft:torch", "warforge:siegecampblock"};
        private String[] BLOCK_INTERACT_WHITELIST_IDS = new String[]{"minecraft:ender_chest", "warforge:citadelblock", "warforge:basicclaimblock", "warforge:reinforcedclaimblock", "warforge:siegecampblock"};
        private String[] ITEM_USE_WHITELIST_IDS = new String[]{"minecraft:snowball"};

        public final MineTime mineTime = new MineTime();
        private boolean MINETIME_ENABLED = false;
        private String MINETIME_MODE = "MULTIPLIER";
        private double MINETIME_VALUE = 5.0;
        private String[] MINETIME_WHITELIST_IDS = new String[]{};
        private String[] MINETIME_BLACKLIST_IDS = new String[]{};

        private ModConfigSpec.BooleanValue BREAK_BLOCKS_V;
        private ModConfigSpec.BooleanValue PLACE_BLOCKS_V;
        private ModConfigSpec.BooleanValue BLOCK_REMOVAL_V;
        private ModConfigSpec.BooleanValue EXPLOSION_DAMAGE_V;
        private ModConfigSpec.BooleanValue INTERACT_V;
        private ModConfigSpec.BooleanValue USE_ITEM_V;
        private ModConfigSpec.BooleanValue PLAYER_TAKE_DAMAGE_FROM_MOB_V;
        private ModConfigSpec.BooleanValue PLAYER_TAKE_DAMAGE_FROM_PLAYER_V;
        private ModConfigSpec.BooleanValue PLAYER_TAKE_DAMAGE_FROM_OTHER_V;
        private ModConfigSpec.BooleanValue PLAYER_DEAL_DAMAGE_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_PLACE_WHITELIST_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_BREAK_WHITELIST_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_INTERACT_WHITELIST_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> ITEM_USE_WHITELIST_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_PLACE_BLACKLIST_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_BREAK_BLACKLIST_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_INTERACT_BLACKLIST_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> ITEM_USE_BLACKLIST_V;
        private ModConfigSpec.BooleanValue ALLOW_MOB_SPAWNS_V;
        private ModConfigSpec.BooleanValue ALLOW_MOB_ENTRY_V;
        private ModConfigSpec.BooleanValue ALLOW_DISMOUNT_ENTITY_V;
        private ModConfigSpec.BooleanValue ALLOW_FOREIGN_FLUID_INFLOW_V;
        private ModConfigSpec.BooleanValue ALLOW_FOREIGN_PISTON_PUSH_V;
        private ModConfigSpec.BooleanValue ALLOW_MOUNT_ENTITY_V;
        private ModConfigSpec.BooleanValue MINETIME_ENABLED_V;
        private ModConfigSpec.ConfigValue<String> MINETIME_MODE_V;
        private ModConfigSpec.DoubleValue MINETIME_VALUE_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> MINETIME_WHITELIST_V;
        private ModConfigSpec.ConfigValue<List<? extends String>> MINETIME_BLACKLIST_V;

        private static BlockSet findBlocks(String[] input) {
            Set<Block> blocks = new HashSet<>(input.length);
            List<TagKey<Block>> tags = new ArrayList<>();
            for (String entry : input) {
                if (entry.startsWith("#")) {
                    ResourceLocation tagId = Ids.tryParse(entry.substring(1));
                    if (tagId == null) {
                        WarForgeMod.LOGGER.warn("Invalid block tag in config: {}", entry);
                    } else {
                        tags.add(TagKey.create(Registries.BLOCK, tagId));
                    }
                    continue;
                }
                ResourceLocation rl = Ids.parse(entry);
                if (BuiltInRegistries.BLOCK.containsKey(rl)) {
                    blocks.add(Regs.get(BuiltInRegistries.BLOCK, rl));
                } else {
                    WarForgeMod.LOGGER.warn("Unknown block ID in config: {}", entry);
                }
            }
            return new BlockSet(blocks, List.copyOf(tags));
        }

        private static Set<Item> findItems(String[] input) {
            Set<Item> output = new HashSet<>(input.length);
            for (String itemID : input) {
                ResourceLocation rl = Ids.parse(itemID);
                if (BuiltInRegistries.ITEM.containsKey(rl)) {
                    output.add(Regs.get(BuiltInRegistries.ITEM, rl));
                } else {
                    WarForgeMod.LOGGER.warn("Unknown item ID in config: {}", itemID);
                }
            }
            return output;
        }

        public void findBlocks() {
            BLOCK_PLACE_WHITELIST = findBlocks(BLOCK_PLACE_WHITELIST_IDS);
            BLOCK_BREAK_WHITELIST = findBlocks(BLOCK_BREAK_WHITELIST_IDS);
            BLOCK_INTERACT_WHITELIST = findBlocks(BLOCK_INTERACT_WHITELIST_IDS);
            ITEM_USE_WHITELIST = findItems(ITEM_USE_WHITELIST_IDS);

            BLOCK_PLACE_BLACKLIST = findBlocks(BLOCK_PLACE_BLACKLIST_IDS);
            BLOCK_BREAK_BLACKLIST = findBlocks(BLOCK_BREAK_BLACKLIST_IDS);
            BLOCK_INTERACT_BLACKLIST = findBlocks(BLOCK_INTERACT_BLACKLIST_IDS);
            ITEM_USE_BLACKLIST = findItems(ITEM_USE_BLACKLIST_IDS);
        }

        public void define(ModConfigSpec.Builder cfg, String name, String desc) {
            cfg.push(name);

            BREAK_BLOCKS_V = cfg.comment("Can players break blocks in " + desc).define(name + " - Break Blocks", BREAK_BLOCKS);
            PLACE_BLOCKS_V = cfg.comment("Can players place blocks in " + desc).define(name + " - Place Blocks", PLACE_BLOCKS);
            BLOCK_REMOVAL_V = cfg.comment("Can blocks be removed at all in (including from explosions, mobs etc) " + desc).define(name + " - Block Removal", BLOCK_REMOVAL);
            EXPLOSION_DAMAGE_V = cfg.comment("Can explosions damage blocks in " + desc).define(name + " - Explosion Damage", EXPLOSION_DAMAGE);
            INTERACT_V = cfg.comment("Can players interact with blocks and entities in " + desc).define(name + " - Interact", INTERACT);
            USE_ITEM_V = cfg.comment("Can players use items in " + desc).define(name + " - Use Items", USE_ITEM);
            PLAYER_TAKE_DAMAGE_FROM_MOB_V = cfg.comment("Can players take mob damage in " + desc).define(name + " - Take Dmg From Mob", PLAYER_TAKE_DAMAGE_FROM_MOB);
            PLAYER_TAKE_DAMAGE_FROM_PLAYER_V = cfg.comment("Can players take damage from other players in " + desc).define(name + " - Take Dmg From Player", PLAYER_TAKE_DAMAGE_FROM_PLAYER);
            PLAYER_TAKE_DAMAGE_FROM_OTHER_V = cfg.comment("Can players take damage from any other source in " + desc).define(name + " - Take Any Other Dmg", PLAYER_TAKE_DAMAGE_FROM_OTHER);
            PLAYER_DEAL_DAMAGE_V = cfg.comment("Can players deal damage in " + desc).define(name + " - Deal Damage", PLAYER_DEAL_DAMAGE);

            BLOCK_PLACE_WHITELIST_V = cfg.comment("Whitelist: block IDs that can still be placed. Has no effect if block placement is allowed anyway")
                    .defineList(name + " - Place Whitelist", asList(BLOCK_PLACE_WHITELIST_IDS), o -> o instanceof String);
            BLOCK_BREAK_WHITELIST_V = cfg.comment("Whitelist: block IDs that can still be broken. Has no effect if block breaking is allowed anyway")
                    .defineList(name + " - Break Whitelist", asList(BLOCK_BREAK_WHITELIST_IDS), o -> o instanceof String);
            BLOCK_INTERACT_WHITELIST_V = cfg.comment("Whitelist: block IDs that can still be interacted with. Has no effect if interacting is allowed anyway")
                    .defineList(name + " - Interact Whitelist", asList(BLOCK_INTERACT_WHITELIST_IDS), o -> o instanceof String);
            ITEM_USE_WHITELIST_V = cfg.comment("Whitelist: item IDs that can still be used. Has no effect if using items is allowed anyway")
                    .defineList(name + " - Use Whitelist", asList(ITEM_USE_WHITELIST_IDS), o -> o instanceof String);

            BLOCK_PLACE_BLACKLIST_V = cfg.comment("Blacklist: block IDs that can never be placed, even if placement is otherwise allowed")
                    .defineList(name + " - Place Blacklist", asList(BLOCK_PLACE_BLACKLIST_IDS), o -> o instanceof String);
            BLOCK_BREAK_BLACKLIST_V = cfg.comment("Blacklist: block IDs that can never be broken, even if breaking is otherwise allowed")
                    .defineList(name + " - Break Blacklist", asList(BLOCK_BREAK_BLACKLIST_IDS), o -> o instanceof String);
            BLOCK_INTERACT_BLACKLIST_V = cfg.comment("Blacklist: block IDs that can never be interacted with, even if interaction is otherwise allowed")
                    .defineList(name + " - Interact Blacklist", asList(BLOCK_INTERACT_BLACKLIST_IDS), o -> o instanceof String);
            ITEM_USE_BLACKLIST_V = cfg.comment("Blacklist: item IDs that can never be used, even if using items is otherwise allowed")
                    .defineList(name + " - Use Blacklist", asList(ITEM_USE_BLACKLIST_IDS), o -> o instanceof String);

            ALLOW_MOB_SPAWNS_V = cfg.comment("Can mobs spawn in " + desc).define(name + " - Allow Mob Spawns", ALLOW_MOB_SPAWNS);
            ALLOW_MOB_ENTRY_V = cfg.comment("Can mobs enter " + desc).define(name + " - Allow Mob Entry", ALLOW_MOB_ENTRY);

            ALLOW_DISMOUNT_ENTITY_V = cfg.comment("Can players dismount entities " + desc).define(name + " - Allow Dismount Entity", ALLOW_DISMOUNT_ENTITY);
            ALLOW_FOREIGN_FLUID_INFLOW_V = cfg.comment("Can fluid flow into " + desc + " from a chunk of another owner (allies and siege zones follow the same profile rules as players)").define(name + " - Allow Foreign Fluid Inflow", ALLOW_FOREIGN_FLUID_INFLOW);
            ALLOW_FOREIGN_PISTON_PUSH_V = cfg.comment("Can a piston in a chunk of another owner push blocks into or out of " + desc).define(name + " - Allow Foreign Piston Push", ALLOW_FOREIGN_PISTON_PUSH);
            ALLOW_MOUNT_ENTITY_V = cfg.comment("Can players mount entities " + desc).define(name + " - Allow Mount Entity", ALLOW_MOUNT_ENTITY);

            cfg.push("MineTime");
            MINETIME_ENABLED_V = cfg.comment("If enabled, breaking a block this profile would normally block is slowed down rather than cancelled in " + desc + ". Whitelisted per-block values below still apply even when this is false.").define(name + " - MineTime Enabled", MINETIME_ENABLED);
            MINETIME_MODE_V = cfg.comment("Default slow-down mode: MULTIPLIER (break time = natural time x value) or FIXED (break time = value seconds).").define(name + " - MineTime Default Mode", MINETIME_MODE);
            MINETIME_VALUE_V = cfg.comment("Default value for the chosen mode: a time multiplier for MULTIPLIER, or a break time in seconds for FIXED.").defineInRange(name + " - MineTime Default Value", MINETIME_VALUE, 0.0, 100000.0);
            MINETIME_WHITELIST_V = cfg.comment(
                            "Blocks that MineTime always slows in this profile (even when disabled above), optionally with a per-entry override. " +
                            "Each entry is a pattern, optionally followed by '=' and a value spec. " +
                            "Patterns: exact id ('gregtech:steam_macerator'), '*' globs ('gregtech:*', 'minecraft:*_ore') or block tags ('#forge:ores'). " +
                            "Value specs: 'x10' or '10' = 10x time, '30s' = fixed 30 seconds; omit to use the default mode/value. " +
                            "Example: 'gregtech:*=x20' slows every GregTech block to 20x break time.")
                    .defineList(name + " - MineTime Whitelist", asList(MINETIME_WHITELIST_IDS), o -> o instanceof String);
            MINETIME_BLACKLIST_V = cfg.comment("Blocks excluded from MineTime in this profile (they keep full, uncancellable protection when the system is enabled). Same pattern syntax as the whitelist; no value spec. Whitelist entries win over the blacklist.")
                    .defineList(name + " - MineTime Blacklist", asList(MINETIME_BLACKLIST_IDS), o -> o instanceof String);
            cfg.pop();

            cfg.pop();
        }

        private void bake(Source src) {
            BREAK_BLOCKS = src.bool(BREAK_BLOCKS_V);
            PLACE_BLOCKS = src.bool(PLACE_BLOCKS_V);
            BLOCK_REMOVAL = src.bool(BLOCK_REMOVAL_V);
            EXPLOSION_DAMAGE = src.bool(EXPLOSION_DAMAGE_V);
            INTERACT = src.bool(INTERACT_V);
            USE_ITEM = src.bool(USE_ITEM_V);
            PLAYER_TAKE_DAMAGE_FROM_MOB = src.bool(PLAYER_TAKE_DAMAGE_FROM_MOB_V);
            PLAYER_TAKE_DAMAGE_FROM_PLAYER = src.bool(PLAYER_TAKE_DAMAGE_FROM_PLAYER_V);
            PLAYER_TAKE_DAMAGE_FROM_OTHER = src.bool(PLAYER_TAKE_DAMAGE_FROM_OTHER_V);
            PLAYER_DEAL_DAMAGE = src.bool(PLAYER_DEAL_DAMAGE_V);

            BLOCK_PLACE_WHITELIST_IDS = toStringArray(src.strings(BLOCK_PLACE_WHITELIST_V));
            BLOCK_BREAK_WHITELIST_IDS = toStringArray(src.strings(BLOCK_BREAK_WHITELIST_V));
            BLOCK_INTERACT_WHITELIST_IDS = toStringArray(src.strings(BLOCK_INTERACT_WHITELIST_V));
            ITEM_USE_WHITELIST_IDS = toStringArray(src.strings(ITEM_USE_WHITELIST_V));

            BLOCK_PLACE_BLACKLIST_IDS = toStringArray(src.strings(BLOCK_PLACE_BLACKLIST_V));
            BLOCK_BREAK_BLACKLIST_IDS = toStringArray(src.strings(BLOCK_BREAK_BLACKLIST_V));
            BLOCK_INTERACT_BLACKLIST_IDS = toStringArray(src.strings(BLOCK_INTERACT_BLACKLIST_V));
            ITEM_USE_BLACKLIST_IDS = toStringArray(src.strings(ITEM_USE_BLACKLIST_V));

            ALLOW_MOB_SPAWNS = src.bool(ALLOW_MOB_SPAWNS_V);
            ALLOW_MOB_ENTRY = src.bool(ALLOW_MOB_ENTRY_V);
            ALLOW_DISMOUNT_ENTITY = src.bool(ALLOW_DISMOUNT_ENTITY_V);
            ALLOW_FOREIGN_FLUID_INFLOW = src.bool(ALLOW_FOREIGN_FLUID_INFLOW_V);
            ALLOW_FOREIGN_PISTON_PUSH = src.bool(ALLOW_FOREIGN_PISTON_PUSH_V);
            ALLOW_MOUNT_ENTITY = src.bool(ALLOW_MOUNT_ENTITY_V);

            MINETIME_ENABLED = src.bool(MINETIME_ENABLED_V);
            MINETIME_MODE = src.string(MINETIME_MODE_V);
            MINETIME_VALUE = src.dbl(MINETIME_VALUE_V);
            MINETIME_WHITELIST_IDS = toStringArray(src.strings(MINETIME_WHITELIST_V));
            MINETIME_BLACKLIST_IDS = toStringArray(src.strings(MINETIME_BLACKLIST_V));
            mineTime.configure(MINETIME_ENABLED, MINETIME_MODE, MINETIME_VALUE, MINETIME_WHITELIST_IDS, MINETIME_BLACKLIST_IDS);
        }
    }
}
