package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PacketNamePlateChange extends PacketBase {
    public boolean isRemove = false;
    public String faction = "";
    public String name = "";
    public int color;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeBoolean(isRemove);
        writeUTF(data, faction);
        writeUTF(data, name);
        data.writeInt(color);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        isRemove = data.readBoolean();
        faction = readUTF(data);
        name = readUTF(data);
        color = data.readInt();

    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.info("Recieved faction nametag for " + name + " [" + faction + "]");
        if (!isRemove)
            WarForgeMod.NAMETAG_CACHE.add(name, faction, color);
        else
            WarForgeMod.NAMETAG_CACHE.remove(name);


    }
}
