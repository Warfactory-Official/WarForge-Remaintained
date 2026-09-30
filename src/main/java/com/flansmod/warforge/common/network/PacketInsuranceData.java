package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.FactionInsuranceGuiFactory;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import com.flansmod.warforge.compat.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketInsuranceData extends PacketBase {
    public static PacketInsuranceData latest = null;

    public boolean hasFaction = false;
    public UUID factionId = Faction.nullUuid;
    public String factionName = "";
    public int factionColor = 0xFFFFFF;
    public boolean canWithdraw = false;
    public List<ItemStack> stacks = new ArrayList<>();

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeBoolean(hasFaction);
        writeUUID(data, factionId);
        writeUTF(data, factionName);
        data.writeInt(factionColor);
        data.writeBoolean(canWithdraw);
        data.writeShort(stacks.size());
        for (ItemStack stack : stacks) {
            Net.writeItem(data, stack);
        }
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        hasFaction = data.readBoolean();
        factionId = readUUID(data);
        factionName = readUTF(data);
        factionColor = data.readInt();
        canWithdraw = data.readBoolean();
        int count = data.readShort();
        stacks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            stacks.add(Net.readItem(data));
        }
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        WarForgeMod.LOGGER.error("Received PacketInsuranceData on server side");
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        latest = this;
        FactionInsuranceGuiFactory.INSTANCE.openInsuranceScreen();
    }
}
