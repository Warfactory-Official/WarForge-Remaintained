package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

public class PacketMoveCitadel extends PacketBase
{
	public DimBlockPos pos = DimBlockPos.ZERO;

	@Override
	public void encodeInto(FriendlyByteBuf data)
	{
		data.writeUtf(Keys.id(pos.dim).toString());
		data.writeInt(pos.getX());
		data.writeInt(pos.getY());
		data.writeInt(pos.getZ());
	}

	@Override
	public void decodeInto(FriendlyByteBuf data)
	{
		ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
		pos = new DimBlockPos(dim, data.readInt(), data.readInt(), data.readInt());
	}

	@Override
	public void handleServerSide(ServerPlayer player)
	{
		// The GUI's "Move Citadel" button sends an empty packet to prompt the player to look at a
		// block; requestMoveCitadel answers that with an instruction message. A real destination is
		// ray-picked within 10 blocks, so anything further away did not come from the client UI.
		if (!pos.equals(DimBlockPos.ZERO) && !PacketSecurity.requireReach(player, pos, "move citadel"))
			return;

		WarForgeMod.FACTIONS.requestMoveCitadel(player, pos);
	}

	@Override
	public void handleClientSide(Player clientPlayer)
	{

	}

}
