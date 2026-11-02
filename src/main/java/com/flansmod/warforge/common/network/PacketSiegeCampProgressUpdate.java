package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.compat.Keys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class PacketSiegeCampProgressUpdate extends PacketBase {
    /** Drops every siege the client remembers; {@link #info} is absent. */
    public boolean clearAll;
    public SiegeCampProgressInfo info;
    public long msServerNow = 0L;

    public static PacketSiegeCampProgressUpdate clearPacket() {
        PacketSiegeCampProgressUpdate packet = new PacketSiegeCampProgressUpdate();
        packet.clearAll = true;
        return packet;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeBoolean(clearAll);
        if (clearAll) {
            return;
        }
        msServerNow = WarClock.siegeClock();
        data.writeUtf(Keys.id(info.attackingPos.dim).toString());
        data.writeInt(info.attackingPos.getX());
        data.writeInt(info.attackingPos.getY());
        data.writeInt(info.attackingPos.getZ());
        data.writeInt(info.attackingColour);
        writeUTF(data, info.attackingName);

        data.writeUtf(Keys.id(info.defendingPos.dim).toString());
        data.writeInt(info.defendingPos.getX());
        data.writeInt(info.defendingPos.getY());
        data.writeInt(info.defendingPos.getZ());
        data.writeInt(info.defendingColour);
        writeUTF(data, info.defendingName);
        data.writeInt(info.battleRadius);
        data.writeInt(info.siegedRadius);
        data.writeUUID(info.defendingFactionId);
        data.writeUUID(info.attackingFactionId);
        data.writeInt(info.attackerAbandonSeconds);

        data.writeInt(info.progress);
        data.writeInt(info.previousProgress);
        data.writeInt(info.completionPoint);

        data.writeLong(info.timeProgress);
        data.writeLong(info.endTimestamp);
        data.writeBoolean(info.finished);
        data.writeLong(msServerNow);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        clearAll = data.readBoolean();
        if (clearAll) {
            return;
        }
        info = new SiegeCampProgressInfo();

        ResourceKey<Level> attackingDim = PacketSecurity.readDimOrOverworld(data);
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        info.attackingPos = new DimBlockPos(attackingDim, x, y, z);
        info.attackingColour = data.readInt();
        info.attackingName = readUTF(data);

        ResourceKey<Level> defendingDim = PacketSecurity.readDimOrOverworld(data);
        x = data.readInt();
        y = data.readInt();
        z = data.readInt();
        info.defendingPos = new DimBlockPos(defendingDim, x, y, z);
        info.defendingColour = data.readInt();
        info.defendingName = readUTF(data);
        info.battleRadius = data.readInt();
        info.siegedRadius = data.readInt();
        info.defendingFactionId = data.readUUID();
        info.attackingFactionId = data.readUUID();
        info.attackerAbandonSeconds = data.readInt();

        info.progress = data.readInt();
        info.previousProgress = data.readInt();
        info.completionPoint = data.readInt();

        info.timeProgress = data.readLong();
        info.endTimestamp = data.readLong();
        info.finished = data.readBoolean();
        msServerNow = data.readLong();
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        PacketSecurity.violation(playerEntity, "Received siege info on server");
    }
}
