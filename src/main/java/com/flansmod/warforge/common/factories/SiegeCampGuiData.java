package com.flansmod.warforge.common.factories;

import com.cleanroommc.modularui.factory.GuiData;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

public class SiegeCampGuiData extends GuiData {
    public final DimBlockPos siegeCampPos;
    public final List<SiegeCampAttackInfo> possibleAttacks;
    public final byte momentum;

    public SiegeCampGuiData(EntityPlayer player, DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks, byte momentum) {
        super(player);
        this.siegeCampPos = siegeCampPos;
        this.possibleAttacks = new ArrayList<SiegeCampAttackInfo>(possibleAttacks);
        this.momentum = momentum;
    }

    public PacketSiegeCampInfo toPacket() {
        PacketSiegeCampInfo packet = new PacketSiegeCampInfo();
        packet.mSiegeCampPos = siegeCampPos;
        packet.mPossibleAttacks.addAll(possibleAttacks);
        packet.momentum = momentum;
        return packet;
    }
}
