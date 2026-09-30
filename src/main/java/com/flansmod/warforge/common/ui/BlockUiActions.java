package com.flansmod.warforge.common.ui;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.common.factories.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.network.PacketDisbandFaction;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PacketOpenCreateFaction;
import com.flansmod.warforge.common.network.PacketOpenFlagSelect;
import com.flansmod.warforge.common.network.PacketOpenRecolour;
import com.flansmod.warforge.common.network.PacketRequestInsurance;
import com.flansmod.warforge.common.network.PacketRequestMemberData;
import com.flansmod.warforge.common.network.PacketRequestUpgradeUI;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.fob.Fob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Server-side actions of the block UI buttons (citadel, claim, FOB, collector). Both UI libraries call these, so
 * the buttons do the same on every version.
 */
public final class BlockUiActions {
    private BlockUiActions() {
    }

    public static void close(ServerPlayer player) {
        player.closeContainer();
    }

    /** Sends the faction info screen of a faction (nothing if it does not exist). */
    public static void showFactionInfo(ServerPlayer player, UUID factionId) {
        Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
        if (faction != null) {
            PacketFactionInfo packet = new PacketFactionInfo();
            packet.info = faction.createInfo(player.getUUID());
            WarForgeMod.NETWORK.sendTo(packet, player);
        }
    }

    public static void chooseFlag(ServerPlayer player, UUID factionId) {
        PacketOpenFlagSelect packet = new PacketOpenFlagSelect();
        packet.factionId = factionId;
        WarForgeMod.NETWORK.sendTo(packet, player);
    }

    public static void openMembers(ServerPlayer player) {
        PacketRequestMemberData request = new PacketRequestMemberData();
        request.page = FactionMemberManagerGuiData.Page.MEMBERS;
        request.handleServerSide(player);
    }

    public static void openInsurance(ServerPlayer player, UUID factionId) {
        PacketRequestInsurance request = new PacketRequestInsurance();
        request.factionId = factionId;
        request.handleServerSide(player);
    }

    public static void openUpgrade(ServerPlayer player, UUID factionId) {
        PacketRequestUpgradeUI request = new PacketRequestUpgradeUI();
        request.mFactionIDRequest = factionId;
        request.handleServerSide(player);
    }

    public static void openRecolour(ServerPlayer player, TileEntityCitadel citadel) {
        PacketOpenRecolour packet = new PacketOpenRecolour();
        packet.colour = citadel.colour;
        WarForgeMod.NETWORK.sendTo(packet, player);
    }

    /** Disbands the player's faction and closes the UI. */
    public static void disband(ServerPlayer player) {
        new PacketDisbandFaction().handleServerSide(player);
        player.closeContainer();
    }

    /** Opens the create-faction screen for the citadel at pos. */
    public static void openCreateFaction(ServerPlayer player, BlockPos pos) {
        PacketOpenCreateFaction packet = new PacketOpenCreateFaction();
        packet.pos = new DimBlockPos(player.level().dimension(), pos);
        WarForgeMod.NETWORK.sendTo(packet, player);
    }

    public static void unclaim(ServerPlayer player, DimBlockPos claimPos) {
        WarForgeMod.FACTIONS.requestRemoveClaim(player, claimPos);
    }

    public static void moveCitadel(ServerPlayer player) {
        WarForgeMod.FACTIONS.requestMoveCitadel(player, DimBlockPos.ZERO);
    }

    /** Warps the player to the FOB whose block is at pos. */
    public static void warpToFob(ServerPlayer player, BlockPos pos) {
        DimBlockPos dpos = new DimBlockPos(player.level().dimension(), pos);
        Fob warpTarget = WarForgeMod.FOBS.getFobAt(dpos.toChunkPos());
        if (warpTarget != null) {
            WarForgeMod.FOBS.requestFobWarp(player, warpTarget);
        }
    }
}
