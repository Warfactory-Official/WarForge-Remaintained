package com.flansmod.warforge.platform;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

/** Access to a loaded config spec's live values. The loader keeps them in a private field. */
public final class ConfigAccess {
    private ConfigAccess() {}

    /**
     * Merges source into the spec's live config (with merge) and clears the cached values.
     * Returns false if the spec is not loaded yet.
     */
    public static boolean pushInto(ForgeConfigSpec spec, UnmodifiableConfig source,
                                   BiConsumer<UnmodifiableConfig, CommentedConfig> merge) throws ReflectiveOperationException {
        Field field = ForgeConfigSpec.class.getDeclaredField("childConfig");
        field.setAccessible(true);
        Object loaded = field.get(spec);
        if (!(loaded instanceof CommentedConfig config)) {
            return false;
        }
        merge.accept(source, config);
        spec.afterReload();
        return true;
    }
}
