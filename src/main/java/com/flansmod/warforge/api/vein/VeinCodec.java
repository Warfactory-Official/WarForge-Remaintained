package com.flansmod.warforge.api.vein;

import com.flansmod.warforge.common.item.ItemMatcher;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

/** Vein value packing shared by server storage and client display. */
public final class VeinCodec {
    /** 10,000 so a weight fraction fits a short. */
    public static final short WEIGHT_FRACTION_TENS_POW = 10000;
    public static final short NULL_VEIN_ID = (short) 0x00_00_FF_FF;

    private VeinCodec() {}

    public static short percentToShort(float percent) {
        return (short) Math.round(percent * WEIGHT_FRACTION_TENS_POW);
    }

    /** Assumes 4 significant figures; drops the decimal part when 0. */
    public static String shortToPercentStr(short percent) {
        int whole = percent / 100;
        int decimal = percent - whole * 100;
        return whole + (decimal > 0 ? "." + decimal + "%" : "%");
    }

    /** Per identical item component: {weight, guaranteed yield, percent yield}. Assumes comp and dim are present. */
    public static ArrayList<short[]> getYieldInfo(ItemMatcher comp, Pair<Vein, Quality> veinInfo, ResourceKey<Level> dim) {
        Vein vein = veinInfo.getLeft();
        Quality qual = veinInfo.getRight();
        ArrayList<Object2FloatOpenHashMap<ResourceKey<Level>>> yields = vein.compYields.get(comp);
        ArrayList<Object2ShortOpenHashMap<ResourceKey<Level>>> weights = vein.compWeights.get(comp);
        ArrayList<short[]> yieldInfos = new ArrayList<>(yields.size());

        for (int subCompIndex = 0; subCompIndex < yields.size(); ++subCompIndex) {
            float yield = yields.get(subCompIndex).getFloat(dim) * qual.getLocalMultiplier(vein);
            short guaranteed = (short) yield;
            yieldInfos.add(new short[]{weights.get(subCompIndex).getShort(dim), guaranteed, percentToShort(yield - guaranteed)});
        }
        return yieldInfos;
    }

    /** {id, quality}; the null vein id is also its info. */
    public static short[] splitVeinInfo(short veinInfo) {
        if (veinInfo == NULL_VEIN_ID) { return new short[]{NULL_VEIN_ID, (short) 7}; }
        return new short[]{(short) (veinInfo & 0x00_00_1F_FF), (short) ((veinInfo & 0x00_00_E0_00) >> 13)};
    }

    public static short compressVeinInfo(int veinId, int qualityIndex) {
        return (short) (veinId | (qualityIndex << 13));
    }

    public static short compressVeinInfo(Pair<Vein, Quality> veinInfo) {
        if (veinInfo == null) { return NULL_VEIN_ID; }
        return compressVeinInfo(veinInfo.getLeft().getId(), veinInfo.getRight().ordinal());
    }

    /** x in the high 32 bits, z in the low 32 bits; megachunk n covers [n*L, n*L+L-1] on both signs. */
    public static long megachunkKey(int chunkX, int chunkZ, int length) {
        return ((long) Math.floorDiv(chunkX, length) << 32) | (Math.floorDiv(chunkZ, length) & 0xFFFFFFFFL);
    }

    /** x offset in the high byte, z offset in the low byte; length <= 180 fits 8 bits. */
    public static short chunkOffset(int chunkX, int chunkZ, int length) {
        return (short) ((Math.floorMod(chunkX, length) << 8) | Math.floorMod(chunkZ, length));
    }

    public static int[] chunkCoords(short offset, long megachunkKey, int length) {
        return new int[]{(int) (megachunkKey >> 32) * length + ((offset >>> 8) & 0xFF), (int) megachunkKey * length + (offset & 0xFF)};
    }
}
