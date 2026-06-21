package com.flansmod.warforge.common.mixins;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTarget.class)
public class RenderTargetStencilConfigMixin {

    @Redirect(
            method = "createBuffers",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;",
                    remap = false))
    private Object warforge$safeStencilConfig(ForgeConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException configNotLoaded) {
            return value.getDefault();
        }
    }
}
