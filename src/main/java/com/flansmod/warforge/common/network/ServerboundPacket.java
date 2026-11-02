package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.util.DimChunkPos;

import javax.annotation.Nullable;

/**
 * A packet a client may send. Only subclasses can be registered as serverbound in {@link WFNetwork}, and
 * registration demands an {@link Exposure}.
 */
public abstract class ServerboundPacket extends PacketBase {
    /** The chunk this packet addresses; required for {@link Exposure.Kind#IN_RANGE}. */
    @Nullable
    public DimChunkPos target() {
        return null;
    }
}
