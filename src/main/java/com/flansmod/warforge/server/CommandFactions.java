package com.flansmod.warforge.server;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.LeaderboardInfo;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PacketLeaderboardInfo;
import com.flansmod.warforge.common.network.PacketToggleBorders;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.TimeHelper;
import com.flansmod.warforge.compat.Chunks;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import com.flansmod.warforge.compat.Perms;
import com.flansmod.warforge.compat.PlayerRef;
import com.flansmod.warforge.compat.Profiles;
import com.flansmod.warforge.server.Leaderboard.FactionStat;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

public class CommandFactions {
    private static boolean isOp(CommandSourceStack src) {
        return Perms.isGameMaster(src);
    }

    private static final SuggestionProvider<CommandSourceStack> FACTION_NAME_SUGGESTIONS = (ctx, builder) -> {
        List<String> names = new ArrayList<>();
        for (Faction faction : WarForgeMod.FACTIONS.registry.getAllFactions()) {
            if (!FactionIds.isNeutralZone(faction.getUuid())) {
                names.add(faction.getName());
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final List<String> HELP_LINES = List.of(
            "/f invite <playerName>", "/f accept [factionName]", "/f disband", "/f expel <playerName>", "/f leave",
            "/f time", "/f info <factionName>", "/f list", "/f top", "/f wealth", "/f legacy", "/f notoriety", "/f borders",
            "/f tpa <playerName>", "/f tpaccept | tpdeny [playerName]", "/f ally chat <message>", "/f vault redeem");
    private static final List<String> OP_HELP_LINES = List.of(
            "/f offlineprotection <faction> <enable|disable|status>", "/f zone <safe|war|remove>",
            "/f rename <oldFactionName> <newFactionName>", "/f flag <factionName> <set <flagId> | reset>",
            "/f vein <info|set <vein> [quality]|clear|reroll> [at <chunkX> <chunkZ> [dim] [radius]]", "/f reloadConfig");

    private static final SuggestionProvider<CommandSourceStack> FLAG_ID_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(WarForgeMod.FLAG_REGISTRY.getAvailableFlagIds(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(buildTree("faction"));
        dispatcher.register(Commands.literal("f").redirect(root));
        dispatcher.register(Commands.literal("factions").redirect(root));
        dispatcher.register(Commands.literal("war").redirect(root));
        dispatcher.register(Commands.literal("warforge").redirect(root));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTree(String name) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name);
        root.executes(ctx -> {
            ctx.getSource().sendSuccess(() -> t("cmd.try_help"), false);
            return Command.SINGLE_SUCCESS;
        });

        root.then(Commands.literal("help").executes(CommandFactions::doHelp));
        root.then(Commands.literal("create").executes(CommandFactions::doCreate));

        root.then(Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> doInvite(ctx, false))
                        .then(Commands.argument("faction", StringArgumentType.string())
                                .suggests(FACTION_NAME_SUGGESTIONS)
                                .requires(CommandFactions::isOp)
                                .executes(ctx -> doInvite(ctx, true)))));

        root.then(Commands.literal("accept")
                .executes(ctx -> doAccept(ctx, false))
                .then(Commands.argument("faction", StringArgumentType.string())
                        .suggests(FACTION_NAME_SUGGESTIONS)
                        .executes(ctx -> doAccept(ctx, true))));

        root.then(Commands.literal("disband")
                .executes(CommandFactions::doDisbandSelf)
                .then(Commands.argument("faction", StringArgumentType.string())
                        .suggests(FACTION_NAME_SUGGESTIONS)
                        .requires(CommandFactions::isOp)
                        .executes(CommandFactions::doDisbandNamed)));

        root.then(Commands.literal("expel")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> doRemove(ctx))));
        root.then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> doRemove(ctx))));

        root.then(Commands.literal("leave").executes(CommandFactions::doLeave));
        root.then(Commands.literal("exit").executes(CommandFactions::doLeave));

        root.then(Commands.literal("setleader")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(CommandFactions::doSetLeader)));

        root.then(Commands.literal("time").executes(CommandFactions::doTime));

        root.then(Commands.literal("conquered")
                .requires(CommandFactions::isOp)
                .then(Commands.literal("clear").executes(CommandFactions::doConqueredClear))
                .then(Commands.literal("set")
                        .then(Commands.argument("time", StringArgumentType.word())
                                .executes(CommandFactions::doConqueredSet))));

        root.then(Commands.literal("info")
                .executes(ctx -> doInfo(ctx, false))
                .then(Commands.argument("faction", StringArgumentType.string())
                        .suggests(FACTION_NAME_SUGGESTIONS)
                        .executes(ctx -> doInfo(ctx, true))));

        root.then(Commands.literal("list").executes(CommandFactions::doList));

        for (String alias : new String[]{"top"}) {
            root.then(Commands.literal(alias).executes(ctx -> doLeaderboard(ctx, FactionStat.TOTAL, "**Top Leaderboard**")));
        }
        for (String alias : new String[]{"wealth", "wealthtop", "bal", "baltop"}) {
            root.then(Commands.literal(alias).executes(ctx -> doLeaderboard(ctx, FactionStat.WEALTH, "**Top Leaderboard**")));
        }
        for (String alias : new String[]{"notoriety", "notorietytop", "pvp", "pvptop"}) {
            root.then(Commands.literal(alias).executes(ctx -> doLeaderboard(ctx, FactionStat.NOTORIETY, "**Notoriety Leaderboard**")));
        }
        for (String alias : new String[]{"legacy", "legacytop", "playtime", "playtimetop"}) {
            root.then(Commands.literal(alias).executes(ctx -> doLeaderboard(ctx, FactionStat.LEGACY, "**Legacy Leaderboard**")));
        }

        for (String alias : new String[]{"zone", "zones"}) {
            root.then(Commands.literal(alias).requires(CommandFactions::isOp)
                    .executes(CommandFactions::doZoneUsage)
                    .then(Commands.literal("safe").executes(ctx -> doZoneClaim(ctx, FactionIds.SAFE_ZONE)))
                    .then(Commands.literal("war").executes(ctx -> doZoneClaim(ctx, FactionIds.WAR_ZONE)))
                    .then(Commands.literal("remove").executes(CommandFactions::doZoneUnclaim)));
        }

        for (String alias : new String[]{"opProtection", "protection", "protectionOverride"}) {
            root.then(Commands.literal(alias).requires(CommandFactions::isOp)
                    .executes(CommandFactions::doProtectionToggle));
        }

        for (String alias : new String[]{"siege", "sieges"}) {
            root.then(Commands.literal(alias).requires(CommandFactions::isOp)
                    .executes(CommandFactions::doSiegeUsage)
                    .then(Commands.literal("list").executes(CommandFactions::doSiegeList))
                    .then(Commands.literal("l").executes(CommandFactions::doSiegeList))
                    .then(Commands.literal("terminate").then(siegeTerminateArgs()))
                    .then(Commands.literal("t").then(siegeTerminateArgs())));
        }

        root.then(Commands.literal("home").executes(CommandFactions::doHome));
        root.then(Commands.literal("spawn").executes(CommandFactions::doSpawn));

        root.then(Commands.literal("promote")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> doPromoteDemote(ctx, true))));
        root.then(Commands.literal("demote")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> doPromoteDemote(ctx, false))));

        root.then(Commands.literal("vault")
                .executes(CommandFactions::doVaultUsage)
                .then(Commands.literal("redeem").executes(CommandFactions::doVaultRedeem)));

        root.then(Commands.literal("offlineprotection").requires(CommandFactions::isOp)
                .executes(CommandFactions::doOfflineProtectionUsage)
                .then(Commands.argument("faction", StringArgumentType.string())
                        .suggests(FACTION_NAME_SUGGESTIONS)
                        .executes(CommandFactions::doOfflineProtectionUsage)
                        .then(Commands.literal("enable").executes(ctx -> doOfflineProtection(ctx, "enable")))
                        .then(Commands.literal("disable").executes(ctx -> doOfflineProtection(ctx, "disable")))
                        .then(Commands.literal("status").executes(ctx -> doOfflineProtection(ctx, "status")))));

        root.then(Commands.literal("flag").requires(CommandFactions::isOp)
                .executes(CommandFactions::doFlagUsage)
                .then(Commands.argument("faction", StringArgumentType.string())
                        .suggests(FACTION_NAME_SUGGESTIONS)
                        .executes(CommandFactions::doFlagUsage)
                        .then(Commands.literal("reset").executes(CommandFactions::doFlagReset))
                        .then(Commands.literal("set")
                                .then(Commands.argument("flagId", StringArgumentType.greedyString())
                                        .suggests(FLAG_ID_SUGGESTIONS)
                                        .executes(CommandFactions::doFlagSet)))));

        root.then(Commands.literal("rename").requires(CommandFactions::isOp)
                .executes(CommandFactions::doRenameUsage)
                .then(Commands.argument("oldFactionName", StringArgumentType.string())
                        .suggests(FACTION_NAME_SUGGESTIONS)
                        .executes(CommandFactions::doRenameUsage)
                        .then(Commands.argument("newFactionName", StringArgumentType.string())
                                .executes(CommandFactions::doRename))));

        for (String alias : new String[]{"reloadConfig", "reloadconfig", "reload"}) {
            root.then(Commands.literal(alias).requires(CommandFactions::isOp)
                    .executes(CommandFactions::doReloadConfig));
        }

        root.then(Commands.literal("clearnotoriety").requires(CommandFactions::isOp)
                .executes(ctx -> { WarForgeMod.FACTIONS.registry.clearNotoriety(); return Command.SINGLE_SUCCESS; }));
        root.then(Commands.literal("clearlegacy").requires(CommandFactions::isOp)
                .executes(ctx -> { WarForgeMod.FACTIONS.registry.clearLegacy(); return Command.SINGLE_SUCCESS; }));

        root.then(Commands.literal("chat")
                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(CommandFactions::doMsg)));
        root.then(Commands.literal("msg")
                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(CommandFactions::doMsg)));

        root.then(Commands.literal("ally")
                .then(Commands.literal("chat")
                        .then(Commands.argument("message", StringArgumentType.greedyString()).executes(CommandFactions::doAllyMsg)))
                .then(Commands.literal("msg")
                        .then(Commands.argument("message", StringArgumentType.greedyString()).executes(CommandFactions::doAllyMsg))));
        root.then(Commands.literal("allychat")
                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(CommandFactions::doAllyMsg)));
        root.then(Commands.literal("ac")
                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(CommandFactions::doAllyMsg)));

        for (String alias : new String[]{"tpa", "tprequest", "tp"}) {
            root.then(Commands.literal(alias)
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(CommandFactions::doTpaRequest)));
        }
        root.then(Commands.literal("tpaccept")
                .executes(ctx -> doTpaResolve(ctx, true, false))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> doTpaResolve(ctx, true, true))));
        root.then(Commands.literal("tpdeny")
                .executes(ctx -> doTpaResolve(ctx, false, false))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> doTpaResolve(ctx, false, true))));

        root.then(Commands.literal("borders").executes(CommandFactions::doBorders));

        root.then(Commands.literal("debugmsg").requires(CommandFactions::isOp)
                .then(Commands.argument("title", StringArgumentType.string())
                        .then(Commands.argument("subtitle", StringArgumentType.string())
                                .then(Commands.argument("color", StringArgumentType.string())
                                        .executes(CommandFactions::doDebugMsg)))));

        root.then(buildVeinTree());
        root.then(buildFobTree());

        return root;
    }

    private static FactionStorage factions() {
        return WarForgeMod.FACTIONS;
    }

    private static int ok() {
        return Command.SINGLE_SUCCESS;
    }

    private static Faction factionArg(CommandContext<CommandSourceStack> ctx, String arg) {
        Faction faction = factions().registry.findByName(StringArgumentType.getString(ctx, arg));
        if (faction == null) {
            ctx.getSource().sendFailure(t("cmd.faction_not_found"));
        }
        return faction;
    }

    private static int doHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        for (String line : HELP_LINES) {
            src.sendSuccess(() -> Component.literal(line), false);
        }
        if (isOp(src)) {
            for (String line : OP_HELP_LINES) {
                src.sendSuccess(() -> Component.literal(line), false);
            }
        }
        return ok();
    }

    private static int doCreate(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> t("cmd.create_hint"), false);
        return ok();
    }

    private static int doInvite(CommandContext<CommandSourceStack> ctx, boolean withFaction) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer invitee = EntityArgument.getPlayer(ctx, "player");
        if (withFaction) {
            Faction faction = factionArg(ctx, "faction");
            if (faction != null) {
                factions().membership.requestInvite(src, faction.getUuid(), invitee.getUUID());
            }
        } else if (src.getEntity() instanceof ServerPlayer player) {
            factions().membership.requestInvitePlayerToMyFaction(player, invitee.getUUID());
        }
        return ok();
    }

    private static int doAccept(CommandContext<CommandSourceStack> ctx, boolean withFaction) {
        CommandSourceStack src = ctx.getSource();
        if (!(src.getEntity() instanceof Player player)) {
            src.sendFailure(t("cmd.console_cannot_accept"));
        } else if (withFaction) {
            factions().membership.acceptInvite(player, StringArgumentType.getString(ctx, "faction"));
        } else {
            factions().membership.acceptInvite(player);
        }
        return ok();
    }

    private static int doDisbandSelf(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Faction faction = src.getEntity() instanceof Player player ? factions().registry.getFactionOfPlayer(player.getUUID()) : null;
        if (faction == null) {
            src.sendFailure(t("not_in_faction"));
        } else {
            factions().lifecycle.requestDisband((Player) src.getEntity(), faction.getUuid());
        }
        return ok();
    }

    private static int doDisbandNamed(CommandContext<CommandSourceStack> ctx) {
        Faction faction = factionArg(ctx, "faction");
        if (faction != null) {
            factions().lifecycle.defeat(faction);
        }
        return ok();
    }

    private static int doRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        UUID toRemoveId = EntityArgument.getPlayer(ctx, "player").getUUID();
        Faction faction = src.getEntity() instanceof Player player ? factions().registry.getFactionOfPlayer(player.getUUID()) : null;
        if (faction == null) {
            faction = factions().registry.getFactionOfPlayer(toRemoveId);
        }
        if (faction == null) {
            src.sendFailure(t("cmd.player_no_faction"));
        } else {
            factions().membership.requestRemovePlayer(src, faction.getUuid(), toRemoveId);
        }
        return ok();
    }

    private static int doLeave(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!(src.getEntity() instanceof Player player)) {
            src.sendFailure(t("cmd.players_only"));
            return ok();
        }
        Faction faction = factions().registry.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            src.sendFailure(t("not_in_faction"));
        } else {
            factions().membership.requestRemovePlayer(src, faction.getUuid(), player.getUUID());
        }
        return ok();
    }

    private static int doSetLeader(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!(src.getEntity() instanceof Player player)) {
            src.sendFailure(t("cmd.players_only"));
            return ok();
        }
        String name = StringArgumentType.getString(ctx, "name");
        PlayerRef profile = Profiles.get(src.getServer(), name).orElse(null);
        if (profile == null) {
            src.sendFailure(t("cmd.player_not_found", name));
            return ok();
        }
        Faction target = factions().registry.getFactionOfPlayer(profile.getId());
        if (target == null) {
            src.sendFailure(t("cmd.target_no_faction"));
        } else {
            factions().membership.requestTransferLeadership(player, target.getUuid(), profile.getId());
        }
        return ok();
    }

    private static int doConqueredClear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        DimChunkPos chunk = new DimChunkPos(player.level().dimension(), player.blockPosition());
        if (factions().conquered.clear(chunk)) {
            ctx.getSource().sendSuccess(() -> t("cmd.conquered_cleared", chunk.x, chunk.z), true);
        } else {
            ctx.getSource().sendFailure(t("cmd.not_conquered"));
        }
        return ok();
    }

    private static int doConqueredSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String timeArg = StringArgumentType.getString(ctx, "time");
        long ms = TimeHelper.parseDurationMs(timeArg);
        if (ms <= 0 || ms > Integer.MAX_VALUE) {
            ctx.getSource().sendFailure(t("cmd.bad_time", timeArg));
            return 0;
        }
        DimChunkPos chunk = new DimChunkPos(player.level().dimension(), player.blockPosition());
        Faction faction = factions().registry.getFactionOfPlayer(player.getUUID());
        factions().conquered.set(chunk, faction == null ? FactionIds.NONE : faction.getUuid(), (int) ms);
        ctx.getSource().sendSuccess(() -> t("cmd.conquered_set", chunk.x, chunk.z, TimeHelper.formatTime(ms)), true);
        return ok();
    }

    private static int doTime(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String yields = TimeHelper.formatTime(WarClock.TIME.getTimeToNextYieldMs());
        String sieges = TimeHelper.formatTime(WarClock.TIME.getTimeToNextSiegeAdvanceMs());
        src.sendSuccess(() -> t("cmd.time_yields", yields), false);
        src.sendSuccess(() -> t("cmd.time_sieges", sieges), false);
        return ok();
    }

    private static int doInfo(CommandContext<CommandSourceStack> ctx, boolean withFaction) {
        CommandSourceStack src = ctx.getSource();
        Faction own = src.getEntity() instanceof Player player ? factions().registry.getFactionOfPlayer(player.getUUID()) : null;
        Faction target = withFaction ? factionArg(ctx, "faction") : own;
        if (target == null) {
            if (!withFaction) {
                src.sendFailure(t("cmd.faction_not_found"));
            }
            return ok();
        }
        if (src.getEntity() instanceof ServerPlayer sender && target == own) {
            PacketFactionInfo packet = new PacketFactionInfo();
            packet.info = target.createInfo(sender.getUUID());
            WarForgeMod.NETWORK.sendTo(packet, sender);
            return ok();
        }
        PlayerRef leader = Profiles.get(src.getServer(), target.getLeaderId()).orElse(null);
        Component publicInfo = t("cmd.info_public", target.getName(), leader == null ? "?" : leader.getName(),
                target.getMemberCount(), target.getCitadelLevel(), target.getClaimCount());
        src.sendSuccess(() -> publicInfo, false);
        if (isOp(src)) {
            src.sendSuccess(() -> t("cmd.info_stats", target.getNotoriety(), target.getWealth(), target.getLegacy()), false);
        }
        return ok();
    }

    private static int doList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<Faction> list = new ArrayList<>();
        for (Faction faction : factions().registry.getAllFactions()) {
            if (!FactionIds.isNeutralZone(faction.getUuid())) {
                list.add(faction);
            }
        }
        if (list.isEmpty()) {
            src.sendSuccess(() -> t("cmd.list_empty"), false);
            return ok();
        }
        list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        src.sendSuccess(() -> t("cmd.list_header", list.size()), false);
        for (Faction faction : list) {
            src.sendSuccess(() -> t("cmd.list_entry", faction.getName(), faction.getMemberCount()), false);
        }
        return ok();
    }

    private static int doLeaderboard(CommandContext<CommandSourceStack> ctx, FactionStat stat, String consoleHeader) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayer sender) {
            PacketLeaderboardInfo packet = new PacketLeaderboardInfo();
            packet.info = WarForgeMod.LEADERBOARD.createInfo(0, stat, sender.getUUID());
            WarForgeMod.NETWORK.sendTo(packet, sender);
            return ok();
        }
        LeaderboardInfo info = WarForgeMod.LEADERBOARD.createInfo(0, stat, FactionIds.NONE);
        StringBuilder result = new StringBuilder(consoleHeader);
        for (FactionDisplayInfo facInfo : info.factionInfos) {
            if (facInfo == null) {
                continue;
            }
            switch (stat) {
                case TOTAL -> result.append("\n#").append(facInfo.totalRank).append(" | ").append(facInfo.legacy + facInfo.notoriety + facInfo.wealth);
                case WEALTH -> result.append("\n#").append(facInfo.wealthRank).append(" | ").append(facInfo.wealth);
                case NOTORIETY -> result.append("\n#").append(facInfo.notorietyRank).append(" | ").append(facInfo.notoriety);
                case LEGACY -> result.append("\n#").append(facInfo.legacyRank).append(" | ").append(facInfo.legacy);
            }
        }
        String out = result.toString();
        src.sendSuccess(() -> Component.literal(out), false);
        return ok();
    }

    private static int doZoneUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(t("cmd.zone_usage"));
        return ok();
    }

    private static int doZoneClaim(CommandContext<CommandSourceStack> ctx, UUID zoneId) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof Player player) {
            factions().claimService.requestZoneClaim(player, new DimBlockPos(player).toChunkPos(), zoneId);
            refreshClaimViews(src);
        } else {
            src.sendFailure(t("cmd.op_player_only"));
        }
        return ok();
    }

    private static int doZoneUnclaim(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof Player player) {
            factions().claimService.requestZoneUnclaim(player, new DimBlockPos(player).toChunkPos());
            refreshClaimViews(src);
        } else {
            src.sendFailure(t("cmd.op_player_only"));
        }
        return ok();
    }

    private static int doProtectionToggle(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!(src.getEntity() instanceof Player player)) {
            src.sendFailure(t("cmd.players_only"));
            return ok();
        }
        boolean enabled = ProtectionsModule.toggleOpOverride(player.getUUID());
        src.sendSuccess(() -> t(enabled ? "cmd.protection_on" : "cmd.protection_off"), false);
        return ok();
    }

    private static int doReloadConfig(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        try {
            String status = WarForgeMod.reloadServerConfig();
            src.sendSuccess(() -> t("cmd.reloaded", status), true);
            return ok();
        } catch (RuntimeException e) {
            WarForgeMod.LOGGER.error("Failed to reload WarForge config", e);
            src.sendFailure(t("cmd.reload_failed", String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static int doSiegeUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(t("cmd.siege_usage"));
        return ok();
    }

    private static int doSiegeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> t("cmd.siege_list_header"), false);
        factions().sieges.entries().forEach((chunk, siege) -> {
            Faction attacker = factions().registry.getFaction(siege.attackingFaction);
            Faction defender = factions().registry.getFaction(siege.defendingFaction);
            src.sendSuccess(() -> t("cmd.siege_list_entry", chunk.x, chunk.z, Keys.id(chunk.dim).toString(),
                    attacker == null ? "?" : attacker.getName(), defender == null ? "?" : defender.getName()), false);
        });
        return ok();
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> siegeTerminateArgs() {
        return Commands.argument("chunkX", IntegerArgumentType.integer())
                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                        .then(Commands.argument("dim", StringArgumentType.string())
                                .executes(ctx -> doSiegeTerminate(ctx, false))
                                .then(Commands.argument("conclusion", StringArgumentType.string())
                                        .executes(ctx -> doSiegeTerminate(ctx, true)))));
    }

    private static int doSiegeTerminate(CommandContext<CommandSourceStack> ctx, boolean withConclusion) {
        CommandSourceStack src = ctx.getSource();
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Ids.parse(StringArgumentType.getString(ctx, "dim")));
        DimChunkPos chunk = new DimChunkPos(dim, IntegerArgumentType.getInteger(ctx, "chunkX"), IntegerArgumentType.getInteger(ctx, "chunkZ"));

        SiegeService.SiegeTermination termination = SiegeService.SiegeTermination.NEUTRAL;
        if (withConclusion) {
            String arg = StringArgumentType.getString(ctx, "conclusion");
            try {
                termination = SiegeService.SiegeTermination.valueOf(arg.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                src.sendFailure(t("cmd.siege_bad_conclusion", arg));
                return 0;
            }
        }
        if (factions().sieges.get(chunk) == null) {
            src.sendFailure(t("cmd.siege_missing"));
            return 0;
        }
        factions().siegeService.conclude(chunk, termination, true);
        SiegeService.SiegeTermination applied = termination;
        src.sendSuccess(() -> t("cmd.siege_terminated", chunk.x, chunk.z, Keys.id(chunk.dim).toString(), applied.name()), false);
        return ok();
    }

    private static int doHome(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof Player player) {
            WarForgeMod.TELEPORTS.requestFHome(player);
        } else {
            src.sendFailure(t("cmd.players_only"));
        }
        return ok();
    }

    private static int doSpawn(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof Player player) {
            WarForgeMod.TELEPORTS.requestSpawn(player);
        } else {
            src.sendFailure(t("cmd.players_only"));
        }
        return ok();
    }

    private static int doTpaRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof Player player) {
            WarForgeMod.TELEPORTS.requestTpa(player, EntityArgument.getPlayer(ctx, "player"));
        } else {
            src.sendFailure(t("cmd.players_only"));
        }
        return ok();
    }

    private static int doTpaResolve(CommandContext<CommandSourceStack> ctx, boolean accept, boolean withPlayer) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(t("cmd.players_only"));
            return ok();
        }
        UUID filter = withPlayer ? EntityArgument.getPlayer(ctx, "player").getUUID() : null;
        if (accept) {
            WarForgeMod.TELEPORTS.acceptTpa(player, filter);
        } else {
            WarForgeMod.TELEPORTS.denyTpa(player, filter);
        }
        return ok();
    }

    private static int doPromoteDemote(CommandContext<CommandSourceStack> ctx, boolean promote) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        if (!(src.getEntity() instanceof Player player)) {
            src.sendFailure(t("cmd.players_only"));
            return ok();
        }
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        if (promote) {
            factions().membership.requestPromote(player, target);
        } else {
            factions().membership.requestDemote(player, target);
        }
        return ok();
    }

    private static int doVaultUsage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendFailure(t(src.getEntity() instanceof ServerPlayer ? "cmd.vault_usage" : "cmd.players_only"));
        return ok();
    }

    private static int doVaultRedeem(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayer player) {
            factions().insurance.redeem(player);
        } else {
            src.sendFailure(t("cmd.players_only"));
        }
        return ok();
    }

    private static int doOfflineProtectionUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(t("cmd.offline_usage"));
        return ok();
    }

    private static int doOfflineProtection(CommandContext<CommandSourceStack> ctx, String action) {
        CommandSourceStack src = ctx.getSource();
        Faction target = factionArg(ctx, "faction");
        if (target == null) {
            return ok();
        }
        switch (action) {
            case "enable" -> {
                target.setOfflineRaidProtectionDisabled(false);
                src.sendSuccess(() -> t("cmd.offline_enabled", target.getName()), false);
            }
            case "disable" -> {
                target.setOfflineRaidProtectionDisabled(true);
                src.sendSuccess(() -> t("cmd.offline_disabled", target.getName()), false);
            }
            case "status" -> src.sendSuccess(() -> t("cmd.offline_status", target.getName(),
                    target.isOfflineRaidProtectionDisabled() ? "DISABLED" : "ENABLED",
                    target.countOnlineMembers(null), target.isOfflineRaidProtected()), false);
            default -> src.sendFailure(t("cmd.offline_usage"));
        }
        return ok();
    }

    private static int doFlagUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(t("cmd.flag_usage"));
        return ok();
    }

    private static int doFlagReset(CommandContext<CommandSourceStack> ctx) {
        Faction faction = factionArg(ctx, "faction");
        if (faction != null) {
            factions().lifecycle.adminSetFlag(ctx.getSource(), faction.getUuid(), "");
        }
        return ok();
    }

    private static int doFlagSet(CommandContext<CommandSourceStack> ctx) {
        Faction faction = factionArg(ctx, "faction");
        if (faction != null) {
            factions().lifecycle.adminSetFlag(ctx.getSource(), faction.getUuid(), StringArgumentType.getString(ctx, "flagId").trim());
        }
        return ok();
    }

    private static int doRenameUsage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendFailure(t("cmd.rename_usage"));
        return ok();
    }

    private static int doRename(CommandContext<CommandSourceStack> ctx) {
        Faction faction = factionArg(ctx, "oldFactionName");
        if (faction != null) {
            factions().lifecycle.requestRename(ctx.getSource(), faction.getUuid(), StringArgumentType.getString(ctx, "newFactionName"));
        }
        return ok();
    }

    private static int doMsg(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof Player player) {
            Faction faction = factions().registry.getFactionOfPlayer(player.getUUID());
            if (faction != null) {
                faction.messageAll(Component.literal("§a[" + src.getTextName() + " > Faction]§f " + StringArgumentType.getString(ctx, "message")));
            }
        }
        return ok();
    }

    private static int doAllyMsg(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof Player player) {
            Faction faction = factions().registry.getFactionOfPlayer(player.getUUID());
            if (faction == null) {
                src.sendFailure(t("not_in_faction"));
                return ok();
            }
            factions().alliances.messageAlliance(faction, Component.literal("§b[" + src.getTextName() + " (" + faction.getName()
                    + ") > Ally]§f " + StringArgumentType.getString(ctx, "message")));
        }
        return ok();
    }

    private static int doBorders(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (src.getEntity() instanceof ServerPlayer player) {
            WarForgeMod.NETWORK.sendTo(new PacketToggleBorders(), player);
        } else {
            src.sendFailure(t("cmd.players_only"));
        }
        return ok();
    }

    private static int doDebugMsg(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            factions().notifier.toPlayer(player, "warforge_notification_debug",
                    Component.literal(StringArgumentType.getString(ctx, "title")), Component.literal(StringArgumentType.getString(ctx, "subtitle")),
                    hexToArgb(StringArgumentType.getString(ctx, "color")), FactionNotifier.DURATION_DEFAULT_MS, player.getUUID());
        }
        return ok();
    }

    /** Showcase filming helper: the FOB screen's two buttons as commands (a scripted player cannot click them). */
    private static LiteralArgumentBuilder<CommandSourceStack> buildFobTree() {
        return Commands.literal("fob")
                .then(Commands.literal("establish").then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    net.minecraft.core.BlockPos base = player.blockPosition();
                    for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(base.offset(-4, -2, -4), base.offset(4, 2, 4))) {
                        if (player.level().getBlockEntity(pos) instanceof com.flansmod.warforge.common.blocks.FobBlockEntity te
                                && te.ownerFaction.equals(FactionIds.NONE)) {
                            Faction owner = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
                            Component denial = owner == null ? Component.literal("not in a faction")
                                    : WarForgeMod.FOBS.checkPlacement(owner, new DimChunkPos(player.level().dimension(), pos));
                            if (denial != null) {
                                ctx.getSource().sendFailure(denial);
                                return 0;
                            }
                            boolean made = WarForgeMod.FOBS.requestCreateFob(te, player, StringArgumentType.getString(ctx, "name"));
                            if (!made) {
                                ctx.getSource().sendFailure(Component.literal("FOB not established (officer role?)"));
                                return 0;
                            }
                            return Command.SINGLE_SUCCESS;
                        }
                    }
                    ctx.getSource().sendFailure(Component.literal("No unestablished FOB block within 4 blocks"));
                    return 0;
                })))
                .then(Commands.literal("warp").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
                    List<com.flansmod.warforge.server.fob.Fob> fobs = faction == null ? List.of() : WarForgeMod.FOBS.fobsOf(faction);
                    if (fobs.isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("Your faction has no FOB"));
                        return 0;
                    }
                    WarForgeMod.FOBS.requestFobWarp(player, fobs.get(0));
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildVeinTree() {
        LiteralArgumentBuilder<CommandSourceStack> vein = Commands.literal("vein").requires(CommandFactions::isOp);
        vein.executes(ctx -> { veinUsage(ctx.getSource()); return Command.SINGLE_SUCCESS; });

        vein.then(veinAtCapable(Commands.literal("info"), (ctx, dim, baseX, baseZ, radius) -> doVeinInfo(ctx.getSource(), dim, baseX, baseZ)));
        vein.then(veinAtCapable(Commands.literal("clear"), (ctx, dim, baseX, baseZ, radius) -> doVeinClear(ctx.getSource(), dim, baseX, baseZ, radius)));
        vein.then(veinAtCapable(Commands.literal("reroll"), (ctx, dim, baseX, baseZ, radius) -> doVeinReroll(ctx.getSource(), dim, baseX, baseZ, radius)));
        vein.then(veinAtCapable(Commands.literal("seed"), (ctx, dim, baseX, baseZ, radius) -> doVeinReroll(ctx.getSource(), dim, baseX, baseZ, radius)));

        vein.then(Commands.literal("set")
                .then(veinAtCapable(Commands.argument("vein", StringArgumentType.string()),
                        (ctx, dim, baseX, baseZ, radius) -> doVeinSet(ctx, dim, baseX, baseZ, radius, false))
                        .then(veinAtCapable(Commands.argument("quality", StringArgumentType.string()),
                                (ctx, dim, baseX, baseZ, radius) -> doVeinSet(ctx, dim, baseX, baseZ, radius, true)))));

        return vein;
    }

    @FunctionalInterface
    private interface VeinAction {
        int run(CommandContext<CommandSourceStack> ctx, ResourceKey<Level> dim, int baseX, int baseZ, int radius);
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T veinAtCapable(T node, VeinAction action) {
        node.executes(ctx -> runVeinFromPlayer(ctx, action));
        node.then(Commands.literal("at")
                .then(Commands.argument("atX", IntegerArgumentType.integer())
                        .then(Commands.argument("atZ", IntegerArgumentType.integer())
                                .executes(ctx -> runVeinAt(ctx, action, false, false))
                                .then(Commands.argument("atDim", StringArgumentType.string())
                                        .executes(ctx -> runVeinAt(ctx, action, true, false))
                                        .then(Commands.argument("atRadius", IntegerArgumentType.integer())
                                                .executes(ctx -> runVeinAt(ctx, action, true, true)))))));
        return node;
    }

    private static boolean veinReady(CommandSourceStack src) {
        if (!WarForgeConfig.ENABLE_CHUNK_VEINS) {
            src.sendFailure(t("cmd.vein_disabled"));
            return false;
        }
        if (WarForgeMod.VEIN_HANDLER == null || !WarForgeMod.VEIN_HANDLER.hasFinishedInit) {
            src.sendFailure(t("cmd.vein_not_ready"));
            return false;
        }
        return true;
    }

    private static int runVeinFromPlayer(CommandContext<CommandSourceStack> ctx, VeinAction action) {
        CommandSourceStack src = ctx.getSource();
        if (!veinReady(src)) return Command.SINGLE_SUCCESS;
        if (!(src.getEntity() instanceof Player player)) {
            src.sendFailure(t("cmd.vein_console_at"));
            return Command.SINGLE_SUCCESS;
        }
        int baseX = Chunks.x(player.chunkPosition());
        int baseZ = Chunks.z(player.chunkPosition());
        ResourceKey<Level> dim = player.level().dimension();
        return action.run(ctx, dim, baseX, baseZ, 0);
    }

    private static int runVeinAt(CommandContext<CommandSourceStack> ctx, VeinAction action, boolean hasDim, boolean hasRadius) {
        CommandSourceStack src = ctx.getSource();
        if (!veinReady(src)) return Command.SINGLE_SUCCESS;
        int baseX = IntegerArgumentType.getInteger(ctx, "atX");
        int baseZ = IntegerArgumentType.getInteger(ctx, "atZ");
        ResourceKey<Level> dim = hasDim
                ? ResourceKey.create(Registries.DIMENSION, Ids.parse(StringArgumentType.getString(ctx, "atDim")))
                : senderDim(src);
        int radius = hasRadius ? IntegerArgumentType.getInteger(ctx, "atRadius") : 0;
        radius = Math.max(0, Math.min(radius, 16));
        return action.run(ctx, dim, baseX, baseZ, radius);
    }

    private static void veinUsage(CommandSourceStack src) {
        src.sendFailure(t("cmd.vein_usage", OP_HELP_LINES.get(4)));
    }

    private static int doVeinInfo(CommandSourceStack src, ResourceKey<Level> dim, int baseX, int baseZ) {
        Pair<Vein, Quality> info = WarForgeMod.VEIN_HANDLER.peekStoredVein(dim, baseX, baseZ);
        String location = "chunk (" + baseX + ", " + baseZ + ") in dim " + Keys.id(dim);
        if (info == null) {
            src.sendSuccess(() -> t("cmd.vein_info_none", location), false);
        } else if (info.getLeft() == null) {
            src.sendSuccess(() -> t("cmd.vein_info_cleared", location), false);
        } else {
            src.sendSuccess(() -> t("cmd.vein_info", location, info.getLeft().translationKey, String.valueOf(info.getRight())), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int doVeinSet(CommandContext<CommandSourceStack> ctx, ResourceKey<Level> dim, int baseX, int baseZ, int radius, boolean hasQuality) {
        CommandSourceStack src = ctx.getSource();
        Vein vein = WarForgeMod.VEIN_HANDLER.findVein(StringArgumentType.getString(ctx, "vein"));
        if (vein == null) {
            src.sendFailure(t("cmd.vein_unknown", StringArgumentType.getString(ctx, "vein")));
            return Command.SINGLE_SUCCESS;
        }
        int quality = Quality.FAIR.ordinal();
        if (hasQuality) {
            String qualityArg = StringArgumentType.getString(ctx, "quality");
            try {
                quality = Quality.valueOf(qualityArg.toUpperCase(Locale.ROOT)).ordinal();
            } catch (IllegalArgumentException e) {
                src.sendFailure(t("cmd.vein_unknown_quality", qualityArg));
                return Command.SINGLE_SUCCESS;
            }
        }
        int count = 0;
        for (int x = baseX - radius; x <= baseX + radius; ++x) {
            for (int z = baseZ - radius; z <= baseZ + radius; ++z) {
                WarForgeMod.VEIN_HANDLER.setVeinOverride(dim, x, z, vein.getId(), quality);
                WarForgeMod.JOURNEYMAP_VEIN_SYNC.onVeinChanged(dim, x, z);
                ++count;
            }
        }
        final int c = count;
        final int q = quality;
        src.sendSuccess(() -> t("cmd.vein_set", c, vein.translationKey, String.valueOf(Quality.getQuality(q))), false);
        refreshClaimViews(src);
        return Command.SINGLE_SUCCESS;
    }

    private static int doVeinClear(CommandSourceStack src, ResourceKey<Level> dim, int baseX, int baseZ, int radius) {
        int count = 0;
        for (int x = baseX - radius; x <= baseX + radius; ++x) {
            for (int z = baseZ - radius; z <= baseZ + radius; ++z) {
                WarForgeMod.VEIN_HANDLER.clearVeinAt(dim, x, z);
                WarForgeMod.JOURNEYMAP_VEIN_SYNC.onVeinChanged(dim, x, z);
                ++count;
            }
        }
        final int c = count;
        src.sendSuccess(() -> t("cmd.vein_cleared", c), false);
        refreshClaimViews(src);
        return Command.SINGLE_SUCCESS;
    }

    private static int doVeinReroll(CommandSourceStack src, ResourceKey<Level> dim, int baseX, int baseZ, int radius) {
        long seed = src.getServer().overworld().getSeed();
        int count = 0;
        for (int x = baseX - radius; x <= baseX + radius; ++x) {
            for (int z = baseZ - radius; z <= baseZ + radius; ++z) {
                WarForgeMod.VEIN_HANDLER.rerollVeinAt(dim, x, z, seed);
                WarForgeMod.JOURNEYMAP_VEIN_SYNC.onVeinChanged(dim, x, z);
                ++count;
            }
        }
        final int c = count;
        src.sendSuccess(() -> t("cmd.vein_rerolled", c), false);
        refreshClaimViews(src);
        return Command.SINGLE_SUCCESS;
    }

    private static ResourceKey<Level> senderDim(CommandSourceStack src) {
        return (src.getEntity() instanceof Player player) ? player.level().dimension() : Level.OVERWORLD;
    }

    private static void refreshClaimViews(CommandSourceStack src) {
        for (ServerPlayer player : src.getServer().getPlayerList().getPlayers()) {
            ClaimMapView.send(player, new DimChunkPos(player.level().dimension(), player.blockPosition()), WarForgeConfig.CLAIM_MANAGER_RADIUS);
        }
    }

    public static int hexToArgb(String hex) {
        if (hex == null) return 0xFFFFFFFF;

        String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;

        try {
            long value = Long.parseLong(cleanHex, 16);

            if (cleanHex.length() <= 6) {
                return (int) (value | 0xFF000000);
            }

            return (int) value;
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }
}
