package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.ui.InsuranceUi;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketRequestInsurance extends ServerboundPacket {
    public UUID factionId = FactionIds.NONE;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUUID(data, factionId);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        factionId = readUUID(data);
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        UUID effectiveId = factionId;
        if (effectiveId.equals(FactionIds.NONE)) {
            Faction playerFaction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
            effectiveId = playerFaction == null ? FactionIds.NONE : playerFaction.getUuid();
        }

        Faction faction = WarForgeMod.FACTIONS.registry.getFaction(effectiveId);
        PacketInsuranceData view = new PacketInsuranceData();
        if (faction == null) {
            InsuranceUi.open(player, view, null);
            return;
        }

        // factionId is the client's: vault contents go only to members (ops too)
        if (!WarForgeMod.isOp(player) && !faction.isPlayerInFaction(player.getUUID())) {
            PacketSecurity.violation(player, "insurance request for a faction they are not in (" + effectiveId + ")");
            InsuranceUi.open(player, view, null);
            return;
        }

        view.hasFaction = true;
        view.factionId = faction.getUuid();
        view.factionName = faction.getName();
        view.factionColor = faction.getColour();
        view.canDeposit = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUUID(), Role.OFFICER);
        view.canVoid = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUUID(), Role.LEADER);
        view.slotCount = faction.getInsuranceSlotCount();
        InsuranceUi.open(player, view, faction);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received PacketRequestInsurance on client side");
    }
}
