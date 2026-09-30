package com.flansmod.warforge.common.mixins;

import com.flansmod.warforge.client.xaero.WorldMapClaimHighlighter;
import com.flansmod.warforge.client.xaero.XaeroRefresh;
import com.mojang.logging.LogUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.highlight.AbstractHighlighter;
import xaero.map.highlight.HighlighterRegistry;

/**
 * Adds the claim highlighter to Xaero's World Map. Its registry is built for every world session and made read-only
 * by end(), so the highlighter is added just before that. Applied only when xaeroworldmap is loaded
 * (CompatMixinPlugin); an error here is logged and never breaks the map.
 */
@Pseudo
@Mixin(value = HighlighterRegistry.class, remap = false)
public abstract class XaeroWorldMapHighlighterMixin {
    @Shadow
    public abstract void register(AbstractHighlighter highlighter);

    @Inject(method = "end", at = @At("HEAD"), require = 0)
    private void warforge$addClaimHighlighter(CallbackInfo ci) {
        try {
            register(new WorldMapClaimHighlighter());
            XaeroRefresh.listenForWorldMap();
        } catch (Throwable t) {
            LogUtils.getLogger().warn("Could not add the WarForge claims to Xaero's World Map", t);
        }
    }
}
