package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.compat.Levels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.server.ServerText.t;

/** /f spawn, /f home and /f tpa: stand-still countdowns. Holds ids only; players can log out mid-countdown. */
public class TeleportsModule {
    private static final int TICKS_PER_SECOND = 20;

    private static final class PendingTeleport {
        private UUID playerId;
        private int ticksRemaining;
        private BlockPos pos;
        private DimBlockPos target;
    }

    private static final class TpaRequest {
        private UUID requester;
        private UUID target;
        private int ticksRemaining;
    }

    private final List<PendingTeleport> pendingTeleports = new ArrayList<>();
    private final List<TpaRequest> tpaRequests = new ArrayList<>();

    public void requestSpawn(Player player) {
        if (!WarForgeConfig.ENABLE_SPAWN_COMMAND) {
            player.sendSystemMessage(t("tp.spawn_disabled"));
            return;
        }
        if (!WarForgeConfig.ALLOW_SPAWN_BETWEEN_DIMENSIONS && !player.level().dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(t("tp.need_overworld"));
            return;
        }
        ServerLevel overworld = WarForgeMod.MC_SERVER.overworld();
        schedule(player, new DimBlockPos(Level.OVERWORLD, Levels.sharedSpawnPos(overworld)));
        player.sendSystemMessage(t("tp.started"));
    }

    public void requestFHome(Player player) {
        if (!WarForgeConfig.ENABLE_F_HOME_COMMAND) {
            player.sendSystemMessage(t("tp.home_disabled"));
            return;
        }
        Faction faction = FactionAccess.requireFaction(player);
        if (faction == null) {
            return;
        }
        DimBlockPos target = faction.getCitadelPos();
        if (!WarForgeConfig.ALLOW_F_HOME_BETWEEN_DIMENSIONS && !target.dim.equals(player.level().dimension())) {
            player.sendSystemMessage(t("tp.home_dimension"));
            return;
        }
        schedule(player, target);
        player.sendSystemMessage(t("tp.started"));
    }

    private void schedule(Player player, DimBlockPos target) {
        PendingTeleport tp = new PendingTeleport();
        tp.playerId = player.getUUID();
        tp.pos = player.blockPosition();
        tp.target = target;
        tp.ticksRemaining = WarForgeConfig.NUM_TICKS_FOR_WARP_COMMANDS;
        pendingTeleports.add(tp);
    }

    public void requestTpa(Player requester, ServerPlayer target) {
        if (!WarForgeConfig.ENABLE_TPA_COMMAND) {
            requester.sendSystemMessage(t("tp.tpa_disabled"));
            return;
        }
        if (requester.getUUID().equals(target.getUUID())) {
            requester.sendSystemMessage(t("tp.tpa_self"));
            return;
        }
        tpaRequests.removeIf(r -> r.requester.equals(requester.getUUID()) && r.target.equals(target.getUUID()));

        TpaRequest req = new TpaRequest();
        req.requester = requester.getUUID();
        req.target = target.getUUID();
        req.ticksRemaining = WarForgeConfig.TPA_REQUEST_TIMEOUT_SECONDS * TICKS_PER_SECOND;
        tpaRequests.add(req);

        requester.sendSystemMessage(t("tp.tpa_sent", target.getName().getString()));
        target.sendSystemMessage(t("tp.tpa_received", requester.getName().getString()));
    }

    public void acceptTpa(ServerPlayer target, UUID requesterFilter) {
        TpaRequest chosen = takeRequest(target, requesterFilter);
        if (chosen == null) {
            return;
        }
        ServerPlayer requester = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(chosen.requester);
        if (requester == null) {
            target.sendSystemMessage(t("tp.tpa_offline"));
            return;
        }
        schedule(requester, new DimBlockPos(target));
        target.sendSystemMessage(t("tp.tpa_accepted", requester.getName().getString()));
        requester.sendSystemMessage(t("tp.tpa_accepted_requester", target.getName().getString()));
    }

    public void denyTpa(ServerPlayer target, UUID requesterFilter) {
        TpaRequest chosen = takeRequest(target, requesterFilter);
        if (chosen == null) {
            return;
        }
        ServerPlayer requester = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(chosen.requester);
        if (requester != null) {
            requester.sendSystemMessage(t("tp.tpa_denied_requester", target.getName().getString()));
        }
        target.sendSystemMessage(t("tp.tpa_denied"));
    }

    /** Newest matching request, removed from the list; tells the player when there is none. */
    private TpaRequest takeRequest(ServerPlayer target, UUID requesterFilter) {
        if (!WarForgeConfig.ENABLE_TPA_COMMAND) {
            target.sendSystemMessage(t("tp.tpa_disabled"));
            return null;
        }
        for (int i = tpaRequests.size() - 1; i >= 0; i--) {
            TpaRequest req = tpaRequests.get(i);
            if (req.target.equals(target.getUUID()) && (requesterFilter == null || req.requester.equals(requesterFilter))) {
                tpaRequests.remove(i);
                return req;
            }
        }
        target.sendSystemMessage(t("tp.tpa_none"));
        return null;
    }

    /** Logout: drops the player's countdowns and requests. */
    public void onLogout(UUID playerId) {
        pendingTeleports.removeIf(tp -> tp.playerId.equals(playerId));
        tpaRequests.removeIf(r -> r.requester.equals(playerId) || r.target.equals(playerId));
    }

    public void update() {
        for (int i = pendingTeleports.size() - 1; i >= 0; i--) {
            PendingTeleport tp = pendingTeleports.get(i);
            ServerPlayer player = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(tp.playerId);
            if (player == null || !player.blockPosition().equals(tp.pos)) {
                if (player != null) {
                    player.sendSystemMessage(t("tp.cancelled"));
                }
                pendingTeleports.remove(i);
                continue;
            }
            if (tp.ticksRemaining % TICKS_PER_SECOND == 0) {
                player.sendSystemMessage(t("tp.countdown", tp.ticksRemaining / TICKS_PER_SECOND));
            }
            if (--tp.ticksRemaining == 0) {
                TeleportUtil.teleportPlayer(player, tp.target.dim, tp.target.toRegularPos());
                pendingTeleports.remove(i);
            }
        }
        for (int i = tpaRequests.size() - 1; i >= 0; i--) {
            TpaRequest req = tpaRequests.get(i);
            if (--req.ticksRemaining <= 0) {
                tpaRequests.remove(i);
                ServerPlayer requester = WarForgeMod.MC_SERVER.getPlayerList().getPlayer(req.requester);
                if (requester != null) {
                    requester.sendSystemMessage(t("tp.tpa_expired"));
                }
            }
        }
    }
}
