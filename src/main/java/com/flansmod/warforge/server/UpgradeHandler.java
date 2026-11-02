package com.flansmod.warforge.server;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.item.ItemMatcher;
import com.flansmod.warforge.compat.CommonTags;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpgradeHandler {
    public static final String STUB = """
            # WarForge citadel progression. Each faction starts at level 0 and spends the listed
            # materials at its citadel to reach the next level, unlocking more claims, force-loaded
            # chunks, insurance slots and FOBs. Requirements use vanilla items and common tags so the
            # tree works on a plain install; edit freely to fit your pack.

            [[levels]]
            level = 0
            claim_limit = 5
            insurance_slots = 0
            loaded_chunks = 4
            fob_ticket_limit = 5
            fob_ticket_regen = 1
            max_fobs = 1
            extra_claim_cost = []
            requirements = []

            [[levels]]
            level = 1
            claim_limit = 8
            insurance_slots = 9
            loaded_chunks = 6
            fob_ticket_limit = 5
            fob_ticket_regen = 1
            max_fobs = 2
            extra_claim_cost = []
            requirements = [
                { type = "ore", id = "%1$s:ingots/iron", count = 32 },
                { type = "ore", id = "%1$s:ingots/copper", count = 16 },
            ]

            [[levels]]
            level = 2
            claim_limit = 12
            insurance_slots = 18
            loaded_chunks = 8
            fob_ticket_limit = 5
            fob_ticket_regen = 1
            max_fobs = 2
            extra_claim_cost = []
            requirements = [
                { type = "ore", id = "%1$s:ingots/gold", count = 32 },
                { type = "ore", id = "%1$s:gems/diamond", count = 8 },
            ]

            [[levels]]
            level = 3
            claim_limit = 16
            insurance_slots = 27
            loaded_chunks = 12
            fob_ticket_limit = 5
            fob_ticket_regen = 1
            max_fobs = 3
            extra_claim_cost = []
            requirements = [
                { type = "ore", id = "%1$s:gems/diamond", count = 16 },
                { type = "item", id = "minecraft:netherite_ingot", count = 1 },
            ]
            """.formatted(CommonTags.NAMESPACE);

    public static final LevelSpec UNDEFINED_LEVEL = new LevelSpec(Map.of(), -1, 0, 0, 0, 0, 0, Map.of());

    /** One citadel level; claim limit -1 = unlimited. */
    public record LevelSpec(Map<ItemMatcher, Integer> requirements, int claimLimit, int insuranceSlots, int loadedChunks,
                            int fobTicketLimit, int fobTicketRegen, int maxFobs, Map<ItemMatcher, Integer> extraClaimCost) {
    }

    /** Immutable; replaced as a whole on (re)load. */
    private volatile List<LevelSpec> levels = List.of();

    public List<LevelSpec> getLevels() {
        return levels;
    }

    private LevelSpec spec(int level) {
        List<LevelSpec> current = levels;
        return level < 0 || level >= current.size() ? null : current.get(level);
    }

    public static void writeStubIfEmpty(Path filePath) throws IOException {
        if (Files.notExists(filePath) || Files.size(filePath) == 0) {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, STUB.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
    }

    /** Parses the file and installs the result on {@link WarForgeMod#UPGRADE_HANDLER}; a parse failure leaves the old levels. */
    public static void parseConfig(Path path) throws IOException {
        Config root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            root = new TomlParser().parse(reader);
        }
        Object rawLevels = root.get("levels");
        if (!(rawLevels instanceof List<?> rawLevelList)) {
            throw new IllegalStateException("Upgrade config must contain a 'levels' array of tables");
        }

        ArrayList<LevelSpec> parsed = new ArrayList<>();
        for (Object rawLevel : rawLevelList) {
            if (!(rawLevel instanceof Config levelMap)) {
                throw new IllegalArgumentException("Each level entry must be a table");
            }
            int level = readRequiredInt(levelMap, "level");
            int claimLimit = readRequiredInt(levelMap, "claim_limit");
            int insurance = readOptionalInt(levelMap, "insurance_slots", 0);
            int loaded = readOptionalInt(levelMap, "loaded_chunks", 0);
            if (claimLimit != -1 && claimLimit <= 0) {
                throw new IllegalArgumentException("Claim limit must be > 0 or -1");
            }
            if (insurance < 0) {
                throw new IllegalArgumentException("Insurance slots must be >= 0");
            }
            if (loaded < 0) {
                throw new IllegalArgumentException("Loaded chunks must be >= 0");
            }
            while (parsed.size() <= level) {
                parsed.add(UNDEFINED_LEVEL);
            }
            parsed.set(level, new LevelSpec(
                    parseMatchers(levelMap.get("requirements"), "requirements", level, false),
                    claimLimit, insurance, loaded,
                    readOptionalInt(levelMap, "fob_ticket_limit", 0),
                    readOptionalInt(levelMap, "fob_ticket_regen", 0),
                    readOptionalInt(levelMap, "max_fobs", 0),
                    parseMatchers(levelMap.get("extra_claim_cost"), "extra_claim_cost", level, true)));
        }
        validateMonotonicClaims(parsed);
        WarForgeMod.UPGRADE_HANDLER.levels = List.copyOf(parsed);
    }

    /** @param skipNonPositive extra_claim_cost entries with count <= 0 are ignored; requirements keep them */
    private static Map<ItemMatcher, Integer> parseMatchers(Object rawEntries, String section, int level, boolean skipNonPositive) {
        HashMap<ItemMatcher, Integer> result = new HashMap<>();
        if (!(rawEntries instanceof List<?> entries)) {
            return Map.of();
        }
        for (Object rawEntry : entries) {
            if (!(rawEntry instanceof Config entry)) {
                throw new IllegalArgumentException(section + " entries must be tables");
            }
            int count = readOptionalInt(entry, "count", 1);
            if (skipNonPositive && count <= 0) {
                continue;
            }
            String type = coerceString(entry.get("type")).toLowerCase(Locale.ROOT);
            String id = coerceString(entry.get("id"));
            ItemMatcher matcher;
            if ("ore".equals(type)) {
                matcher = ItemMatcher.ofTag(id);
            } else if ("item".equals(type)) {
                matcher = ItemMatcher.ofItem(id);
            } else {
                throw new IllegalArgumentException("Unknown " + section + " type: " + type);
            }
            if (matcher == null) {
                WarForgeMod.LOGGER.warn("UpgradeHandler config: {} '{}' (type {}) at level {} does not resolve to a known item/tag; skipping.", section, id, type, level);
                continue;
            }
            result.put(matcher, count);
        }
        return Map.copyOf(result);
    }

    /** @return null at and beyond the last level */
    public Map<ItemMatcher, Integer> getRequirementsFor(int level) {
        LevelSpec spec = spec(level);
        return spec == null ? null : spec.requirements();
    }

    public int getClaimLimitForLevel(int level) {
        LevelSpec spec = spec(level);
        return spec == null ? -1 : spec.claimLimit();
    }

    public int getInsuranceSlotsForLevel(int level) {
        LevelSpec spec = spec(level);
        return spec == null ? 0 : spec.insuranceSlots();
    }

    public int getLoadedChunksForLevel(int level) {
        LevelSpec spec = spec(level);
        return spec == null ? 0 : spec.loadedChunks();
    }

    public int getFobTicketLimitForLevel(int level) {
        LevelSpec spec = spec(level);
        return spec == null ? 0 : spec.fobTicketLimit();
    }

    public int getFobRegenForLevel(int level) {
        LevelSpec spec = spec(level);
        return spec == null ? 0 : spec.fobTicketRegen();
    }

    public int getMaxFobsForLevel(int level) {
        LevelSpec spec = spec(level);
        return spec == null ? 0 : spec.maxFobs();
    }

    /** @return null for undefined levels */
    public Map<ItemMatcher, Integer> getExtraClaimCostForLevel(int level) {
        LevelSpec spec = spec(level);
        return spec == null ? null : spec.extraClaimCost();
    }

    private static void validateMonotonicClaims(List<LevelSpec> specs) {
        for (int i = 1; i < specs.size(); i++) {
            int claims = specs.get(i).claimLimit();
            int previous = specs.get(i - 1).claimLimit();
            if (claims != -1 && previous != -1 && claims < previous) {
                throw new IllegalStateException("Claim limit at level " + i + " is less than previous level");
            }
        }
    }

    private static int readRequiredInt(Config map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required key: " + key);
        }
        return coerceInt(value);
    }

    private static int readOptionalInt(Config map, String key, int fallback) {
        Object value = map.get(key);
        return value == null ? fallback : coerceInt(value);
    }

    // night-config returns TOML integers as Number and strings as String or char[] by version
    private static int coerceInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(coerceString(value).trim());
    }

    private static String coerceString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof char[] chars) {
            return new String(chars);
        }
        return value.toString();
    }
}
