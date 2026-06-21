package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.effect.EffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

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
    @OnlyIn(Dist.CLIENT)
    public void handleClientSide(Player clientPlayer) {
        CompoundTag compound;
        try {
            compound = TagParser.parseTag(dataNBT);
        } catch (Exception e) {
            WarForgeMod.LOGGER.error("Malformed effect data NBT for " + type);
            return;
        }
        if (EffectRegistry.EFFECT_REGISTRY.containsKey(type)) {
            EffectRegistry.EFFECT_REGISTRY.get(type).runEffect(
                    Minecraft.getInstance().level,
                    clientPlayer,
                    Minecraft.getInstance().getTextureManager(),
                    new Random(),
                    x, y, z,
                    compound
            );

        }


    }
}
