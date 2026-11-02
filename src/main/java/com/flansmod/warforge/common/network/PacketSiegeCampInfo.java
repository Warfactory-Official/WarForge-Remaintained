package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.SyncedVeins;
import com.flansmod.warforge.common.faction.ClaimType;

import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import com.flansmod.warforge.compat.Keys;

public class PacketSiegeCampInfo extends PacketBase {
    public static PacketSiegeCampInfo lastReceived = null;

    public DimBlockPos siegeCampPos;
    public List<SiegeCampAttackInfo> possibleAttacks = new ArrayList<>();
    public byte momentum;
    public int color;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(Keys.id(siegeCampPos.dim).toString());
        data.writeInt(siegeCampPos.getX());
        data.writeInt(siegeCampPos.getY());
        data.writeInt(siegeCampPos.getZ());

        data.writeByte(possibleAttacks.size());

        for (SiegeCampAttackInfo info : possibleAttacks) {
            data.writeBoolean(info.canAttack);
            writeUUID(data, info.factionUuid);
            writeUTF(data, info.factionName);
            data.writeByte(info.offset.getX());
            data.writeByte(info.offset.getZ());
            data.writeInt(info.factionColour);
            data.writeShort(info.warforgeVein != null ? info.warforgeVein.getId() : -1);
            writeUTF(data, info.claimType.serializedName);

            byte oreQualOrd = 0;
            if (info.oreQuality != null) { oreQualOrd = (byte) info.oreQuality.ordinal(); }
            data.writeByte(oreQualOrd);

            data.writeBoolean(info.conquered);
        }

        data.writeByte(momentum);
        data.writeInt(color);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        siegeCampPos = new DimBlockPos(dim, x, y, z);

        int numAttacks = PacketSecurity.clampCount(data.readByte(), 64);
        possibleAttacks.clear();
        for (int i = 0; i < numAttacks; i++) {
            SiegeCampAttackInfo info = new SiegeCampAttackInfo();

            info.canAttack = data.readBoolean();
            info.factionUuid = readUUID(data);
            info.factionName = readUTF(data, PacketSecurity.MAX_NAME);
            int dx = data.readByte();
            int dz = data.readByte();
            info.offset = new Vec3i(dx, 0, dz);
            info.factionColour = data.readInt();

            short possibleVein = data.readShort();
            info.warforgeVein = possibleVein < 0 ? null : SyncedVeins.ENTRIES.get(possibleVein);
            info.claimType = ClaimType.fromSerialized(readUTF(data, PacketSecurity.MAX_NAME));
            info.oreQuality = PacketSecurity.readEnum(data, Quality.values(), Quality.values()[0]);
            info.conquered = data.readBoolean();

            possibleAttacks.add(info);
        }
        momentum = data.readByte();
        color = data.readInt();

    }

    /** Sends the siege camp UI data to the player. */
    public static void sendTo(net.minecraft.world.entity.player.Player player, DimBlockPos siegeCampPos,
                              List<SiegeCampAttackInfo> possibleAttacks, byte momentum, int color) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PacketSiegeCampInfo packet = new PacketSiegeCampInfo();
        packet.siegeCampPos = siegeCampPos;
        packet.possibleAttacks.addAll(possibleAttacks);
        packet.momentum = momentum;
        packet.color = color;
        WarForgeMod.NETWORK.sendTo(packet, serverPlayer);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        PacketSecurity.violation(playerEntity, "Received a siege info packet server side");
    }
}
