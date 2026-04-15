package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketMoveCitadel extends PacketBase 
{
	public DimBlockPos pos = DimBlockPos.ZERO;

	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		data.writeInt(pos.dim);
		data.writeInt(pos.getX());
		data.writeInt(pos.getY());
		data.writeInt(pos.getZ());
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
		pos = new DimBlockPos(data.readInt(), data.readInt(), data.readInt(), data.readInt());
	}

	@Override
	public void handleServerSide(EntityPlayerMP player) 
	{
		WarForgeMod.FACTIONS.requestMoveCitadel(player, pos);
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer)
	{
		 //noop
	}

}
