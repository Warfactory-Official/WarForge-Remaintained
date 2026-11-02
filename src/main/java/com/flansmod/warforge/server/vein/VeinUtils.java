package com.flansmod.warforge.server.vein;

import org.apache.commons.lang3.tuple.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.VeinCodec;
import com.flansmod.warforge.api.vein.VeinEntry;
import com.flansmod.warforge.api.vein.VeinView;
import com.flansmod.warforge.api.WarforgeAPI;
import com.flansmod.warforge.api.vein.VeinKey;
import com.flansmod.warforge.api.vein.VeinWeightProvider;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortRBTreeMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

import static com.flansmod.warforge.common.WarForgeMod.LOGGER;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import com.flansmod.warforge.compat.Nbt;

public class VeinUtils {
    private static final short WEIGHT_FRACTION_TENS_POW = VeinCodec.WEIGHT_FRACTION_TENS_POW;
    private static final short NULL_VEIN_ID = VeinCodec.NULL_VEIN_ID;

    private static final int STORAGE_FORMAT = 2;

    public Short2ObjectOpenHashMap<Vein> ID_TO_VEINS = new Short2ObjectOpenHashMap<>();

    // we pack the chunk indices as x(higher bits) z(lower bits) and vein data as iteration id (higher bits) vein id (lower bits)
    // for the mega chunk occurrence data, the default return value set's first entry is the current weight
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Object2ShortAVLTreeMap<VeinKey>> DIM_VEIN_WEIGHT_MAP;  // dim -> [key->id map, .obj -> veinkey -> id
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2ObjectOpenHashMap<Pair<Short2ShortOpenHashMap, Short2ShortRBTreeMap>>> MEGA_CHUNK_OCCURRENCE_DATA;  // dim -> megchunk -> (id -> occurrences and short [byte-X, byte-Z] offsets -> id)

    public final short iterationId;
    public final short megachunkLength;
    public final short megachunkArea;

    public final double gradientCenterX;
    public final double gradientCenterZ;
    public final double gradientRadius;
    public final double gradientFrequency;
    public final boolean gradientEnabled;

    public boolean hasFinishedInit;

    VeinUtils(short iterationId, short megachunkLength) {
        this(iterationId, megachunkLength, 0.0, 0.0, 0.0, 1.0);
    }

    VeinUtils(short iterationId, short megachunkLength, double gradientCenterX, double gradientCenterZ, double gradientRadius, double gradientFrequency) {
        this.iterationId = iterationId;
        this.megachunkLength = megachunkLength;
        this.megachunkArea = (short) (megachunkLength * megachunkLength);
        ID_TO_VEINS.defaultReturnValue(null);

        this.gradientCenterX = gradientCenterX;
        this.gradientCenterZ = gradientCenterZ;
        this.gradientRadius = gradientRadius;
        this.gradientFrequency = gradientFrequency;
        this.gradientEnabled = gradientRadius > 0.0;

        DIM_VEIN_WEIGHT_MAP = new Object2ObjectOpenHashMap<>();
        MEGA_CHUNK_OCCURRENCE_DATA = new Object2ObjectOpenHashMap<>();
        hasFinishedInit = false;
    }

    public boolean dimHasVeins(ResourceKey<Level> dim) {
        return DIM_VEIN_WEIGHT_MAP.containsKey(dim);
    }

    public short getVeinId(ResourceKey<Level> dim, VeinKey key) {
        return DIM_VEIN_WEIGHT_MAP.get(dim).getShort(key);
    }

    public short getVeinIdSafe(ResourceKey<Level> dim, VeinKey key) {
        if (!dimHasVeins(dim)) { return NULL_VEIN_ID; }
        return getVeinId(dim, key);
    }

    public short getVeinOccurrences(ResourceKey<Level> dim, long megachunkKey, short veinId) {
        return MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey).getLeft().get(veinId);
    }

    public short getVeinOccurrencesSafe(ResourceKey<Level> dim, long megachunkKey, short veinId) {
        ensureMegachunkPopulated(dim, megachunkKey);
        return getVeinOccurrences(dim, megachunkKey, veinId);
    }

    public int[] generateChunkHash(int chunk_x, int chunk_z, long seed) {
        int hash = (int) (seed * 2654435761L);
        hash = (int) ((hash + chunk_x) * 2654435761L);
        hash = (int) ((hash + chunk_z) * 2654435761L);
        hash = (hash << 1) >>> 1;

        int quality = hash % Quality.values().length;

        return new int[]{hash, quality};
    }

    public double gradientDensity(int chunkX, int chunkZ) {
        if (!gradientEnabled) { return 1.0; }
        double blockX = chunkX * 16.0 + 8.0;
        double blockZ = chunkZ * 16.0 + 8.0;
        double dist = Math.max(Math.abs(blockX - gradientCenterX), Math.abs(blockZ - gradientCenterZ));
        double t = dist / gradientRadius;
        if (t >= 1.0) { return 0.0; }
        return 1.0 - Math.pow(t, gradientFrequency);
    }

    public int gradientGateHash(int chunkX, int chunkZ, long seed) {
        int hash = (int) ((seed ^ 0x9E3779B97F4A7C15L) * 0x27D4EB2F165667C5L);
        hash = (int) ((hash + chunkX) * 0x85EBCA6B);
        hash = (int) ((hash + chunkZ) * 0xC2B2AE35);
        hash ^= (hash >>> 15);
        return (hash << 1) >>> 1;
    }

    public boolean gradientGatesOut(int chunkX, int chunkZ, long seed) {
        double density = gradientDensity(chunkX, chunkZ);
        if (density >= 1.0) { return false; }
        if (density <= 0.0) { return true; }
        double roll = gradientGateHash(chunkX, chunkZ, seed) / 2147483648.0;
        return roll >= density;
    }

    public long produceMegachunkKey(int chunkX, int chunkZ) {
        return VeinCodec.megachunkKey(chunkX, chunkZ, megachunkLength);
    }

    public short produceChunkOffset(int chunkX, int chunkZ) {
        return VeinCodec.chunkOffset(chunkX, chunkZ, megachunkLength);
    }

    public Vein getVein(short veinId) {
        return ID_TO_VEINS.get(veinId);
    }

    public Pair<Vein, Quality> getVein(ResourceKey<Level> dim, int chunkX, int chunkZ, long seed) {
        long megachunkKey = produceMegachunkKey(chunkX, chunkZ);
        short offset = produceChunkOffset(chunkX, chunkZ);

        if (!dimHasVeins(dim)) { return null; }

        Pair<Vein, Quality> veinInfo = pullVein(dim, megachunkKey, offset);
        if (veinInfo != null) { return veinInfo.getLeft() == null ? null : veinInfo; }

        veinInfo = generateVeinInfo(dim, megachunkKey, chunkX, chunkZ, seed);
        short veinId = veinInfo == null ? NULL_VEIN_ID : veinInfo.getLeft().getId();
        int quality = veinInfo == null ? 7 : veinInfo.getRight().ordinal();

        addVeinInfo(dim, megachunkKey, offset, veinId, quality);
        return veinInfo;
    }

    public Pair<Vein, Quality> peekStoredVein(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        return pullVein(dim, produceMegachunkKey(chunkX, chunkZ), produceChunkOffset(chunkX, chunkZ));
    }

    public Vein findVein(String identifier) {
        if (identifier == null) { return null; }
        String trimmed = identifier.trim();
        try {
            Vein byId = ID_TO_VEINS.get(Short.parseShort(trimmed));
            if (byId != null) { return byId; }
        } catch (NumberFormatException ignored) { }
        for (Vein vein : ID_TO_VEINS.values()) {
            if (vein.translationKey.equalsIgnoreCase(trimmed)) { return vein; }
        }
        return null;
    }

    private void ensureMegachunkStorage(ResourceKey<Level> dim, long megachunkKey) {
        if (isMegachunkPopulated(dim, megachunkKey)) { return; }
        if (DIM_VEIN_WEIGHT_MAP.containsKey(dim)) {
            populateMegachunkInfo(dim, megachunkKey);
            return;
        }
        var dimMap = MEGA_CHUNK_OCCURRENCE_DATA.computeIfAbsent(dim, d -> new Long2ObjectOpenHashMap<>());
        var pair = Pair.of(new Short2ShortOpenHashMap(), new Short2ShortRBTreeMap());
        pair.getLeft().defaultReturnValue(WEIGHT_FRACTION_TENS_POW);
        pair.getRight().defaultReturnValue(NULL_VEIN_ID);
        dimMap.put(megachunkKey, pair);
    }

    public void setVeinOverride(ResourceKey<Level> dim, int chunkX, int chunkZ, short veinId, int quality) {
        long megachunkKey = produceMegachunkKey(chunkX, chunkZ);
        short offset = produceChunkOffset(chunkX, chunkZ);
        ensureMegachunkStorage(dim, megachunkKey);

        var currMegachunk = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);
        var occurrences = currMegachunk.getLeft();
        var offsets = currMegachunk.getRight();

        if (offsets.containsKey(offset)) {
            short oldId = VeinCodec.splitVeinInfo(offsets.get(offset))[0];
            if (oldId != NULL_VEIN_ID && occurrences.containsKey(oldId)) {
                occurrences.put(oldId, (short) Math.max(0, occurrences.get(oldId) - 1));
            }
        }

        short info = (veinId == NULL_VEIN_ID) ? NULL_VEIN_ID : VeinCodec.compressVeinInfo(veinId, quality);
        offsets.put(offset, info);
        if (veinId != NULL_VEIN_ID && occurrences.containsKey(veinId)) {
            occurrences.put(veinId, (short) (occurrences.get(veinId) + 1));
        }

        recomputeRemainingWeight(dim, megachunkKey);
    }

    public void clearVeinAt(ResourceKey<Level> dim, int chunkX, int chunkZ) {
        setVeinOverride(dim, chunkX, chunkZ, NULL_VEIN_ID, 7);
    }

    public Pair<Vein, Quality> rerollVeinAt(ResourceKey<Level> dim, int chunkX, int chunkZ, long seed) {
        long megachunkKey = produceMegachunkKey(chunkX, chunkZ);
        short offset = produceChunkOffset(chunkX, chunkZ);
        if (isMegachunkPopulated(dim, megachunkKey)) {
            var currMegachunk = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);
            if (currMegachunk.getRight().containsKey(offset)) {
                short oldId = VeinCodec.splitVeinInfo(currMegachunk.getRight().get(offset))[0];
                currMegachunk.getRight().remove(offset);
                if (oldId != NULL_VEIN_ID && currMegachunk.getLeft().containsKey(oldId)) {
                    currMegachunk.getLeft().put(oldId, (short) Math.max(0, currMegachunk.getLeft().get(oldId) - 1));
                }
                recomputeRemainingWeight(dim, megachunkKey);
            }
        }
        return getVein(dim, chunkX, chunkZ, seed);
    }

    private void recomputeRemainingWeight(ResourceKey<Level> dim, long megachunkKey) {
        if (!isMegachunkPopulated(dim, megachunkKey)) { return; }
        var occurrences = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey).getLeft();
        int remaining = WEIGHT_FRACTION_TENS_POW;
        for (var entry : occurrences.short2ShortEntrySet()) {
            Vein vein = ID_TO_VEINS.get(entry.getShortKey());
            if (vein == null) { continue; }
            if (entry.getShortValue() >= vein.getDimExpectedCount(dim)) {
                remaining -= vein.getDimWeight(dim);
            }
        }
        occurrences.defaultReturnValue((short) Math.max(0, remaining));
    }

    public boolean isMegachunkPopulated(ResourceKey<Level> dim, long megachunkKey) {
        return MEGA_CHUNK_OCCURRENCE_DATA.containsKey(dim) && MEGA_CHUNK_OCCURRENCE_DATA.get(dim).containsKey(megachunkKey);
    }

    public void ensureMegachunkPopulated(ResourceKey<Level> dim, long megachunkKey) {
        if (!isMegachunkPopulated(dim, megachunkKey) && DIM_VEIN_WEIGHT_MAP.containsKey(dim)) {
            populateMegachunkInfo(dim, megachunkKey);
        }
    }

    // should only be called for dimensions which actually expect to see veins within them
    public void populateMegachunkInfo(ResourceKey<Level> dim, long megachunkKey) {
        var currDimMegachunks = MEGA_CHUNK_OCCURRENCE_DATA.computeIfAbsent(dim, d -> new Long2ObjectOpenHashMap<>());
        currDimMegachunks.put(megachunkKey, Pair.of(
            new Short2ShortOpenHashMap(),
            new Short2ShortRBTreeMap()
        ));

        var currMegachunk = currDimMegachunks.get(megachunkKey);
        for (var entry : DIM_VEIN_WEIGHT_MAP.get(dim).object2ShortEntrySet()) {
            if (entry.getKey() == VeinKey.NULL_KEY) { continue; }
            currMegachunk.getLeft().put(entry.getShortValue(), (short) 0);
        }

        currMegachunk.getLeft().defaultReturnValue(WEIGHT_FRACTION_TENS_POW);
        currMegachunk.getRight().defaultReturnValue(NULL_VEIN_ID);
    }

    public Pair<Vein, Quality> pullVein(ResourceKey<Level> dim, long megachunkKey, short offset) {
        if (!isMegachunkPopulated(dim, megachunkKey)) { return null; }

        var offsetIds = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey).getRight();
        if (!offsetIds.containsKey(offset)) { return null; }

        short chunkData = offsetIds.get(offset);
        if (chunkData == NULL_VEIN_ID) { return Pair.of(null, null); }

        short[] unpackedData = VeinCodec.splitVeinInfo(chunkData);
        return Pair.of(ID_TO_VEINS.get(unpackedData[0]), Quality.getQuality(unpackedData[1]));
    }

    public Pair<Vein, Quality> generateVeinInfo(ResourceKey<Level> dim, long megachunkKey, int chunkX, int chunkZ, long seed) {
        if (!DIM_VEIN_WEIGHT_MAP.containsKey(dim)) { return null; }
        ensureMegachunkPopulated(dim, megachunkKey);

        int[] chunkHash = generateChunkHash(chunkX, chunkZ, seed);
        VeinWeightProvider provider = WarforgeAPI.veinWeightProvider();
        VeinWeightProvider.Assignment assigned = provider == null ? null : provider.assigned(dim, chunkX, chunkZ);
        if (assigned != null) {
            return assigned.vein() == null ? null
                    : Pair.of(ID_TO_VEINS.get(assigned.vein().id()), VeinWeightProvider.shift(chunkHash[1], assigned.richness()));
        }

        if (gradientEnabled && gradientGatesOut(chunkX, chunkZ, seed)) { return null; }

        ToDoubleFunction<VeinView> multipliers = provider == null ? null : provider.multipliers(dim, chunkX, chunkZ);
        if (multipliers != null) {
            Pair<Vein, Quality> rolled = weightedVeinRoll(dim, chunkHash, multipliers);
            return rolled == null ? null : Pair.of(rolled.getLeft(), VeinWeightProvider.shift(chunkHash[1],
                    provider.richness(dim, chunkX, chunkZ, rolled.getLeft().view())));
        }
        Object2ShortAVLTreeMap<VeinKey> currDimWeights = DIM_VEIN_WEIGHT_MAP.get(dim);
        var currMegachunk = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);

        // we limit the hash result based on the remaining weight in the megachunk, then skip over veins which exceed
        // their expected value [floor(megachunkLength^2 * weight / 1000)]
        short weightRemaining = currMegachunk.getLeft().defaultReturnValue();
        boolean doRandomRoll = weightRemaining == 0;

        short trimmedHash = (short) (chunkHash[0] % weightRemaining);
        VeinKey currVeinKey = new VeinKey(trimmedHash);
        short currID = currDimWeights.getShort(currVeinKey);

        Vein currVein = ID_TO_VEINS.get(currID);
        short dimExpCount = getDimExpCount(currVein, dim);

        if (!doRandomRoll && weightRemaining < WEIGHT_FRACTION_TENS_POW) {
            int skipGuard = ID_TO_VEINS.size() + 1;
            while (currMegachunk.getLeft().get(currID) >= dimExpCount) {
                short step = getDimWeight(currVein, dim);
                if (step <= 0 || --skipGuard < 0) { break; }
                trimmedHash += step;
                currVeinKey.rebaseKey(trimmedHash);
                currID = currDimWeights.getShort(currVeinKey);
                currVein = ID_TO_VEINS.get(currID);
            }
        }

        if (currVein == null) { return null; }
        return Pair.of(currVein, Quality.getQuality(chunkHash[1]));
    }

    // ids from the weight tree: veins dropped for overflowing 10000 stay excluded
    private Pair<Vein, Quality> weightedVeinRoll(ResourceKey<Level> dim, int[] chunkHash, ToDoubleFunction<VeinView> multipliers) {
        Object2ShortAVLTreeMap<VeinKey> dimWeights = DIM_VEIN_WEIGHT_MAP.get(dim);
        ArrayList<Vein> veins = new ArrayList<>();
        DoubleArrayList weights = new DoubleArrayList();
        double total = dimWeights.defaultReturnValue();
        for (var entry : dimWeights.object2ShortEntrySet()) {
            if (entry.getKey() == VeinKey.NULL_KEY || entry.getShortValue() == NULL_VEIN_ID) { continue; }
            Vein vein = ID_TO_VEINS.get(entry.getShortValue());
            double weight = vein.getDimWeight(dim) * Math.max(0.0, multipliers.applyAsDouble(vein.view()));
            if (weight <= 0.0) { continue; }
            veins.add(vein);
            weights.add(weight);
            total += weight;
        }
        double roll = chunkHash[0] / 2147483648.0 * total;
        for (int i = 0; i < veins.size(); ++i) {
            roll -= weights.getDouble(i);
            if (roll < 0.0) { return Pair.of(veins.get(i), Quality.getQuality(chunkHash[1])); }
        }
        return null;
    }

    public short getDimWeight(Vein vein, ResourceKey<Level> dim) {
        return vein == null ? DIM_VEIN_WEIGHT_MAP.get(dim).defaultReturnValue() : vein.getDimExpectedCount(dim);
    }

    public short getDimExpCount(Vein vein, ResourceKey<Level> dim) {
        return vein == null ? DIM_VEIN_WEIGHT_MAP.get(dim).getShort(VeinKey.NULL_KEY) : vein.getDimExpectedCount(dim);
    }

    public void addVeinInfo(ResourceKey<Level> dim, int chunkX, int chunkZ, short veinId, int quality) {
        long megachunkKey = produceMegachunkKey(chunkX, chunkZ);
        short offset = produceChunkOffset(chunkX, chunkZ);

        addVeinInfo(dim, megachunkKey, offset, veinId, quality);
    }

    public void addVeinInfo(ResourceKey<Level> dim, long megachunkKey, short offset, short veinId, int quality) {
        ensureMegachunkPopulated(dim, megachunkKey);
        var currMegachunk = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);

        int currOccurrences = currMegachunk.getLeft().get(veinId);
        currMegachunk.getLeft().put(veinId, (short) (currOccurrences + 1));
        Vein currVein = ID_TO_VEINS.get(veinId);

        short veinData;
        if (veinId == NULL_VEIN_ID) { veinData = NULL_VEIN_ID; }
        else { veinData = VeinCodec.compressVeinInfo(veinId, quality); }
        currMegachunk.getRight().put(offset, veinData);

        if (currMegachunk.getLeft().defaultReturnValue() == 0) { return; }

        short expectedCount = getDimExpCount(currVein, dim);

        if (currMegachunk.getLeft().defaultReturnValue() > 0 && currOccurrences + 1 >= expectedCount) {
            int dimWeight = getDimWeight(currVein, dim);
            currMegachunk.getLeft().defaultReturnValue((short) (currMegachunk.getLeft().defaultReturnValue() - dimWeight));
        }
    }

    private void populateDimVeinMap(ResourceKey<Level> dim) {
        ArrayList<Vein> smallestWeights = new ArrayList<>();
        ArrayList<Vein> mediumWeights = new ArrayList<>();
        ArrayList<Vein> largerWeights = new ArrayList<>();
        ArrayList<Vein> largestWeights = new ArrayList<>();

        ArrayList<ArrayList<Vein>> sortedVeins = new ArrayList<>();
        sortedVeins.add(smallestWeights);
        sortedVeins.add(mediumWeights);
        sortedVeins.add(largerWeights);
        sortedVeins.add(largestWeights);
        short remainingWeight = WEIGHT_FRACTION_TENS_POW;

        var dimWeightMap = DIM_VEIN_WEIGHT_MAP.get(dim);

        for (Vein currVein : ID_TO_VEINS.values()) {
            short currDimWeight = currVein.getDimWeight(dim);
            if (currDimWeight == 0) { continue; }
            if (remainingWeight < currDimWeight) {
                LOGGER.error("The maximum weight (10000) has been exceeded at vein {} in dim {}; ignoring current vein.",
                        currVein.translationKey, Keys.id(dim));
                continue;
            } else {
                remainingWeight -= currDimWeight;
            }

            if (currDimWeight <= 1250) { smallestWeights.add(currVein); }
            else if (currDimWeight <= 2500) { mediumWeights.add(currVein); }
            else if (currDimWeight <= 5000) { largerWeights.add(currVein); }
            else { largestWeights.add(currVein); }
        }

        final short nullWeight = remainingWeight;

        if (nullWeight > 0) {
            if (nullWeight <= 1250) { smallestWeights.add(null); }
            else if (nullWeight <= 2500) { mediumWeights.add(null); }
            else if (nullWeight <= 5000) { largerWeights.add(null); }
            else { largestWeights.add(null); }
        }

        Comparator<Vein> weight_sorter = (vein1, vein2) -> {
            short weight1 = vein1 == null ? nullWeight : vein1.getDimWeight(dim);
            short weight2 = vein2 == null ? nullWeight : vein2.getDimWeight(dim);

            return Short.compare(weight1, weight2);
        };

        smallestWeights.sort(weight_sorter);
        mediumWeights.sort(weight_sorter);
        largerWeights.sort(weight_sorter);
        largestWeights.sort(weight_sorter);

        // we will assign key bounds such that the least likely veins end up with the most extreme bounds
        // while the most likely veins have the intermediate bounds
        // this should hopefully bias the most likely veins towards the top of the tree
        for (int i = 0; i < sortedVeins.size(); ++i) {
            ArrayList<Vein> currVeinCategory = sortedVeins.get(i);
            int halfPoint = (currVeinCategory.size() + 1) >> 1;
            for (int j = 0; j < halfPoint; ++j) {
                Vein currVein = currVeinCategory.get(j);
                short currWeight = nullWeight;
                short currID = NULL_VEIN_ID;
                if (currVein != null) {
                    currWeight = currVein.getDimWeight(dim);
                    currID = currVein.getId();
                }

                dimWeightMap.put(new VeinKey(currWeight, false), currID);
            }
        }

        for (int i = sortedVeins.size() - 1; i >= 0; --i) {
            ArrayList<Vein> currVeinCategory = sortedVeins.get(i);
            int halfPoint = (currVeinCategory.size() + 1) >> 1;
            for (int j = halfPoint; j < currVeinCategory.size(); ++j) {
                Vein currVein = currVeinCategory.get(j);
                short currWeight = nullWeight;
                short currID = NULL_VEIN_ID;
                if (currVein != null) {
                    currWeight = currVein.getDimWeight(dim);
                    currID = currVein.getId();
                }

                dimWeightMap.put(new VeinKey(currWeight, false), currID);
            }
        }

        // default return value is the null vein weight, while the null key returns the expected null vein occurrences
        dimWeightMap.defaultReturnValue(nullWeight);
        dimWeightMap.put(VeinKey.NULL_KEY, (short) (megachunkArea * nullWeight / VeinUtils.WEIGHT_FRACTION_TENS_POW));
    }

    void populateVeinMap(@Nullable List<VeinEntry> veinEntries) {
        if (veinEntries == null || veinEntries.size() == 0) { return; }

        for (VeinEntry entry : veinEntries) {
            Vein currVein = new Vein(entry, megachunkArea);
            ID_TO_VEINS.put(currVein.getId(), currVein);

            for (ResourceKey<Level> dim : currVein.getValidDims()) {
                DIM_VEIN_WEIGHT_MAP.put(dim, new Object2ShortAVLTreeMap<>());
            }
        }

        for (ResourceKey<Level> dim : DIM_VEIN_WEIGHT_MAP.keySet()) {
            populateDimVeinMap(dim);
        }

        hasFinishedInit = true;
    }

    private String getVeinInfoID(short veinId) {
        return "vinfo_" + veinId;
    }
    private String getDimInfoID(ResourceKey<Level> dim) { return "dinfo_" + Keys.id(dim).toString(); }

    public void readFromNBT(CompoundTag tags) {
        if (!tags.contains("vein_dims")) { return; }
        if (!tags.contains("vein_format") || Nbt.getInt(tags, "vein_format") != STORAGE_FORMAT) {
            LOGGER.info("Vein storage format changed; discarding stored vein data.");
            return;
        }
        if (!tags.contains("vein_it_id") || Nbt.getShort(tags, "vein_it_id") != iterationId) {
            LOGGER.info("Vein iteration changed; discarding stored vein data.");
            return;
        }
        // megachunk length controls how chunk offsets are packed; if it changed we cannot reuse the stored data
        if (tags.contains("vein_megachunk_length") && Nbt.getShort(tags, "vein_megachunk_length") != megachunkLength) {
            LOGGER.info("Vein megachunk_length changed; discarding stored vein data so chunk offsets can repack.");
            return;
        }

        CompoundTag dims = Nbt.getCompound(tags, "vein_dims");
        for (String dimKey : Nbt.keys(dims)) {
            ResourceLocation dimLoc = Ids.tryParse(dimKey.substring("dinfo_".length()));
            if (dimLoc == null) { continue; }
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimLoc);
            if (!DIM_VEIN_WEIGHT_MAP.containsKey(dim)) { continue; }

            CompoundTag currDim = Nbt.getCompound(dims, dimKey);
            ListTag megachunks = Nbt.getList(currDim, "megachunks", Tag.TAG_COMPOUND);
            for (int megachunkTagIndex = 0; megachunkTagIndex < megachunks.size(); ++megachunkTagIndex) {
                CompoundTag currMegachunkNBT = Nbt.getCompound(megachunks, megachunkTagIndex);
                long megachunkKey = Nbt.getLong(currMegachunkNBT, "key");
                ensureMegachunkPopulated(dim, megachunkKey);
                var currMegachunk = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);

                CompoundTag veins = Nbt.getCompound(currMegachunkNBT, "veins");
                for (String veinTag : Nbt.keys(veins)) {
                    short storedId;
                    try { storedId = Short.parseShort(veinTag.substring("vinfo_".length())); }
                    catch (NumberFormatException e) { continue; }

                    // a vein removed from the config is wiped: its chunks become the permanent null vein
                    boolean veinStillExists = storedId == NULL_VEIN_ID || ID_TO_VEINS.containsKey(storedId);
                    short effectiveId = veinStillExists ? storedId : NULL_VEIN_ID;

                    ListTag veinData = Nbt.getList(veins, veinTag, Tag.TAG_BYTE_ARRAY);
                    for (int veinOccurrences = 0; veinOccurrences < veinData.size(); ++veinOccurrences) {
                        byte[] rawVeinData = ((ByteArrayTag) veinData.get(veinOccurrences)).getAsByteArray();
                        if (rawVeinData.length < 2) { continue; }
                        short offset = (short) (((rawVeinData[0] & 0xFF) << 8) | (rawVeinData[1] & 0xFF));

                        short veinInfo;
                        if (effectiveId == NULL_VEIN_ID) { veinInfo = NULL_VEIN_ID; }
                        else { veinInfo = VeinCodec.compressVeinInfo(effectiveId, rawVeinData.length > 2 ? rawVeinData[2] : 0); }

                        currMegachunk.getRight().put(offset, veinInfo);
                        if (effectiveId != NULL_VEIN_ID && currMegachunk.getLeft().containsKey(effectiveId)) {
                            currMegachunk.getLeft().put(effectiveId, (short) (currMegachunk.getLeft().get(effectiveId) + 1));
                        }
                    }
                }

                recomputeRemainingWeight(dim, megachunkKey);
            }
        }
    }

    public void writeToNBT(CompoundTag tags) {
        tags.putInt("vein_format", STORAGE_FORMAT);
        tags.putShort("vein_it_id", iterationId);
        tags.putShort("vein_megachunk_length", megachunkLength);
        CompoundTag dims = new CompoundTag();

        var writtenDims = MEGA_CHUNK_OCCURRENCE_DATA.keySet();
        for (ResourceKey<Level> dim : writtenDims) {
            CompoundTag currDim = new CompoundTag();

            ListTag megachunks = new ListTag();
            currDim.put("megachunks", megachunks);

            for (long megachunkKey : MEGA_CHUNK_OCCURRENCE_DATA.get(dim).keySet()) {
                var currMegachunkData = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);
                CompoundTag currMegachunkNBT = new CompoundTag();
                currMegachunkNBT.putLong("key", megachunkKey);

                CompoundTag veins = new CompoundTag();
                currMegachunkNBT.put("veins", veins);

                for (var entry : MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey).getRight().short2ShortEntrySet()) {
                    short[] idQual = VeinCodec.splitVeinInfo(entry.getShortValue());
                    String veinTagID = getVeinInfoID(idQual[0]);
                    if (!veins.contains(veinTagID)) {
                        veins.put(veinTagID, new ListTag());
                    }

                    byte offsetX = (byte) (entry.getShortKey() >>> 8);
                    byte offsetZ = (byte) (entry.getShortKey());
                    byte qual = (byte) (idQual[1]);

                    byte[] bytesToStore;
                    if (idQual[0] == NULL_VEIN_ID) { bytesToStore = new byte[]{offsetX, offsetZ}; }
                    else { bytesToStore = new byte[]{offsetX, offsetZ, qual}; }

                    ListTag veinOffsetQualBEBytes = Nbt.getList(veins, veinTagID, Tag.TAG_BYTE_ARRAY);
                    veinOffsetQualBEBytes.add(new ByteArrayTag(bytesToStore));
                }

                megachunks.add(currMegachunkNBT);
            }

            dims.put(getDimInfoID(dim), currDim);
        }

        tags.put("vein_dims", dims);
    }
}
