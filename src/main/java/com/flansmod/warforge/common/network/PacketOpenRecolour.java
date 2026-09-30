package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.factories.CreateFactionGuiFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class PacketOpenRecolour extends PacketBase {
    public int colour = 0xFFFFFF;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeInt(colour);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        colour = data.readInt();
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        CreateFactionGuiFactory.INSTANCE.openRecolourScreen(colour);
    }
}
