package com.flansmod.warforge.client;

import com.flansmod.warforge.client.hud.TimersHud;
import com.flansmod.warforge.client.util.WarForgeNotifications;
import com.flansmod.warforge.common.SyncedVeins;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.ClientPacketRouter;
import com.flansmod.warforge.client.ui.ClaimManagerScreen;
import com.flansmod.warforge.common.network.PacketChunkPosVeinID;
import com.flansmod.warforge.common.network.PacketClaimChunksData;
import com.flansmod.warforge.common.network.PacketClaimCacheState;
import com.flansmod.warforge.common.network.PacketClaimMapDelta;
import com.flansmod.warforge.common.network.PacketClientNotification;
import com.flansmod.warforge.common.network.PacketEffect;
import com.flansmod.warforge.client.fx.EffectRegistry;
import com.flansmod.warforge.compat.Nbt;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import com.flansmod.warforge.common.network.PacketFlagChunk;
import com.flansmod.warforge.common.network.PacketFlagManifest;
import com.flansmod.warforge.common.network.PacketJourneyMapVeins;
import com.flansmod.warforge.common.network.PacketNamePlateChange;
import com.flansmod.warforge.client.render.HudClock;
import com.flansmod.warforge.common.network.PacketSiegeCampProgressUpdate;
import com.flansmod.warforge.common.network.PacketSyncConfig;
import com.flansmod.warforge.common.network.PacketTerrainColors;
import com.flansmod.warforge.common.network.PacketTimeUpdates;
import com.flansmod.warforge.common.network.PacketVeinEntries;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.client.border.ClaimBorderRenderer;
import com.flansmod.warforge.client.ui.UiNavigation;
import com.flansmod.warforge.common.ui.BlockUiState;
import net.minecraft.network.chat.Component;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PacketOpenFlagSelect;
import com.flansmod.warforge.common.network.PacketOpenRecolour;
import com.flansmod.warforge.common.network.PacketOpenCreateFaction;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.PacketUpgradeUI;
import com.flansmod.warforge.common.network.PacketMemberData;
import com.flansmod.warforge.common.network.PacketOperationsData;
import com.flansmod.warforge.common.network.PacketCitadelUpgradeRequirement;
import com.flansmod.warforge.common.network.PacketToggleBorders;
import com.flansmod.warforge.common.network.PacketBlockUiState;
import com.flansmod.warforge.common.network.PacketLeaderboardInfo;
import com.flansmod.warforge.common.network.PacketInsuranceData;
import net.minecraft.world.entity.player.Player;

final class ClientPackets {
    private ClientPackets() {}

    static void register() {
        ClientPacketRouter.register(PacketFactionInfo.class, ClientPackets::factionInfo);
        ClientPacketRouter.register(PacketOpenFlagSelect.class, (packet, player) ->
                UiNavigation.openFlagSelect(packet.factionId, packet.factionName, packet.factionColor, packet.currentFlagId));
        ClientPacketRouter.register(PacketOpenRecolour.class, (packet, player) -> UiNavigation.openRecolour(packet.colour));
        ClientPacketRouter.register(PacketOpenCreateFaction.class, ClientPackets::openCreateFaction);
        ClientPacketRouter.register(PacketSiegeCampInfo.class, ClientPackets::siegeCampInfo);
        ClientPacketRouter.register(PacketUpgradeUI.class, ClientPackets::upgradeUi);
        ClientPacketRouter.register(PacketMemberData.class, ClientPackets::memberData);
        ClientPacketRouter.register(PacketOperationsData.class, ClientPackets::operationsData);
        ClientPacketRouter.register(PacketCitadelUpgradeRequirement.class, (packet, player) ->
                ClientUpgradeTable.set(packet.level, packet.requirements, packet.limit, packet.insuranceSlots, packet.loadedChunks));
        ClientPacketRouter.register(PacketToggleBorders.class, ClientPackets::toggleBorders);
        ClientPacketRouter.register(PacketBlockUiState.class, (packet, player) -> BlockUiState.receive(packet.pos, packet.state));
        ClientPacketRouter.register(PacketLeaderboardInfo.class, (packet, player) -> PacketLeaderboardInfo.sLatestInfo = packet.info);
        ClientPacketRouter.register(PacketInsuranceData.class, (packet, player) -> PacketInsuranceData.latest = packet);
        ClientPacketRouter.register(PacketChunkPosVeinID.class, ClientPackets::veinId);
        ClientPacketRouter.register(PacketClaimChunksData.class, ClientPackets::claimChunks);
        ClientPacketRouter.register(PacketClaimMapDelta.class, ClientPackets::claimMapDelta);
        ClientPacketRouter.register(PacketClaimCacheState.class, (packet, player) -> ClientBorderCache.applyState(packet));
        ClientPacketRouter.register(PacketClientNotification.class, ClientPackets::notification);
        ClientPacketRouter.register(PacketEffect.class, ClientPackets::effect);
        ClientPacketRouter.register(PacketFlagChunk.class, ClientPackets::flagChunk);
        ClientPacketRouter.register(PacketFlagManifest.class, ClientPackets::flagManifest);
        ClientPacketRouter.register(PacketJourneyMapVeins.class, ClientPackets::journeyMapVeins);
        ClientPacketRouter.register(PacketNamePlateChange.class, ClientPackets::namePlate);
        ClientPacketRouter.register(PacketSiegeCampProgressUpdate.class, ClientPackets::siegeProgress);
        ClientPacketRouter.register(PacketSyncConfig.class, ClientPackets::syncConfig);
        ClientPacketRouter.register(PacketTerrainColors.class, ClientPackets::terrainColors);
        ClientPacketRouter.register(PacketTimeUpdates.class, ClientPackets::timeUpdates);
        ClientPacketRouter.register(PacketVeinEntries.class, ClientPackets::veinEntries);
    }

    private static void factionInfo(PacketFactionInfo packet, Player player) {
        PacketFactionInfo.latestInfo = packet.info;
        if (packet.info != null) {
            UiNavigation.openFactionStats(packet.info.factionId);
        }
    }

    private static void openCreateFaction(PacketOpenCreateFaction packet, Player player) {
        PacketOpenCreateFaction.pendingCitadelPos = packet.pos;
        UiNavigation.openCreateFaction();
    }

    private static void siegeCampInfo(PacketSiegeCampInfo packet, Player player) {
        PacketSiegeCampInfo.lastReceived = packet;
        UiNavigation.openSiegeCamp(packet);
    }

    private static void upgradeUi(PacketUpgradeUI packet, Player player) {
        PacketUpgradeUI.latest = packet;
        UiNavigation.openUpgradeScreen();
    }

    private static void memberData(PacketMemberData packet, Player player) {
        PacketMemberData.latest = packet;
        UiNavigation.openMemberScreen();
    }

    private static void operationsData(PacketOperationsData packet, Player player) {
        PacketOperationsData.latest = packet;
        UiNavigation.openOperationsScreen();
    }

    private static void toggleBorders(PacketToggleBorders packet, Player player) {
        ClaimBorderRenderer.Mode mode = ClaimBorderRenderer.cycleMode();
        player.sendSystemMessage(Component.translatable("warforge.client.borders." + mode.key));
    }

    private static void veinId(PacketChunkPosVeinID packet, Player player) {
        if (packet.veinLocation != null) {
            ClientProxy.CHUNK_VEIN_CACHE.add(packet.veinLocation, packet.resultInfo);
        }
    }

    private static void claimChunks(PacketClaimChunksData packet, Player player) {
        ClientClaimChunkCache.setPlayerFactionId(packet.playerFactionId);
        if (packet.outlineOnly) {
            ClientBorderCache.replaceWindow(packet.dim, packet.centerX, packet.centerZ, packet.radius, packet.chunks);
            return;
        }
        ClientClaimChunkCache.replaceAll(packet.dim, packet.forceLoadedCount, packet.forceLoadedMax, packet.claimCount, packet.claimMax, packet.chunks);
        ClaimManagerScreen.refreshIfOpen();
    }

    private static void claimMapDelta(PacketClaimMapDelta packet, Player player) {
        ClientBorderCache.applyWorldDelta(packet);
        for (PacketClaimMapDelta.Set set : packet.sets) {
            BorderSyncScheduler.invalidateNear(set.dim(), set.x(), set.z());
        }
        for (PacketClaimMapDelta.Remove remove : packet.removes) {
            BorderSyncScheduler.invalidateNear(remove.dim(), remove.x(), remove.z());
        }
    }

    private static void notification(PacketClientNotification packet, Player player) {
        String subtitle = packet.subtitle.getString();
        WarForgeNotifications.show(packet.token, packet.title.getString(), subtitle.isEmpty() ? null : subtitle,
                packet.accentColor, packet.durationMs, packet.playerId);
    }

    private static void effect(PacketEffect packet, Player player) {
        CompoundTag data;
        try {
            data = Nbt.parse(packet.dataNBT);
        } catch (CommandSyntaxException e) {
            WarForgeMod.LOGGER.warn("Malformed effect data for {}", packet.type, e);
            return;
        }
        EffectRegistry.runClientEffect(packet.type, player, packet.x, packet.y, packet.z, data);
    }

    private static void flagChunk(PacketFlagChunk packet, Player player) {
        ClientFlagRegistry.receiveCustomFlagChunk(packet.flagId, packet.width, packet.height, packet.partIndex, packet.totalParts, packet.data);
    }

    private static void flagManifest(PacketFlagManifest packet, Player player) {
        ClientFlagRegistry.setAvailableFlags(packet.flagIds);
    }

    private static void journeyMapVeins(PacketJourneyMapVeins packet, Player player) {
        if (packet.clear) {
            JourneyMapVeinCache.applyClear();
        }
        for (PacketJourneyMapVeins.Set set : packet.sets) {
            JourneyMapVeinCache.set(set.dim, set.x, set.z, set.veinInfo);
        }
        for (PacketJourneyMapVeins.Remove remove : packet.removes) {
            JourneyMapVeinCache.remove(remove.dim, remove.x, remove.z);
        }
    }

    private static void namePlate(PacketNamePlateChange packet, Player player) {
        if (packet.isRemove) {
            PlayerNametagCache.INSTANCE.remove(packet.name);
        } else {
            PlayerNametagCache.INSTANCE.add(packet.name, packet.faction, packet.color);
        }
    }

    private static void siegeProgress(PacketSiegeCampProgressUpdate packet, Player player) {
        if (packet.clearAll) {
            ClientSiegeState.clear();
            return;
        }
        if (packet.info.endTimestamp != Long.MAX_VALUE) {
            packet.info.endTimestamp = HudClock.rebase(packet.info.endTimestamp, packet.msServerNow);
        }
        ClientSiegeState.update(packet.info);
    }

    private static void syncConfig(PacketSyncConfig packet, Player player) {
        try {
            WarForgeConfig.apply(packet.snapshot);
        } catch (RuntimeException e) {
            WarForgeMod.LOGGER.error("Failed to apply synced config from server", e);
            return;
        }
        SyncedVeins.megachunkLength = packet.snapshot.megachunkLength();
        SyncedVeins.ENTRIES.clear();
        JourneyMapVeinCache.applyClear();
        WarForgeMod.LOGGER.info("Applied synced WarForge config from server");
    }

    private static void terrainColors(PacketTerrainColors packet, Player player) {
        for (int i = 0; i < packet.chunkCoords.size(); i++) {
            int[] coord = packet.chunkCoords.get(i);
            ServerTerrainCache.put(packet.dim, coord[0], coord[1], packet.colors.get(i), packet.heights.get(i));
        }
    }

    private static void timeUpdates(PacketTimeUpdates packet, Player player) {
        long clientNow = HudClock.now();
        TimersHud.setDeadlines(clientNow + packet.msUntilNextSiegeDay, clientNow + packet.msUntilNextYieldDay);
    }

    private static void veinEntries(PacketVeinEntries packet, Player player) {
        for (Vein vein : packet.veins()) {
            SyncedVeins.ENTRIES.put(vein.getId(), vein);
        }
    }
}
