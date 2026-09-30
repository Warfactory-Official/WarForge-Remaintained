package com.flansmod.warforge.platform;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

/** Access to a loaded config spec's live values. The loader keeps them in a private field. */
public final class ConfigAccess {
    private ConfigAccess() {}

    /**
     * Merges source into the spec's live config (with merge) and clears the cached values.
     * Returns false if the spec is not loaded yet.
     */
    public static boolean pushInto(ModConfigSpec spec, UnmodifiableConfig source,
                                   BiConsumer<UnmodifiableConfig, CommentedConfig> merge) throws ReflectiveOperationException {
        Field field = ModConfigSpec.class.getDeclaredField("loadedConfig");
        field.setAccessible(true);
        Object loaded = field.get(spec);
        if (loaded == null) {
            return false;
        }
        merge.accept(source, ((IConfigSpec.ILoadedConfig) loaded).config());
        spec.resetCaches(ModConfigSpec.RestartType.NONE);
        return true;
    }
}
