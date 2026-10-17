package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.ui.InsuranceUi;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketRequestInsurance extends PacketBase {
    public UUID factionId = Faction.nullUuid;

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
        if (effectiveId.equals(Faction.nullUuid)) {
            Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUUID());
            effectiveId = playerFaction == null ? Faction.nullUuid : playerFaction.uuid;
        }

        Faction faction = WarForgeMod.FACTIONS.getFaction(effectiveId);
        PacketInsuranceData view = new PacketInsuranceData();
        if (faction == null) {
            InsuranceUi.open(player, view, null);
            return;
        }

        // factionId is the client's, so this must not become "read any faction's stash": the vault
        // contents only go to members of that faction (ops included for moderation).
        if (!WarForgeMod.isOp(player) && !faction.isPlayerInFaction(player.getUUID())) {
            PacketSecurity.violation(player, "insurance request for a faction they are not in (" + effectiveId + ")");
            InsuranceUi.open(player, view, null);
            return;
        }

        view.hasFaction = true;
        view.factionId = faction.uuid;
        view.factionName = faction.name;
        view.factionColor = faction.colour;
        view.canDeposit = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUUID(), Faction.Role.OFFICER);
        view.canVoid = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUUID(), Faction.Role.LEADER);
        view.slotCount = faction.getInsuranceSlotCount();
        InsuranceUi.open(player, view, faction);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received PacketRequestInsurance on client side");
    }
}
