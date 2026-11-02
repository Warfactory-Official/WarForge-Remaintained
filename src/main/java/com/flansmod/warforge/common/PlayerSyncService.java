package com.flansmod.warforge.common;

import com.flansmod.warforge.common.network.PacketSyncConfig;
import com.flansmod.warforge.server.ClaimMapView;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.common.network.PacketCitadelUpgradeRequirement;
import com.flansmod.warforge.common.network.PacketVeinEntries;
import com.flansmod.warforge.common.network.SyncQueueHandler;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import static com.flansmod.warforge.common.WarForgeMod.NETWORK;

/** Sends a player everything their client needs from the server's config and world state. */
public final class PlayerSyncService {
    private PlayerSyncService() {}

    /** Login: config and faction state, then the bulk data trickled through the sync queue. */
    public static void onLogin(ServerPlayer player) {
        NETWORK.sendTo(new PacketSyncConfig(WarForgeConfig.snapshot()), player);

        WarForgeMod.FACTIONS.membership.onLoggedIn(player);
        WarForgeMod.FACTIONS.sieges.updateAttackerPresence(player);

        NETWORK.sendTo(WarClock.timeUpdate(), player);
        WarForgeMod.FACTIONS.mapSync.sendAll(player);
        ClaimMapView.send(player, new DimChunkPos(player.level().dimension(), player.blockPosition()), WarForgeConfig.CLAIM_MANAGER_RADIUS);

        NETWORK.sendTo(com.flansmod.warforge.server.SiegeSync.clearSiegeData(), player);
        WarForgeMod.FACTIONS.siegeSync.sendAllInfoToNearby();
        syncConfigDependentData(player, task -> SyncQueueHandler.enqueue(player, () -> task.accept(player)));
    }

    /** After a config reload: the same data, sent at once. */
    public static void resync(ServerPlayer player) {
        NETWORK.sendTo(new PacketSyncConfig(WarForgeConfig.snapshot()), player);
        syncConfigDependentData(player, task -> task.accept(player));
    }

    private static void syncConfigDependentData(ServerPlayer player, Consumer<Consumer<ServerPlayer>> schedule) {
        WarForgeMod.FLAG_REGISTRY.syncToPlayer(player);
        WarForgeMod.JOURNEYMAP_VEIN_SYNC.onPlayerJoin(player);

        var levels = WarForgeMod.UPGRADE_HANDLER.getLevels();
        for (int i = 0; i < levels.size(); i++) {
            final int level = i;
            final var spec = levels.get(i);
            schedule.accept(target -> NETWORK.sendTo(
                    new PacketCitadelUpgradeRequirement(level, new HashMap<>(spec.requirements()), spec.claimLimit(),
                            spec.insuranceSlots(), spec.loadedChunks()), target));
        }

        ArrayList<Vein> veins = WarForgeConfig.ENABLE_CHUNK_VEINS
                ? new ArrayList<>(WarForgeMod.VEIN_HANDLER.ID_TO_VEINS.values()) : new ArrayList<>();
        int index = 0;
        while (index < veins.size()) {
            PacketVeinEntries packet = new PacketVeinEntries();
            index = packet.fillFrom(veins, index);
            schedule.accept(target -> NETWORK.sendTo(packet, target));
        }
    }

    public static void syncClaimToPlayer(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BlockEntity be = com.flansmod.warforge.compat.Players.serverLevel(serverPlayer).getBlockEntity(pos);
        if (be instanceof com.flansmod.warforge.common.blocks.ClaimBlockEntity claim) {
            var packet = claim.getUpdatePacket();
            if (packet != null) {
                serverPlayer.connection.send(packet);
            }
        }
    }
}
