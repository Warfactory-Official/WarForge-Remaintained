package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.client.ClientBorderCache;
import com.flansmod.warforge.common.network.PacketClaimCacheState;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

// Jump into a replay chunk = snapshot + body up to target. Target == chunk start skips the body => state must be in the snapshot.
@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.Recorder", remap = false)
public abstract class FlashbackRecorderClaimMixin {
    @ModifyArg(method = "writeSnapshot", index = 1, require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lcom/moulberry/flashback/io/AsyncReplaySaver;writeGamePackets(Lnet/minecraft/network/codec/StreamCodec;Ljava/util/List;)V"))
    private List<Packet<? super ClientGamePacketListener>> warforge$addClaimState(List<Packet<? super ClientGamePacketListener>> packets) {
        for (PacketClaimCacheState state : ClientBorderCache.captureState()) {
            packets.add(new ClientboundCustomPayloadPacket(state));
        }
        return packets;
    }
}
