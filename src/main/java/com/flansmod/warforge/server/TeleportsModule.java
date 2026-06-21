package com.flansmod.warforge.server;

import java.util.ArrayList;
import java.util.List;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class TeleportsModule
{
	private class PendingTeleport
	{
		public Player player;
		public int ticksRemaining;
		public BlockPos pos;

		public DimBlockPos target;
	}


	private List<PendingTeleport> mPendingTPs = new ArrayList<PendingTeleport>();

	public void requestSpawn(Player player)
	{
		if(!WarForgeConfig.ENABLE_SPAWN_COMMAND)
		{
			player.sendSystemMessage(Component.literal("/f spawn is disabled on this server"));
			return;
		}

		if(!WarForgeConfig.ALLOW_SPAWN_BETWEEN_DIMENSIONS && player.level().dimension() != Level.OVERWORLD)
		{
			player.sendSystemMessage(Component.literal("You need to be in the overworld"));
			return;
		}

		ServerLevel overworld = WarForgeMod.MC_SERVER.overworld();
		DimBlockPos target = new DimBlockPos(Level.OVERWORLD, overworld.getSharedSpawnPos());
		PendingTeleport tp = new PendingTeleport();
		tp.player = player;
		tp.pos = player.blockPosition();
		tp.target = target;
		tp.ticksRemaining = WarForgeConfig.NUM_TICKS_FOR_WARP_COMMANDS;
		mPendingTPs.add(tp);

		player.sendSystemMessage(Component.literal("Teleport started. Stand still."));

	}

	public void RequestFHome(Player player)
	{
		if(!WarForgeConfig.ENABLE_F_HOME_COMMAND)
		{
			player.sendSystemMessage(Component.literal("/f home is disabled on this server"));
			return;
		}

		Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUUID());
		if(faction == null)
		{
			player.sendSystemMessage(Component.literal("You are not in a faction"));
			return;
		}

		DimBlockPos target = faction.citadelPos;
		if(!WarForgeConfig.ALLOW_F_HOME_BETWEEN_DIMENSIONS && target.dim != player.level().dimension())
		{
			player.sendSystemMessage(Component.literal("You need to be in the same dimension as your citadel"));
			return;
		}

		PendingTeleport tp = new PendingTeleport();
		tp.player = player;
		tp.pos = player.blockPosition();
		tp.target = target;
		tp.ticksRemaining = WarForgeConfig.NUM_TICKS_FOR_WARP_COMMANDS;
		mPendingTPs.add(tp);

		player.sendSystemMessage(Component.literal("Teleport started. Stand still."));
	}

	public void update()
	{
		for(int i = mPendingTPs.size() - 1; i >= 0; i--)
		{
			PendingTeleport tp = mPendingTPs.get(i);
			if(!tp.player.blockPosition().equals(tp.pos))
			{
				tp.player.sendSystemMessage(Component.literal("Teleport cancelled."));
				mPendingTPs.remove(i);
			}

			if(tp.ticksRemaining % 20 == 0)
			{
				tp.player.sendSystemMessage(Component.literal("Teleporting in " + (tp.ticksRemaining / 20)));
			}

			tp.ticksRemaining--;

			if(tp.ticksRemaining == 0)
			{
				TeleportUtil.teleportPlayer( (ServerPlayer)tp.player, tp.target.dim, tp.target.toRegularPos());
			}
		}
	}
}
