package com.flansmod.warforge.common;

import sun.misc.Unsafe;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Makes a plain classpath library (e.g. snakeyaml) that Forge does not hand to the mod's module
 * loadable from this mod at runtime.
 *
 * <p>Forge's {@code cpw.mods.cl.ModuleClassLoader} resolves classes through a {@code parentLoaders}
 * map frozen at construction from the module read graph (so a library the mod does not
 * {@code requires} is unreachable) and otherwise falls back to the <em>platform</em> classloader
 * (which does not see the application classpath). We grab {@code sun.misc.Unsafe}, read the trusted
 * {@code MethodHandles.Lookup.IMPL_LOOKUP}, and use it to: wire in the owning named module if there
 * is one, and repoint the loader's fallback at the system classloader so libraries living on the
 * plain {@code -cp} resolve too.
 */
public final class ModuleBridge {
    private ModuleBridge() {}

    /** Ensures {@code probeClassName} (e.g. {@code org.yaml.snakeyaml.Yaml}) is loadable from this mod. */
    public static synchronized void bridge(String probeClassName) {
        String packageName = probeClassName.substring(0, probeClassName.lastIndexOf('.'));
        ClassLoader selfLoader = ModuleBridge.class.getClassLoader();
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);

            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.Lookup lookup = (MethodHandles.Lookup) unsafe.getObject(
                    unsafe.staticFieldBase(implLookupField), unsafe.staticFieldOffset(implLookupField));

            Module self = ModuleBridge.class.getModule();

            // (A) Module-path case: if a named module owns the package, open the graph + route its packages.
            Set<Module> candidates = new LinkedHashSet<>();
            collectModules(self.getLayer(), candidates, new LinkedHashSet<>());
            candidates.addAll(ModuleLayer.boot().modules());
            Module target = candidates.stream()
                    .filter(m -> m.getPackages().contains(packageName)).findFirst().orElse(null);
            Class<?> mcl = Class.forName("cpw.mods.cl.ModuleClassLoader");
            if (target != null) {
                MethodHandle addReads = lookup.findVirtual(Module.class, "implAddReads",
                        MethodType.methodType(void.class, Module.class));
                MethodHandle addExports = lookup.findVirtual(Module.class, "implAddExports",
                        MethodType.methodType(void.class, String.class, Module.class));
                MethodHandle addOpens = lookup.findVirtual(Module.class, "implAddOpens",
                        MethodType.methodType(void.class, String.class, Module.class));
                addReads.invoke(self, target);
                for (String pkg : target.getPackages()) {
                    addExports.invoke(target, pkg, self);
                    addOpens.invoke(target, pkg, self);
                }
                if (mcl.isInstance(selfLoader)) {
                    Field parentLoaders = mcl.getDeclaredField("parentLoaders");
                    @SuppressWarnings("unchecked")
                    Map<String, ClassLoader> map =
                            (Map<String, ClassLoader>) lookup.unreflectGetter(parentLoaders).invoke(selfLoader);
                    ClassLoader targetLoader = target.getClassLoader();
                    for (String pkg : target.getPackages()) map.putIfAbsent(pkg, targetLoader);
                }
            }

            // (B) Plain -cp case: route ONLY the target package to the system loader, via a narrow fallback
            //     that otherwise preserves the original fallback. A blanket system-loader fallback shadows
            //     properly-moduled libs (e.g. com.google.gson) with -cp duplicates -> loader-constraint errors.
            if (mcl.isInstance(selfLoader)) {
                Field fallbackField = mcl.getDeclaredField("fallbackClassLoader");
                ClassLoader original = (ClassLoader) lookup.unreflectGetter(fallbackField).invoke(selfLoader);
                final ClassLoader system = ClassLoader.getSystemClassLoader();
                final String prefix = packageName;
                ClassLoader narrow = new ClassLoader(original) {
                    @Override
                    protected Class<?> findClass(String name) throws ClassNotFoundException {
                        if (name.startsWith(prefix)) return Class.forName(name, false, system);
                        throw new ClassNotFoundException(name);
                    }
                };
                lookup.unreflectSetter(fallbackField).invoke(selfLoader, narrow);
            }

            // (C) Verify the class now resolves through the mod loader.
            Class.forName(probeClassName, false, selfLoader);
        } catch (Throwable t) {
            String cp = System.getProperty("java.class.path", "");
            String lcp = System.getProperty("legacyClassPath", "");
            throw new RuntimeException("Failed to bridge " + probeClassName
                    + "; java.class.path[snakeyaml]=" + filter(cp)
                    + "; legacyClassPath[snakeyaml]=" + filter(lcp)
                    + "; systemLoaderCanLoad=" + canLoad(ClassLoader.getSystemClassLoader(), probeClassName)
                    + "; platformLoaderCanLoad=" + canLoad(ClassLoader.getPlatformClassLoader(), probeClassName), t);
        }
    }

    private static String filter(String classpath) {
        return Arrays.stream(classpath.split(File.pathSeparator))
                .filter(e -> e.contains("snakeyaml")).collect(Collectors.joining(", ", "[", "]"));
    }

    private static boolean canLoad(ClassLoader loader, String name) {
        try { Class.forName(name, false, loader); return true; } catch (Throwable t) { return false; }
    }

    private static void collectModules(ModuleLayer layer, Set<Module> out, Set<ModuleLayer> seen) {
        if (layer == null || !seen.add(layer)) return;
        out.addAll(layer.modules());
        for (ModuleLayer parent : layer.parents()) collectModules(parent, out, seen);
    }
}
