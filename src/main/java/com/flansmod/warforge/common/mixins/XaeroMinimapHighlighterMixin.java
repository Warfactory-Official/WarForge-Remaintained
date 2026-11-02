package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.client.xaero.MinimapClaimHighlighter;
import com.flansmod.warforge.client.xaero.XaeroRefresh;
import com.mojang.logging.LogUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.highlight.AbstractHighlighter;
import xaero.common.minimap.highlight.HighlighterRegistry;

// Minimap does not show World Map highlighters itself. Registry made read-only by end(): add just before. Errors logged, never break the minimap.
@Pseudo
@Mixin(value = HighlighterRegistry.class, remap = false)
public abstract class XaeroMinimapHighlighterMixin {
    @Shadow
    public abstract void register(AbstractHighlighter highlighter);

    @Inject(method = "end", at = @At("HEAD"), require = 0)
    private void warforge$addClaimHighlighter(CallbackInfo ci) {
        try {
            register(new MinimapClaimHighlighter());
            XaeroRefresh.listenForMinimap();
        } catch (Throwable t) {
            LogUtils.getLogger().warn("Could not add the WarForge claims to Xaero's Minimap", t);
        }
    }
}
