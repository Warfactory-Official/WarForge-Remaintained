package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
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
        PacketInsuranceData response = new PacketInsuranceData();

        if (faction == null) {
            WarForgeMod.NETWORK.sendTo(response, player);
            return;
        }

        response.hasFaction = true;
        response.factionId = faction.uuid;
        response.factionName = faction.name;
        response.factionColor = faction.colour;
        response.canWithdraw = WarForgeMod.isOp(player)
                || faction.isPlayerRoleInFaction(player.getUUID(), Faction.Role.LEADER);

        // factionId is the client's, so this must not become "read any faction's stash": the vault
        // contents only go to members of that faction (ops included for moderation).
        if (!WarForgeMod.isOp(player) && !faction.isPlayerInFaction(player.getUUID())) {
            PacketSecurity.violation(player, "insurance request for a faction they are not in (" + effectiveId + ")");
            WarForgeMod.NETWORK.sendTo(new PacketInsuranceData(), player);
            return;
        }

        int slotCount = faction.getInsuranceSlotCount();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) {
            stacks.add(faction.getInsuranceStack(i));
        }
        response.stacks = stacks;

        WarForgeMod.NETWORK.sendTo(response, player);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received PacketRequestInsurance on client side");
    }
}
