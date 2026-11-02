package com.flansmod.warforge.api.vein;

import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.compat.Keys;
import com.electronwill.nightconfig.core.Config;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class VeinEntry {
    final public short id;
    final public String translationKey;
    public float[] qualMults;
    public final byte qualOverrideCount;

    final public Object2ObjectOpenHashMap<ResourceKey<Level>, DimWeight> dimWeights;
    final public List<Component> components;
    final public int wealth;

    public VeinEntry(short id, String translationKey, Object2FloatOpenHashMap<Quality> qualMults, Object2ObjectOpenHashMap<ResourceKey<Level>, DimWeight> dimWeights, List<Component> components, int wealth) {
        this.id = id;
        this.translationKey = translationKey;
        this.dimWeights = dimWeights;
        this.components = components;
        this.wealth = wealth;
        if (qualMults == null) {
            this.qualMults = null;
            qualOverrideCount = 0;
            return;
        }

        this.qualMults = new float[Quality.values().length];
        byte qualOverrideCount = 0;
        for (Quality qual : Quality.values()) {
            if (qualMults.containsKey(qual)) {
                this.qualMults[qual.ordinal()] = qualMults.getFloat(qual);
                ++qualOverrideCount;
            } else {
                this.qualMults[qual.ordinal()] = -1f;
            }
        }

        this.qualOverrideCount = qualOverrideCount;
    }

    public FriendlyByteBuf serialize() {
        FriendlyByteBuf entryByteBuf = new FriendlyByteBuf(Unpooled.buffer());

        entryByteBuf.writeShort(id);
        entryByteBuf.writeUtf(translationKey);
        entryByteBuf.writeInt(wealth);

        entryByteBuf.writeByte(qualOverrideCount);
        if (qualMults != null) {
            for (byte qualOrd = 0; qualOrd < qualMults.length; ++qualOrd) {
                float qualMult = qualMults[qualOrd];
                if (qualMult == -1) { continue; }  // -1 => default
                entryByteBuf.writeByte(qualOrd);
                entryByteBuf.writeFloat(qualMult);
            }
        }

        entryByteBuf.writeInt(dimWeights.size());
        dimWeights.forEach((dimKey, dimWeight) -> dimWeight.serialize(entryByteBuf));

        entryByteBuf.writeInt(components.size());
        for (Component comp : components) {
            entryByteBuf.writeBytes(comp.serialize());
        }

        return entryByteBuf;
    }

    public static VeinEntry deserialize(FriendlyByteBuf buf) {
        short id = buf.readShort();
        String translationKey = buf.readUtf();
        int wealth = buf.readInt();

        int numQualOverrides = buf.readByte();
        Object2FloatOpenHashMap<Quality> qualMappings = null;
        if (numQualOverrides > 0) { qualMappings = new Object2FloatOpenHashMap<>(numQualOverrides); }

        for (; numQualOverrides > 0; --numQualOverrides) {
            qualMappings.put(Quality.getQuality(buf.readByte()), buf.readFloat());
        }

        int numDims = buf.readInt();
        Object2ObjectOpenHashMap<ResourceKey<Level>, DimWeight> dimWeights = new Object2ObjectOpenHashMap<>(numDims);
        for (int i = 0; i < numDims; ++i) {
            DimWeight dw = DimWeight.deserialize(buf);
            dimWeights.put(dw.dim, dw);
        }

        int numComps = buf.readInt();
        List<Component> comps = new ArrayList<>(numComps);
        for (int i = 0; i < numComps; ++i) {
            comps.add(Component.deserialize(buf));
        }

        return new VeinEntry(id, translationKey, qualMappings, dimWeights, comps, wealth);
    }

    @AllArgsConstructor
    public static class DimWeight {
        final public ResourceKey<Level> dim;
        final public short weight;
        final public float multiplier;

        public void serialize(FriendlyByteBuf buf) {
            buf.writeUtf(Keys.id(dim).toString());
            buf.writeShort(weight);
            buf.writeFloat(multiplier);
        }

        public static DimWeight deserialize(FriendlyByteBuf buf) {
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Ids.parse(buf.readUtf()));
            return new DimWeight(dim, buf.readShort(), buf.readFloat());
        }
    }

    @AllArgsConstructor
    public static class Component {
        final public String item;
        final public float yield;
        final public Object2ShortOpenHashMap<ResourceKey<Level>> weights;
        final public Object2FloatOpenHashMap<ResourceKey<Level>> multipliers;

        public static Object2FloatOpenHashMap<ResourceKey<Level>> parseFloatMap(List<? extends Config> multEntries) {
            Object2FloatOpenHashMap<ResourceKey<Level>> result = new Object2FloatOpenHashMap<>();
            if (multEntries == null) { return result; }
            for (Config dimEntry : multEntries) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Ids.parse((String) dimEntry.get("id")));
                result.put(key, ((Number) dimEntry.get("mult")).floatValue());
            }
            return result;
        }

        public static Object2ShortOpenHashMap<ResourceKey<Level>> parseMapF2S(List<? extends Config> weightEntries) {
            Object2ShortOpenHashMap<ResourceKey<Level>> result = new Object2ShortOpenHashMap<>();
            if (weightEntries == null) { return result; }
            for (Config dimEntry : weightEntries) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Ids.parse((String) dimEntry.get("id")));
                result.put(key, VeinCodec.percentToShort(((Number) dimEntry.get("weight")).floatValue()));
            }
            return result;
        }

        public FriendlyByteBuf serialize() {
            FriendlyByteBuf serialData = new FriendlyByteBuf(Unpooled.buffer());
            serialData.writeUtf(item);
            serialData.writeFloat(yield);

            serialData.writeInt(weights.size());
            for (var entry : weights.object2ShortEntrySet()) {
                serialData.writeUtf(Keys.id(entry.getKey()).toString());
                serialData.writeShort(entry.getShortValue());
            }

            serialData.writeInt(multipliers.size());
            for (var entry : multipliers.object2FloatEntrySet()) {
                serialData.writeUtf(Keys.id(entry.getKey()).toString());
                serialData.writeFloat(entry.getFloatValue());
            }

            return serialData;
        }

        public static Component deserialize(FriendlyByteBuf buf) {
            String item = buf.readUtf();
            float yield = buf.readFloat();

                int numWeights = buf.readInt();
            Object2ShortOpenHashMap<ResourceKey<Level>> weights = new Object2ShortOpenHashMap<>(numWeights);
            for (int i = 0; i < numWeights; ++i) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Ids.parse(buf.readUtf()));
                weights.put(key, buf.readShort());
            }

            int numMults = buf.readInt();
            Object2FloatOpenHashMap<ResourceKey<Level>> multipliers = new Object2FloatOpenHashMap<>(numMults);
            for (int i = 0; i < numMults; ++i) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Ids.parse(buf.readUtf()));
                multipliers.put(key, buf.readFloat());
            }

            return new Component(item, yield, weights, multipliers);
        }
    }
}
