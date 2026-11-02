package com.flansmod.warforge.client;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.Locale;

public final class SiegeUiDebugCommand {
    private static final String[] SCENARIOS = {"attacker", "defender", "even", "blowout", "wide", "abandon", "default"};
    private static final long DEBUG_DURATION_MS = 90_000L;
    private static final int ANIMATION_STEP_TICKS = 8;
    private static final int NO_OVERRIDE = -1;

    private static boolean enabled;
    private static boolean animate;
    private static SiegeCampProgressInfo debugSiege;
    private static int defenceOverride = NO_OVERRIDE;
    private static int animTicks;
    private static int animDir = 1;

    public static boolean isEnabled() {
        return enabled;
    }

    public static int defenceThreshold() {
        return enabled && defenceOverride != NO_OVERRIDE ? defenceOverride : WarForgeConfig.SIEGE_DEFENCE_THRESHOLD;
    }

    public static SiegeCampProgressInfo debugSiege() {
        if (debugSiege == null) {
            debugSiege = newDebugSiege();
            debugSiege.endTimestamp = System.currentTimeMillis() + DEBUG_DURATION_MS;
        }
        return debugSiege;
    }

    public static void reset() {
        enabled = false;
        animate = false;
        debugSiege = null;
        defenceOverride = NO_OVERRIDE;
    }

    public static void tick() {
        if (!enabled || !animate) {
            return;
        }
        SiegeCampProgressInfo dbg = debugSiege();
        int defence = Math.max(1, defenceThreshold());

        if (++animTicks >= ANIMATION_STEP_TICKS) {
            animTicks = 0;
            dbg.previousProgress = dbg.progress;
            dbg.progress += animDir;
            if (dbg.progress >= dbg.completionPoint) {
                dbg.progress = dbg.completionPoint;
                animDir = -1;
            } else if (dbg.progress <= -defence) {
                dbg.progress = -defence;
                animDir = 1;
            }
        }

        if (dbg.endTimestamp - System.currentTimeMillis() < 0) {
            dbg.endTimestamp = System.currentTimeMillis() + DEBUG_DURATION_MS;
        }
    }

    private static SiegeCampProgressInfo newDebugSiege() {
        SiegeCampProgressInfo info = new SiegeCampProgressInfo();
        info.defendingPos = new DimBlockPos(Level.OVERWORLD, 100, 64, 100);
        info.attackingPos = new DimBlockPos(Level.OVERWORLD, 120, 64, 100);
        info.attackingColour = 0xFF0000;
        info.defendingColour = 0x0000FF;
        info.attackingName = "Red Team";
        info.defendingName = "Blue Team";
        info.battleRadius = 2;
        info.completionPoint = 10;
        info.progress = 6;
        info.previousProgress = 4;
        info.expiredTicks = 20;
        info.timeProgress = 900;
        return info;
    }

    private static void applyScenario(String scenario) {
        SiegeCampProgressInfo dbg = newDebugSiege();
        dbg.endTimestamp = System.currentTimeMillis() + DEBUG_DURATION_MS;
        switch (scenario.toLowerCase(Locale.ROOT)) {
            case "attacker" -> setProgress(dbg, 10, 6, 8);
            case "defender" -> setProgress(dbg, 10, -1, -3);
            case "even" -> setProgress(dbg, 10, 0, 0);
            case "blowout" -> setProgress(dbg, 20, 18, 19);
            case "wide" -> setProgress(dbg, 30, 10, 12);
            case "abandon" -> {
                setProgress(dbg, 10, 4, 5);
                dbg.attackerAbandonSeconds = 30;
                dbg.attackingFactionId = ClientClaimChunkCache.playerFactionId();
            }
            default -> setProgress(dbg, 10, 4, 6);
        }
        debugSiege = dbg;
    }

    private static void setProgress(SiegeCampProgressInfo dbg, int completionPoint, int previous, int progress) {
        dbg.completionPoint = completionPoint;
        dbg.previousProgress = previous;
        dbg.progress = progress;
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("warforgesiegeui");
        root.executes(ctx -> feedback(ctx, "Siege UI debug: /warforgesiegeui <on|off|toggle|animate|scenario|progress|goal|defence|abandon|reset>"));

        root.then(Commands.literal("on").executes(ctx -> setEnabled(ctx, true)));
        root.then(Commands.literal("off").executes(ctx -> setEnabled(ctx, false)));
        root.then(Commands.literal("toggle").executes(ctx -> setEnabled(ctx, !enabled)));

        root.then(Commands.literal("animate").executes(ctx -> {
            enable();
            animate = !animate;
            return feedback(ctx, "Siege UI animate: " + (animate ? "on" : "off"));
        }));

        root.then(Commands.literal("reset").executes(ctx -> {
            debugSiege = null;
            animate = false;
            defenceOverride = NO_OVERRIDE;
            return feedback(ctx, "Siege UI debug reset to defaults");
        }));

        root.then(Commands.literal("scenario")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SCENARIOS, b))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            enabled = true;
                            animate = false;
                            applyScenario(name);
                            return feedback(ctx, "Siege UI scenario: " + name);
                        })));

        root.then(intOption("progress", (dbg, v) -> {
            dbg.previousProgress = dbg.progress;
            dbg.progress = v;
        }));
        root.then(intOption("goal", (dbg, v) -> dbg.completionPoint = Math.max(1, v)));

        root.then(Commands.literal("defence")
                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            defenceOverride = IntegerArgumentType.getInteger(ctx, "value");
                            enable();
                            return feedback(ctx, "Siege defence threshold (debug display) = " + defenceOverride);
                        })));

        root.then(Commands.literal("abandon")
                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            enable();
                            SiegeCampProgressInfo dbg = debugSiege();
                            dbg.attackerAbandonSeconds = IntegerArgumentType.getInteger(ctx, "seconds");
                            dbg.attackingFactionId = ClientClaimChunkCache.playerFactionId();
                            return feedback(ctx, "Siege abandon seconds = " + dbg.attackerAbandonSeconds);
                        })));

        dispatcher.register(root);
    }

    private interface DebugSetter {
        void apply(SiegeCampProgressInfo dbg, int value);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intOption(String name, DebugSetter setter) {
        return Commands.literal(name)
                .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(ctx -> {
                            int value = IntegerArgumentType.getInteger(ctx, "value");
                            enable();
                            setter.apply(debugSiege(), value);
                            return feedback(ctx, "Siege UI " + name + " = " + value);
                        }));
    }

    private static void enable() {
        enabled = true;
        debugSiege();
    }

    private static int setEnabled(CommandContext<CommandSourceStack> ctx, boolean on) {
        if (on) {
            enable();
        } else {
            enabled = false;
            animate = false;
        }
        return feedback(ctx, "Siege UI debug: " + (on ? "on" : "off"));
    }

    private static int feedback(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return Command.SINGLE_SUCCESS;
    }
}
