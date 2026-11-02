package com.flansmod.warforge.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PacketTimeUpdates extends PacketBase
{
    public long msUntilNextSiegeDay = 0L;
    public long msUntilNextYieldDay = 0L;

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeLong(msUntilNextSiegeDay);
        data.writeLong(msUntilNextYieldDay);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        msUntilNextSiegeDay = data.readLong();
        msUntilNextYieldDay = data.readLong();
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity)
    {

    }

}
