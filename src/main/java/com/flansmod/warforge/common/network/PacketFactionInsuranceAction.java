package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class PacketFactionInsuranceAction extends PacketBase {
    public int slot = -1;

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(slot);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        slot = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(playerEntity.getUniqueID());
        if (faction == null) {
            return;
        }
        if (!WarForgeMod.isOp(playerEntity) && !faction.isPlayerRoleInFaction(playerEntity.getUniqueID(), Faction.Role.LEADER)) {
            playerEntity.sendMessage(new net.minecraft.util.text.TextComponentString("Only the faction leader can void insurance items"));
            return;
        }
        if (slot < 0 || slot >= faction.getInsuranceSlotCount()) {
            return;
        }
        ItemStack existing = faction.getInsuranceStack(slot);
        if (!existing.isEmpty()) {
            faction.setInsuranceStack(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
    }
}
