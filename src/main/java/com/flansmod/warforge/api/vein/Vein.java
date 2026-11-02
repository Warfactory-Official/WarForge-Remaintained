package com.flansmod.warforge.api.vein;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.item.ItemMatcher;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import com.flansmod.warforge.compat.Keys;

public class Vein {
    public final String translationKey;
    public final float[] qualMults;

    @Nonnull
    public final LinkedHashSet<ItemMatcher> compIds;  // if this is null, then nothing is produced
    @Nonnull
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, short[]> dimWeights;  // weight out of 10,000 and expected weight - inherently stores valid dim info
    @Nonnull
    public final Object2ObjectOpenHashMap<ItemMatcher, ArrayList<Object2ShortOpenHashMap<ResourceKey<Level>>>> compWeights;
    @Nonnull
    public final Object2ObjectOpenHashMap<ItemMatcher, ArrayList<Object2FloatOpenHashMap<ResourceKey<Level>>>> compYields;  // compId -> dim -> yield as Float (multiplier applied already)

    // Packet Data:
    public final FriendlyByteBuf SERIALIZED_ENTRY;  // null on client
    private final short id;
    public final int wealth;  // relative wealth a claimed chunk holding this vein adds to a faction

    /** Client copy decoded from the wire; keeps no serialized form. */
    public Vein(final FriendlyByteBuf veinEntryBuf, short megachunkLength) {
        this(VeinEntry.deserialize(veinEntryBuf), (short) (megachunkLength * megachunkLength), false);
    }

    /** Server definition; keeps the serialized form for sync. */
    public Vein(final VeinEntry veinEntry, short megachunkArea) {
        this(veinEntry, megachunkArea, true);
    }

    private Vein(final VeinEntry veinEntry, short megachunkArea, boolean keepSerialized) {
        SERIALIZED_ENTRY = keepSerialized ? veinEntry.serialize() : null;

        this.translationKey = veinEntry.translationKey;
        qualMults = veinEntry.qualMults;
        id = veinEntry.id;
        wealth = veinEntry.wealth;
        dimWeights = new Object2ObjectOpenHashMap<>(veinEntry.dimWeights.size());
        dimWeights.defaultReturnValue(new short[]{0, 0});

        for (VeinEntry.DimWeight dimWeight : veinEntry.dimWeights.values()) {
            dimWeights.put(dimWeight.dim, new short[]{ dimWeight.weight,
                    (short) (megachunkArea * dimWeight.weight / VeinCodec.WEIGHT_FRACTION_TENS_POW)});
        }

        this.compIds = new LinkedHashSet<>(veinEntry.components.size());
        this.compWeights = new Object2ObjectOpenHashMap<>(veinEntry.components.size());
        this.compYields = new Object2ObjectOpenHashMap<>(veinEntry.components.size());
        this.compYields.defaultReturnValue(null);
        for (VeinEntry.Component component : veinEntry.components){
            // we want to map each stack comparable component to some list of weights and yields
            // a component's item may be repeated to give different chances to get a range of values
            // for a set of components with equivalent items, the stack comparable of the first is preferred and stored
            var stackComparable = ItemMatcher.parse(component.item);
            if (stackComparable == null) {
                WarForgeMod.LOGGER.warn("Vein component '{}' did not resolve to a known item or tag; skipping.", component.item);
                continue;
            }
            if (!compIds.contains(stackComparable)) {
                compIds.add(stackComparable);
                compWeights.put(stackComparable, new ArrayList<>(1));
                compYields.put(stackComparable, new ArrayList<>(1));
            }

            component.weights.defaultReturnValue((short) 10000);  // for any dimensions not present
            compWeights.get(stackComparable).add(component.weights);  // store the weights

            Object2FloatOpenHashMap<ResourceKey<Level>> yields = new Object2FloatOpenHashMap<>();
            yields.defaultReturnValue(component.yield);  // default is yield * default mult [1.0]

            if (component.multipliers != null) {
                for (var entry : component.multipliers.object2FloatEntrySet()) {
                    yields.put(entry.getKey(), component.yield * entry.getFloatValue());
                }
            }

            for (var entry : veinEntry.dimWeights.object2ObjectEntrySet()) {
                if (yields.containsKey(entry.getKey())) { continue; }
                yields.put(entry.getKey(), component.yield * entry.getValue().multiplier);
            }

            compYields.get(stackComparable).add(yields);  // store the yields
        }
    }

    public short getDimWeight(ResourceKey<Level> dim) { return dimWeights.get(dim)[0]; }
    public short getDimExpectedCount(ResourceKey<Level> dim) { return dimWeights.get(dim)[1]; }
    public ObjectSet<ResourceKey<Level>> getValidDims() { return dimWeights.keySet(); }
    public short getId() { return id; }

    private VeinView view;

    public VeinView view() {
        if (view == null) {
            HashMap<ResourceKey<Level>, Double> weights = new HashMap<>();
            for (var entry : dimWeights.object2ObjectEntrySet()) {
                weights.put(entry.getKey(), entry.getValue()[0] / (double) VeinCodec.WEIGHT_FRACTION_TENS_POW);
            }
            ArrayList<VeinView.Component> components = new ArrayList<>(compIds.size());
            for (ItemMatcher comp : compIds) {
                components.add(new VeinView.Component(comp.id(), compYields.get(comp).get(0).defaultReturnValue()));
            }
            view = new VeinView(id, translationKey, wealth, Map.copyOf(weights), List.copyOf(components));
        }
        return view;
    }

    public String toString() {
        String dimIds = this.dimWeights.keySet().stream()
                .map(dim -> Keys.id(dim).toString())
                .collect(Collectors.joining(", ", "{", "}"));

        String dimWeights = this.dimWeights.values().stream()
                .map(dw -> String.format("%.4f", ((float) dw[0]) / 10000) + "; EXP: " + dw[1])
                .collect(Collectors.joining(", ", "{", "}"));

        String components = this.compIds.stream()
                .map(compId -> compId.toString() + ": [" + compWeights.get(compId).toString() + "; " + compYields.get(compId).toString() + "]")
                .collect(Collectors.joining(", ", "{", "}"));

        return "ID: " + getId() + "DATA: " + String.join(", ", translationKey, dimIds, dimWeights, "COMP DATA: \n", components);
    }
}
