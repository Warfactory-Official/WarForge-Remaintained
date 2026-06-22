package com.flansmod.warforge.server;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.flansmod.warforge.common.WarForgeMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpgradeHandler {
    public static final String STUB = """
            [[levels]]
            level = 0
            claim_limit = 5
            insurance_slots = 0
            requirements = []

            [[levels]]
            level = 1
            claim_limit = 10
            insurance_slots = 9
            requirements = [
                { type = "ore", id = "forge:ingots/iron", count = 64 },
                { type = "item", id = "minecraft:diamond", count = 1 },
            ]

            [[levels]]
            level = 2
            claim_limit = 15
            insurance_slots = 18
            requirements = [
                { type = "item", id = "minecraft:netherite_ingot", count = 1 },
            ]
            """;

    protected HashMap<ItemMatcher, Integer>[] LEVELS;
    protected int[] LIMITS;
    protected int[] INSURANCE_SLOTS;

    public UpgradeHandler() {
        LEVELS = new HashMap[0];
        LIMITS = new int[0];
        INSURANCE_SLOTS = new int[0];
    }

    public int[] getLIMITS() {
        return LIMITS;
    }

    public HashMap<ItemMatcher, Integer>[] getLEVELS() {
        return LEVELS;
    }

    public int[] getINSURANCE_SLOTS() {
        return INSURANCE_SLOTS;
    }

    public void setLevelAndLimits(int level, HashMap<ItemMatcher, Integer> requirements, int limit, int insuranceSlots) {
        if (level >= LEVELS.length) {
            int newSize = Math.max(level + 1, Math.max(LEVELS.length * 2, 1));
            LEVELS = Arrays.copyOf(LEVELS, newSize);
            LIMITS = Arrays.copyOf(LIMITS, newSize);
            INSURANCE_SLOTS = Arrays.copyOf(INSURANCE_SLOTS, newSize);
        }
        LEVELS[level] = requirements;
        LIMITS[level] = limit;
        INSURANCE_SLOTS[level] = insuranceSlots;
    }

    public static void migrateLegacyConfigIfNeeded(Path legacyCfg, Path tomlPath) throws IOException {
        if (Files.exists(tomlPath) || !Files.exists(legacyCfg)) {
            return;
        }

        LegacyConfigData migrated = parseLegacyConfig(legacyCfg);
        writeTomlConfig(tomlPath, migrated.levels, migrated.claims, migrated.insuranceSlots);
        Files.move(legacyCfg, legacyCfg.resolveSibling(legacyCfg.getFileName() + ".migrated"), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void writeStubIfEmpty(Path filePath) throws IOException {
        if (Files.notExists(filePath) || Files.size(filePath) == 0) {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, STUB.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
    }

    public static void parseConfig(Path path) throws IOException {
        Config root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            root = new TomlParser().parse(reader);
        }

        Object rawLevels = root.get("levels");
        if (!(rawLevels instanceof List<?> rawLevelList)) {
            throw new IllegalStateException("Upgrade config must contain a 'levels' array of tables");
        }

        List<Map<ItemMatcher, Integer>> levels = new ArrayList<>();
        List<Integer> claims = new ArrayList<>();
        List<Integer> insuranceSlots = new ArrayList<>();

        for (Object rawLevel : rawLevelList) {
            if (!(rawLevel instanceof Config levelMap)) {
                throw new IllegalArgumentException("Each level entry must be a table");
            }

            int level = readRequiredInt(levelMap, "level");
            int claimLimit = readRequiredInt(levelMap, "claim_limit");
            int insurance = readOptionalInt(levelMap, "insurance_slots", 0);
            if (claimLimit != -1 && claimLimit <= 0) {
                throw new IllegalArgumentException("Claim limit must be > 0 or -1");
            }
            if (insurance < 0) {
                throw new IllegalArgumentException("Insurance slots must be >= 0");
            }

            while (levels.size() <= level) {
                levels.add(new HashMap<>());
                claims.add(-1);
                insuranceSlots.add(0);
            }

            HashMap<ItemMatcher, Integer> requirements = new HashMap<>();
            Object rawRequirements = levelMap.get("requirements");
            if (rawRequirements instanceof List<?> requirementList) {
                for (Object rawRequirement : requirementList) {
                    if (!(rawRequirement instanceof Config requirementMap)) {
                        throw new IllegalArgumentException("Requirement entries must be tables");
                    }
                    String type = String.valueOf(requirementMap.get("type")).toLowerCase(Locale.ROOT);
                    String id = String.valueOf(requirementMap.get("id"));
                    int count = readOptionalInt(requirementMap, "count", 1);

                    ItemMatcher matcher;
                    if ("ore".equals(type)) {
                        matcher = ItemMatcher.ofTag(id);
                    } else if ("item".equals(type)) {
                        String[] parts = id.split(":");
                        if (parts.length != 2 && parts.length != 3) {
                            throw new IllegalArgumentException("Invalid item id format: " + id);
                        }
                        matcher = ItemMatcher.ofItem(id);
                    } else {
                        throw new IllegalArgumentException("Unknown requirement type: " + type);
                    }
                    if (matcher == null) {
                        WarForgeMod.LOGGER.warn("UpgradeHandler config: requirement '{}' (type {}) at level {} does not resolve to a known item/tag; skipping.", id, type, level);
                        continue;
                    }
                    requirements.put(matcher, count);
                }
            }

            levels.set(level, requirements);
            claims.set(level, claimLimit);
            insuranceSlots.set(level, insurance);
        }

        validateMonotonicClaims(claims);
        applyParsedData(levels, claims, insuranceSlots);
    }

    public HashMap<ItemMatcher, Integer> getRequirementsFor(int level) {
        if (level >= LEVELS.length) {
            return null;
        }
        return LEVELS[level];
    }

    public int getClaimLimitForLevel(int level) {
        if (level >= LIMITS.length || level < 0) {
            return -1;
        }
        return LIMITS[level];
    }

    public int getInsuranceSlotsForLevel(int level) {
        if (level >= INSURANCE_SLOTS.length || level < 0) {
            return 0;
        }
        return INSURANCE_SLOTS[level];
    }

    private static void applyParsedData(List<Map<ItemMatcher, Integer>> levels, List<Integer> claims, List<Integer> insuranceSlots) {
        int size = levels.size();
        WarForgeMod.UPGRADE_HANDLER.LEVELS = new HashMap[size];
        WarForgeMod.UPGRADE_HANDLER.LIMITS = new int[size];
        WarForgeMod.UPGRADE_HANDLER.INSURANCE_SLOTS = new int[size];
        for (int i = 0; i < size; i++) {
            WarForgeMod.UPGRADE_HANDLER.LEVELS[i] = new HashMap<>(levels.get(i));
            WarForgeMod.UPGRADE_HANDLER.LIMITS[i] = claims.get(i);
            WarForgeMod.UPGRADE_HANDLER.INSURANCE_SLOTS[i] = insuranceSlots.get(i);
        }
    }

    private static void validateMonotonicClaims(List<Integer> claims) {
        for (int i = 1; i < claims.size(); i++) {
            if (claims.get(i) != -1 && claims.get(i - 1) != -1 && claims.get(i) < claims.get(i - 1)) {
                throw new IllegalStateException("Claim limit at level " + i + " is less than previous level");
            }
        }
    }

    private static int readRequiredInt(Config map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required key: " + key);
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static int readOptionalInt(Config map, String key, int fallback) {
        Object value = map.get(key);
        return value == null ? fallback : Integer.parseInt(String.valueOf(value));
    }

    private static void writeTomlConfig(Path path, List<Map<ItemMatcher, Integer>> levels, List<Integer> claims, List<Integer> insuranceSlots) throws IOException {
        List<Config> tomlLevels = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            Config levelMap = TomlFormat.newConfig();
            levelMap.set("level", i);
            levelMap.set("claim_limit", claims.get(i));
            levelMap.set("insurance_slots", insuranceSlots.get(i));

            List<Config> requirements = new ArrayList<>();
            for (Map.Entry<ItemMatcher, Integer> entry : levels.get(i).entrySet()) {
                Config requirementMap = TomlFormat.newConfig();
                requirementMap.set("type", entry.getKey().isTag() ? "ore" : "item");
                requirementMap.set("id", entry.getKey().id());
                requirementMap.set("count", entry.getValue());
                requirements.add(requirementMap);
            }
            levelMap.set("requirements", requirements);
            tomlLevels.add(levelMap);
        }

        Config root = TomlFormat.newConfig();
        root.set("levels", tomlLevels);
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            new TomlWriter().write(root, writer);
        }
    }

    private static LegacyConfigData parseLegacyConfig(Path path) throws IOException {
        List<Map<ItemMatcher, Integer>> levels = new ArrayList<>();
        List<Integer> claims = new ArrayList<>();
        List<Integer> insuranceSlots = new ArrayList<>();

        levels.add(new HashMap<>());
        claims.add(-1);
        insuranceSlots.add(0);

        Map<ItemMatcher, Integer> current = null;
        int currentLevel = 0;

        for (String line : Files.readAllLines(path)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("level:")) {
                String levelSpec = line.substring(6).trim();
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\[(\\d+|-1)](?:\\[(\\d+)])?").matcher(levelSpec);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid level format: " + line);
                }

                currentLevel = Integer.parseInt(matcher.group(1));
                int claimLimit = Integer.parseInt(matcher.group(2));
                int insurance = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));

                while (levels.size() <= currentLevel) {
                    levels.add(new HashMap<>());
                    claims.add(-1);
                    insuranceSlots.add(0);
                }

                claims.set(currentLevel, claimLimit);
                insuranceSlots.set(currentLevel, insurance);
                current = levels.get(currentLevel);
                continue;
            }

            if (current == null) {
                throw new IllegalStateException("Item defined before any level");
            }

            String type;
            if (line.startsWith("item:")) {
                type = "item";
            } else if (line.startsWith("ore:")) {
                type = "ore";
            } else {
                throw new IllegalArgumentException("Unknown entry type: " + line);
            }

            String content = line.substring(type.length() + 1).trim();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(.+?)(\\[(\\d+)])?$").matcher(content);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid format for line: " + line);
            }

            String rawEntry = matcher.group(1);
            int count = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 1;

            ItemMatcher itemMatcher;
            if ("ore".equals(type)) {
                itemMatcher = ItemMatcher.ofTag(rawEntry);
            } else {
                String[] parts = rawEntry.split(":");
                if (parts.length != 2 && parts.length != 3) {
                    throw new IllegalArgumentException("Invalid item format: " + rawEntry);
                }
                itemMatcher = ItemMatcher.ofItem(rawEntry);
            }
            if (itemMatcher == null) {
                WarForgeMod.LOGGER.warn("UpgradeHandler legacy config: '{}' did not resolve to a known item/tag; skipping.", rawEntry);
                continue;
            }
            current.put(itemMatcher, count);
        }

        validateMonotonicClaims(claims);
        return new LegacyConfigData(levels, claims, insuranceSlots);
    }

    private static final class LegacyConfigData {
        private final List<Map<ItemMatcher, Integer>> levels;
        private final List<Integer> claims;
        private final List<Integer> insuranceSlots;

        private LegacyConfigData(List<Map<ItemMatcher, Integer>> levels, List<Integer> claims, List<Integer> insuranceSlots) {
            this.levels = levels;
            this.claims = claims;
            this.insuranceSlots = insuranceSlots;
        }
    }
}
