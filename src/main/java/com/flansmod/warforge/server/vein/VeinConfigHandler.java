package com.flansmod.warforge.server.vein;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.VeinCodec;
import com.flansmod.warforge.api.vein.VeinEntry;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.platform.Platform;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.flansmod.warforge.api.vein.VeinCodec.NULL_VEIN_ID;

public class VeinConfigHandler {
    private record Tuple5<A, B, C, D, E>(A _1, B _2, C _3, D _4, E _5) {}

    private static Path configPath() {
        return Platform.configDir().resolve(Tags.MODID).resolve("veins.toml");
    }

    public static final List<String> EXAMPLE_TOML = Collections.unmodifiableList(Arrays.asList(
            "# It should be said that it is RECOMMENDED to BACKUP ANY vein config files with significant time invested into them; ",
            "# trying to update the file when auto-generated vein ids are present may cause data loss if a poorly timed error occurs.",
            "# Additionally, all comments will be removed when auto-generated vein ids are used.",
            "#",
            "# There must be a global iteration id [-32768, 32767] ('iteration'). This is stored alongside the vein id for discovered chunks",
            "# and will cause the saved vein id to be overridden if the iteration id's don't match between the stored and current value.",
            "#",
            "# There must be a global megachunk_length which is the side length [4, 180] of the regions in which the weight minimums are respected.",
            "# There is no guarantee of veins occurring at least once, but they are guaranteed floor(megachunk_length^2 * weight)",
            "#",
            "# An OPTIONAL global distance gradient thins veins out the farther a chunk is from a center point (all coordinates in blocks):",
            "# - gradient_radius: square (Chebyshev) radius in blocks. The falloff fills [0, radius], reaching zero density at the edge, and",
            "#       chunks at or beyond it receive no veins. This is a hard cap that pairs with a square world border (radius = border_size / 2).",
            "#       A value <= 0 (the default) disables the gradient entirely, leaving vein density uniform everywhere.",
            "# - gradient_frequency: curve shape of the falloff (default 1.0). 1.0 is linear; > 1.0 keeps the center dense with a sharp dropoff",
            "#       near the edge; < 1.0 drops off quickly near the center then flattens out. Ignored when the gradient is disabled.",
            "# - gradient_center_x / gradient_center_z: block coordinates of the gradient center (both default 0).",
            "#",
            "# Example vein definition format (TOML). Each vein is an entry in the [[veins]] array of tables:",
            "# - id: An auto-generated unique identifier [0, 8191] which allows for vein properties to change;",
            "#       OMIT this key entirely to have one assigned automatically.",
            "# - key: A unique translation key used for localization or identification.",
            "# - wealth: An integer (default 1) added to a faction's wealth for each claimed chunk holding this vein.",
            "# - quals: An inline table of quality overrides for this vein, with omitted qualities using the global default in cfg",
            "#       quals = { <Qual Name> = <float multiplier>, ... }",
            "# - dims: An array of inline tables of dimension weights, where:",
            "#       id: The dimension ResourceLocation (e.g. minecraft:overworld, minecraft:the_nether, minecraft:the_end, or a modded dim).",
            "#       weight: A float between 0.0 and 1.0 indicating the relative generation chance in that dimension.",
            "#       mult: A float between 0.0 and 1.0 which scales the yield of a vein based on the dimension; omission => 1.",
            "# - components: An array of inline tables of ore components for the vein, where:",
            "#       item: The item ID (e.g. minecraft:iron_ore) to generate in the vein.",
            "#       yield: How many of this item the vein yields when selected. [float w/ decimal value contributed to bonus chance]",
            "#       weights: An array of { id = <dimension ResourceLocation>, weight = <float 0..1> } tables (omitted dimensions assume a weight of 1.0)",
            "#       mults: An array of { id = <dimension ResourceLocation>, mult = <float >= 0.0> } tables (omitted dimensions use the vein dim multiplier)",
            "#     NOTE: If component weights are omitted or empty, all are assumed to have a weight of 1.0.",
            "#",
            "# All fields are mandatory unless otherwise specified.",
            "#",
            "# iteration = 0",
            "# megachunk_length = 32",
            "# gradient_center_x = 0",
            "# gradient_center_z = 0",
            "# gradient_radius = 0",
            "# gradient_frequency = 1.0",
            "#",
            "# [[veins]]",
            "# key = \"warforge.veins.iron_mix\"",
            "# wealth = 1",
            "# quals = { RICH = 10.0, POOR = 0.1 }",
            "# dims = [",
            "#     { id = \"minecraft:the_nether\", weight = 0.5, mult = 2.0 },",
            "#     { id = \"minecraft:overworld\", weight = 0.4215 },",
            "#     { id = \"minecraft:the_end\", weight = 1.0 },",
            "# ]",
            "# components = [",
            "#     { item = \"minecraft:iron_ore\", yield = 2.0, weights = [ { id = \"minecraft:the_nether\", weight = 0.75 }, { id = \"minecraft:overworld\", weight = 1.0 } ] },",
            "#     { item = \"minecraft:coal_ore\", yield = 1.0, mults = [ { id = \"minecraft:the_nether\", mult = 4.0 } ] },",
            "# ]",
            "#..."
    ));

    // Explicit ids: auto-assigned ids rewrite the file and strip its comments.
    public static final List<String> DEFAULT_VEINS_TOML = Collections.unmodifiableList(Arrays.asList(
            "iteration = 0",
            "megachunk_length = 32",
            "gradient_center_x = 0",
            "gradient_center_z = 0",
            "gradient_radius = 0",
            "gradient_frequency = 1.0",
            "",
            "[[veins]]",
            "id = 0",
            "key = \"warforge.veins.iron\"",
            "wealth = 1",
            "dims = [ { id = \"minecraft:overworld\", weight = 0.15 } ]",
            "components = [",
            "    { item = \"minecraft:coal_ore\", yield = 3.0 },",
            "    { item = \"minecraft:iron_ore\", yield = 2.0 },",
            "]",
            "",
            "[[veins]]",
            "id = 1",
            "key = \"warforge.veins.copper\"",
            "wealth = 1",
            "dims = [ { id = \"minecraft:overworld\", weight = 0.12 } ]",
            "components = [",
            "    { item = \"minecraft:copper_ore\", yield = 3.0 },",
            "]",
            "",
            "[[veins]]",
            "id = 2",
            "key = \"warforge.veins.redstone_lapis\"",
            "wealth = 1",
            "dims = [ { id = \"minecraft:overworld\", weight = 0.10 } ]",
            "components = [",
            "    { item = \"minecraft:redstone_ore\", yield = 2.0 },",
            "    { item = \"minecraft:lapis_ore\", yield = 1.5 },",
            "]",
            "",
            "[[veins]]",
            "id = 3",
            "key = \"warforge.veins.gold\"",
            "wealth = 2",
            "dims = [ { id = \"minecraft:overworld\", weight = 0.08 } ]",
            "components = [",
            "    { item = \"minecraft:gold_ore\", yield = 1.5 },",
            "]",
            "",
            "[[veins]]",
            "id = 4",
            "key = \"warforge.veins.gold_diamond\"",
            "wealth = 4",
            "dims = [ { id = \"minecraft:overworld\", weight = 0.04 } ]",
            "components = [",
            "    { item = \"minecraft:gold_ore\", yield = 1.0 },",
            "    { item = \"minecraft:diamond_ore\", yield = 0.5 },",
            "]",
            "",
            "[[veins]]",
            "id = 5",
            "key = \"warforge.veins.emerald\"",
            "wealth = 3",
            "dims = [ { id = \"minecraft:overworld\", weight = 0.03 } ]",
            "components = [",
            "    { item = \"minecraft:emerald_ore\", yield = 0.75 },",
            "]",
            "",
            "[[veins]]",
            "id = 6",
            "key = \"warforge.veins.quartz\"",
            "wealth = 1",
            "dims = [ { id = \"minecraft:the_nether\", weight = 0.25 } ]",
            "components = [",
            "    { item = \"minecraft:nether_quartz_ore\", yield = 4.0 },",
            "]"
    ));

    public static void writeStubIfEmpty() throws IOException {
        Path config = configPath();
        if (Files.notExists(config) || Files.size(config) == 0) {
            Files.createDirectories(config.getParent());
            List<String> stub = new ArrayList<>(EXAMPLE_TOML);
            stub.add("");
            stub.addAll(DEFAULT_VEINS_TOML);
            Files.write(config, stub, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
    }

    /** Parses veins.toml into a fresh handler; the caller publishes it. Call after {@link #writeStubIfEmpty()}. */
    public static VeinUtils loadVeins() throws IOException {
        CommentedConfig globalVeinData;
        try (Reader reader = Files.newBufferedReader(configPath(), StandardCharsets.UTF_8)) {
            globalVeinData = new TomlParser().parse(reader);
        }

        VeinUtils handler = newHandler(globalVeinData);
        List<Config> allVeinData = globalVeinData.get("veins");
        if (allVeinData == null) {
            handler.populateVeinMap(null);
            return handler;
        }

        List<VeinEntry> entries = new ArrayList<>();
        Int2ObjectOpenHashMap<Tuple5<String, Object2FloatOpenHashMap<Quality>, Object2ObjectOpenHashMap<ResourceKey<Level>, VeinEntry.DimWeight>, List<VeinEntry.Component>, Integer>> noIdEntries = new Int2ObjectOpenHashMap<>();

        short[] occupiedIds = new short[allVeinData.size()];
        int numIds = parseVeinEntries(allVeinData, entries, noIdEntries, occupiedIds);

        occupiedIds = Arrays.copyOf(occupiedIds, numIds);
        Arrays.sort(occupiedIds);
        if (occupiedIds.length == 0) { occupiedIds = new short[]{NULL_VEIN_ID}; }  // minimum => all ids valid

        // idSpaces: {index of the occupied id to follow, number of open ids after it}
        short[] idSpaces = new short[]{-1, occupiedIds[0]};
        short currIdOffset = 1;

        Int2ShortOpenHashMap posToId = new Int2ShortOpenHashMap(noIdEntries.size());
        for (var noIdEntry : noIdEntries.int2ObjectEntrySet()) {
            int noIdEntryOGIndex = noIdEntry.getIntKey();
            var noIdEntryVal = noIdEntry.getValue();

            if (currIdOffset > idSpaces[1]) {
                idSpaces = locateNextSpace(occupiedIds, idSpaces[0] + 1);
                currIdOffset = 1;
                if (idSpaces[1] < 1) {
                    throw new IllegalStateException("Ran out of id spaces to assign to veins.");
                }
            }

            short id = (short) (occupiedIds[idSpaces[0]] + currIdOffset++);
            entries.add(new VeinEntry(id, noIdEntryVal._1(), noIdEntryVal._2(), noIdEntryVal._3(), noIdEntryVal._4(), noIdEntryVal._5()));
            posToId.put(noIdEntryOGIndex, id);
        }

        if (!noIdEntries.isEmpty()) {
            for (int noIDVeinIndex : noIdEntries.keySet()) {
                allVeinData.get(noIDVeinIndex).set("id", (int) posToId.get(noIDVeinIndex));
            }
            try {
                writeBack(globalVeinData);
            } catch (IOException e) {
                WarForgeMod.LOGGER.error("Could not write back to vein file; ids will not be updated.", e);
            }
        }

        handler.populateVeinMap(entries);
        return handler;
    }

    /** Comment block then TOML, via temp file + atomic move; all comments in the file are replaced. */
    private static void writeBack(CommentedConfig data) throws IOException {
        StringWriter out = new StringWriter();
        out.write(String.join("\n", EXAMPLE_TOML));
        out.write("\n\n");
        new TomlWriter().write(data, out);

        Path config = configPath();
        Path temp = config.resolveSibling(config.getFileName() + ".tmp");
        Files.writeString(temp, out.toString(), StandardCharsets.UTF_8);
        Files.move(temp, config, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static VeinUtils newHandler(CommentedConfig globalVeinData) {
        short iterationId = number(globalVeinData, "iteration", 0).shortValue();

        short megachunkLength = number(globalVeinData, "megachunk_length", 32).shortValue();
        if (megachunkLength < 4 || megachunkLength > 180) {
            WarForgeMod.LOGGER.error("Invalid megachunk length provided; defaulting to 32");
            megachunkLength = 32;
        }

        double gradientCenterX = number(globalVeinData, "gradient_center_x", 0.0).doubleValue();
        double gradientCenterZ = number(globalVeinData, "gradient_center_z", 0.0).doubleValue();
        double gradientRadius = Math.max(0.0, number(globalVeinData, "gradient_radius", 0.0).doubleValue());

        double gradientFrequency = number(globalVeinData, "gradient_frequency", 1.0).doubleValue();
        if (gradientFrequency < 0.0) {
            WarForgeMod.LOGGER.error("Invalid gradient_frequency provided; defaulting to 1.0");
            gradientFrequency = 1.0;
        }
        return new VeinUtils(iterationId, megachunkLength, gradientCenterX, gradientCenterZ, gradientRadius, gradientFrequency);
    }

    /** A stub config holds only the example comment block, so globals can be absent. */
    private static Number number(CommentedConfig config, String key, Number fallback) {
        Number value = config.get(key);
        return value != null ? value : fallback;
    }

    public static VeinUtils emptyHandler() {
        return new VeinUtils((short) 0, (short) 32);
    }

    /** Returns the number of explicit ids found. */
    private static int parseVeinEntries(List<Config> rawVeins, List<VeinEntry> entries,
                                        Int2ObjectOpenHashMap<Tuple5<String, Object2FloatOpenHashMap<Quality>,
                                            Object2ObjectOpenHashMap<ResourceKey<Level>, VeinEntry.DimWeight>, List<VeinEntry.Component>, Integer>> noIdEntries,
                                        short[] occupiedIds) {
        int numIds = 0;
        int veinIndex = -1;
        for (Config veinData : rawVeins) {
            ++veinIndex;
            try {
                short absoluteId = NULL_VEIN_ID;  // missing or out of [0, 8192) => auto-assigned
                Number idNum = veinData.get("id");
                if (idNum != null && idNum.shortValue() >= 0 && idNum.shortValue() < 8192) { absoluteId = idNum.shortValue(); }

                String translationKey = veinData.get("key");
                Config qualsRaw = veinData.get("quals");

                Object2FloatOpenHashMap<Quality> quals = null;  // null => no overrides
                if (qualsRaw != null) {
                    quals = new Object2FloatOpenHashMap<>(Quality.values().length);
                    for (Quality qual : Quality.values()) {
                        if (!qualsRaw.contains(qual.toString())) { continue; }
                        quals.put(qual, ((Number) qualsRaw.get(qual.toString())).floatValue());
                    }
                }

                List<Config> dimsRaw = veinData.get("dims");

                Object2ObjectOpenHashMap<ResourceKey<Level>, VeinEntry.DimWeight> dims = new Object2ObjectOpenHashMap<>(dimsRaw.size());
                for (Config dimRaw : dimsRaw) {
                    float multiplier = 1;
                    if (dimRaw.contains("mult")) { multiplier = ((Number) dimRaw.get("mult")).floatValue(); }
                    ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION,
                            Ids.parse((String) dimRaw.get("id")));
                    dims.put(dimKey, new VeinEntry.DimWeight(dimKey,
                            VeinCodec.percentToShort(((Number) dimRaw.get("weight")).floatValue()),
                            multiplier));
                }

                List<Config> componentsRaw = veinData.get("components");
                List<VeinEntry.Component> components = componentsRaw.stream()
                        .map(comp -> new VeinEntry.Component(
                            comp.get("item"),
                            ((Number) comp.get("yield")).floatValue(),
                            VeinEntry.Component.parseMapF2S(comp.get("weights")),
                            VeinEntry.Component.parseFloatMap(comp.get("mults"))))
                        .collect(Collectors.toList());

                Number wealthNum = veinData.get("wealth");
                int wealth = wealthNum != null ? wealthNum.intValue() : 1;

                if (absoluteId == NULL_VEIN_ID) {
                    noIdEntries.put(veinIndex, new Tuple5<>(translationKey, quals, dims, components, wealth));
                    continue;
                }

                entries.add(new VeinEntry(absoluteId, translationKey, quals, dims, components, wealth));
                occupiedIds[numIds++] = absoluteId;
            } catch (ClassCastException e) {
                WarForgeMod.LOGGER.error("Failed to parse vein {}", veinIndex, e);
            }
        }

        return numIds;
    }

    /** {index of the first int with a gap after it, size of that gap}; past the end, the gap runs to the max id. */
    public static short[] locateNextSpace(short[] sparseInts, int currIndex) {
        for (; currIndex < sparseInts.length - 1; ++currIndex) {
            if (sparseInts[currIndex] < sparseInts[currIndex + 1] - 1) {
                return new short[]{(short) currIndex, (short) (sparseInts[currIndex + 1] - sparseInts[currIndex] - 1)};
            }
        }

        return new short[]{(short) currIndex, (short) (0x00_00_1F_FF - sparseInts[currIndex])};
    }
}
