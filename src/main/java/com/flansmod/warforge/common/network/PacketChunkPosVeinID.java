package com.flansmod.warforge.common.network;


import org.apache.commons.lang3.tuple.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.VeinCodec;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import static com.flansmod.warforge.common.WarForgeMod.VEIN_HANDLER;
import com.flansmod.warforge.compat.Keys;

public class PacketChunkPosVeinID extends ServerboundPacket {
    public DimChunkPos veinLocation = null;
    public short resultInfo = VeinCodec.NULL_VEIN_ID;

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        if (veinLocation == null) {
            WarForgeMod.LOGGER.error("ChunkPosVeinID packet encoded without a vein location");
            return;
        }

        data.writeUtf(Keys.id(veinLocation.dim).toString());
        data.writeInt(veinLocation.x);
        data.writeInt(veinLocation.z);
        data.writeShort(resultInfo);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        try {
            ResourceKey<Level> dim = PacketSecurity.readDimOrOverworld(data);
            veinLocation = new DimChunkPos(dim, data.readInt(), data.readInt());
            resultInfo = data.readShort();
        } catch (RuntimeException e) {
            veinLocation = null;
            resultInfo = VeinCodec.NULL_VEIN_ID;
        }
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
        if (veinLocation == null) {
            PacketSecurity.violation(playerEntity, "vein query with no position");
            return;
        }

        long seed = ((ServerLevel) playerEntity.level()).getSeed();
        Pair<Vein, Quality> veinInfo = VEIN_HANDLER.getVein(veinLocation.dim, veinLocation.x, veinLocation.z, seed);
        resultInfo = VeinCodec.compressVeinInfo(veinInfo);
        WarForgeMod.NETWORK.sendTo(this, playerEntity);
    }

    @Override
    public DimChunkPos target() {
        return veinLocation;
    }
}
