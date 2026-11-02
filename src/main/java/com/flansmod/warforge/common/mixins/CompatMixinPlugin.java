package com.flansmod.warforge.common.mixins;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// getMixins() registers only mixins whose modid is loaded: no missing-target warnings. LoadingModList is populated before Mixin prepares configs.
public class CompatMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, String> GATED_MIXINS = Map.of(
            "SbwExplosionClaimMixin", "superbwarfare",
            "AshVehicleExplosionClaimMixin", "ashvehicle",
            "XaeroWorldMapHighlighterMixin", "xaeroworldmap",
            "XaeroMinimapHighlighterMixin", "xaerominimap",
            "CarryOnPlacementMixin", "carryon"
    );

    private static final String PACKAGE = "com.flansmod.warforge.common.mixins.";

    private static boolean modPresent(String modId) {
        return LoadingModList.get() != null && LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();
        for (Map.Entry<String, String> entry : GATED_MIXINS.entrySet()) {
            if (modPresent(entry.getValue())) {
                mixins.add(entry.getKey());
            }
        }
        return mixins;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String modId = GATED_MIXINS.get(mixinClassName.substring(PACKAGE.length()));
        return modId == null || modPresent(modId);
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
