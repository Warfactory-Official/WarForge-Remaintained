package com.flansmod.warforge.common.mixins;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

// Gates mixins.warforge.sbw.json on Superb Warfare actually being present at runtime. LoadingModList is
// already populated when Mixin prepares configs, so getMixins() returns the SBW mixin only when the mod
// is installed; otherwise nothing is registered and no missing-target warnings are logged.
public class SbwMixinPlugin implements IMixinConfigPlugin {

    private static final boolean SBW_PRESENT =
            LoadingModList.get() != null && LoadingModList.get().getModFileById("superbwarfare") != null;

    @Override
    public List<String> getMixins() {
        return SBW_PRESENT ? Collections.singletonList("SbwExplosionClaimMixin") : Collections.emptyList();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return SBW_PRESENT;
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
