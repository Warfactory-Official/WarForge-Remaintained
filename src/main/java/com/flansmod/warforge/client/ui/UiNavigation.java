package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.network.PacketRequestInsurance;
import com.flansmod.warforge.common.network.PacketRequestMemberData;
import com.flansmod.warforge.common.network.PacketRequestOperations;
import com.flansmod.warforge.common.network.PacketRequestUpgradeUI;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.util.DimChunkPos;

import java.util.UUID;

public final class UiNavigation {
    private UiNavigation() {}

    public static void openClaimManager(DimChunkPos center, int radius, int pageX, int pageZ) {
        ClaimManagerScreen.open(center, radius, pageX, pageZ);
    }

    public static void openFactionStats(UUID factionId) {
        FactionStatsScreen.open(factionId);
    }

    public static void openFlagSelect(UUID factionId, String factionName, int factionColour, String currentFlagId) {
        FlagSelectScreen.open(factionId, factionName, factionColour, currentFlagId);
    }

    public static void openCreateFaction() {
        CreateFactionScreen.open();
    }

    public static void openRecolour(int colour) {
        CreateFactionScreen.openRecolour(colour);
    }

    public static void openMemberScreen() {
        MemberManagerScreen.open();
    }

    public static void openUpgradeScreen() {
        FactionUpgradeScreen.open();
    }

    public static void openOperationsScreen() {
        OperationsScreen.open();
    }

    public static void openSiegeCamp(PacketSiegeCampInfo data) {
        SiegeCampScreen.open(data);
    }

    public static void requestInsurance(UUID factionId) {
        PacketRequestInsurance packet = new PacketRequestInsurance();
        packet.factionId = factionId;
        WarForgeMod.NETWORK.sendToServer(packet);
    }

    public static void requestMembers(FactionMemberManagerGuiData.Page page) {
        PacketRequestMemberData packet = new PacketRequestMemberData();
        packet.page = page;
        WarForgeMod.NETWORK.sendToServer(packet);
    }

    public static void requestUpgrade(UUID factionId) {
        PacketRequestUpgradeUI packet = new PacketRequestUpgradeUI();
        packet.factionIdRequest = factionId;
        WarForgeMod.NETWORK.sendToServer(packet);
    }

    public static void requestOperations() {
        WarForgeMod.NETWORK.sendToServer(new PacketRequestOperations());
    }
}
