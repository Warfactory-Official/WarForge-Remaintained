package com.flansmod.warforge.common.network;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.client.ClientProxy;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class PacketSiegeCampInfo extends PacketBase {
    public DimBlockPos mSiegeCampPos;
    public List<SiegeCampAttackInfo> mPossibleAttacks = new ArrayList<>();
    public byte momentum;
    public int color;

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(mSiegeCampPos.dim);
        data.writeInt(mSiegeCampPos.getX());
        data.writeInt(mSiegeCampPos.getY());
        data.writeInt(mSiegeCampPos.getZ());

        data.writeByte(mPossibleAttacks.size());
      
        for (SiegeCampAttackInfo info : mPossibleAttacks) {
            data.writeBoolean(info.canAttack);
            writeUUID(data, info.mFactionUUID);
            writeUTF(data, info.mFactionName);
            data.writeByte(info.mOffset.getX());
            data.writeByte(info.mOffset.getZ());
            data.writeInt(info.mFactionColour);
            data.writeShort(info.mWarforgeVein != null ? info.mWarforgeVein.getId() : -1);
            writeUTF(data, info.claimType.serializedName);

            byte oreQualOrd = 0;
            if (info.mOreQuality != null) { oreQualOrd = (byte) info.mOreQuality.ordinal(); }
            data.writeByte(oreQualOrd);
        }

        data.writeByte(momentum);
        data.writeInt(color);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        int dim = data.readInt();
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        mSiegeCampPos = new DimBlockPos(dim, x, y, z);

        int numAttacks = data.readByte();
        mPossibleAttacks.clear();
        for (int i = 0; i < numAttacks; i++) {
            SiegeCampAttackInfo info = new SiegeCampAttackInfo();

            info.canAttack = data.readBoolean();
            info.mFactionUUID = readUUID(data);
            info.mFactionName = readUTF(data);
            int dx = data.readByte();
            int dz = data.readByte();
            info.mOffset = new Vec3i(dx, 0, dz);
            info.mFactionColour = data.readInt();

            short possibleVein = data.readShort();
            info.mWarforgeVein = possibleVein < 0 ? null : ClientProxy.VEIN_ENTRIES.get(possibleVein);
            info.claimType = Faction.ClaimType.fromSerialized(readUTF(data));
            info.mOreQuality = Quality.values()[data.readByte()];

            mPossibleAttacks.add(info);
        }
        momentum = data.readByte();
        color = data.readInt();

    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        WarForgeMod.LOGGER.error("Received a siege info packet server side");
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        WarForgeMod.LOGGER.warn("Ignoring legacy PacketSiegeCampInfo on the client. Siege camp UI now opens through the synced ModularUI factory path.");
    }
}
