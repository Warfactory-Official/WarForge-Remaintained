package com.flansmod.warforge.common.network;

import com.flansmod.warforge.compat.Nbt;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.effect.EffectRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketEffect extends PacketBase {
    public double x, y, z;
    public String type = "";
    public String dataNBT = "";


    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeDouble(x);
        data.writeDouble(y);
        data.writeDouble(z);
        writeUTF(data, type);
        writeUTF(data, dataNBT);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        this.x = data.readDouble();
        this.y = data.readDouble();
        this.z = data.readDouble();
        this.type = readUTF(data);
        this.dataNBT = readUTF(data);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        WarForgeMod.LOGGER.error("Recieved effect packet on Server side!");
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        CompoundTag compound;
        try {
            compound = Nbt.parse(dataNBT);
        } catch (Exception e) {
            WarForgeMod.LOGGER.error("Malformed effect data NBT for " + type);
            return;
        }
        EffectRegistry.runClientEffect(type, clientPlayer, x, y, z, compound);
    }
}
