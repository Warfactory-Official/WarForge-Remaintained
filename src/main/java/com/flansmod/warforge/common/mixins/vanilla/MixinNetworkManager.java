package com.flansmod.warforge.common.mixins.vanilla;

import com.flansmod.warforge.common.WarForgeMod;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(method = "closeChannel", at = @At("HEAD"))
    private void traceCloseChannel(ITextComponent message, CallbackInfo ci) {
        if (!WarForgeMod.PACKET_DEBUG) return;
        Minecraft mc = Minecraft.getMinecraft();
        System.out.println("[Warforge-Diag] NetworkManager.closeChannel() called.");
        System.out.println("[Warforge-Diag] World is currently: " + (mc.world == null ? "NULL" : "EXISTS"));
        
        // Print the stack trace so you know exactly which class triggered the disconnect
        Thread.dumpStack();
    }

    @Inject(method = "channelActive", at = @At("RETURN"))
    private void onChannelActive(ChannelHandlerContext p_channelActive_1_, CallbackInfo ci) {
        if (!WarForgeMod.PACKET_DEBUG) return;
        NetworkManager manager = (NetworkManager) (Object) this;
        
        // Add our spy to the very end of the pipeline
        manager.channel().pipeline().addLast("warforge_spy", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                if (WarForgeMod.PACKET_DEBUG) {
                    System.out.println("[Warforge-Diag] NETTY SOCKET OFFICIALLY INACTIVE (Dead)");
                }
                super.channelInactive(ctx);
            }
        });
    }
}
