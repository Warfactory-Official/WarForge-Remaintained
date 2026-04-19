package com.flansmod.warforge.common.mixins.vanilla;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {
    @Inject(method = "handleTimeUpdate", at = @At("HEAD"))
    private void traceTimeUpdate(SPacketTimeUpdate packetIn, CallbackInfo ci) {
        if (!WarForgeMod.PACKET_DEBUG) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        System.out.println("[Warforge-Diag] Processing TimeUpdate. World Null? " + (mc.world == null));
        
        if (mc.world == null) {
            // This stack trace will show if the packet is being forced 
            // by a specific thread or the standard task queue.
            new Throwable("Zombie Packet Stacktrace: SPacketTimeUpdate called when world is null").printStackTrace();
        }
    }
}
