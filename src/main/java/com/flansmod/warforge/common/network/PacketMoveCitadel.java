package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketMoveCitadel extends PacketBase 
{
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) 
	{
	}

	@Override
	public void handleServerSide(EntityPlayerMP player) 
	{
		WarForgeMod.FACTIONS.requestMoveCitadel(player);
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer)
	{
		 //noop
	}

}
