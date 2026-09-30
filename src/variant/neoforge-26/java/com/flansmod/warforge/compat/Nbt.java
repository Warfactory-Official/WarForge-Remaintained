package com.flansmod.warforge.compat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

import java.util.Set;
import java.util.UUID;

/**
 * NBT reads with the 1.21.1 meaning: a missing key (or a value of the wrong type) gives 0, "", false, an empty
 * compound or an empty list. From 1.21.5 the vanilla getters return Optional, so shared code reads through here.
 */
public final class Nbt {
    private Nbt() {}

    public static int getInt(CompoundTag tag, String key) { return tag.getIntOr(key, 0); }
    public static long getLong(CompoundTag tag, String key) { return tag.getLongOr(key, 0L); }
    public static float getFloat(CompoundTag tag, String key) { return tag.getFloatOr(key, 0f); }
    public static double getDouble(CompoundTag tag, String key) { return tag.getDoubleOr(key, 0d); }
    public static byte getByte(CompoundTag tag, String key) { return tag.getByteOr(key, (byte) 0); }
    public static short getShort(CompoundTag tag, String key) { return tag.getShortOr(key, (short) 0); }
    public static boolean getBoolean(CompoundTag tag, String key) { return tag.getBooleanOr(key, false); }
    public static String getString(CompoundTag tag, String key) { return tag.getStringOr(key, ""); }
    public static int[] getIntArray(CompoundTag tag, String key) { return tag.getIntArray(key).orElseGet(() -> new int[0]); }
    public static CompoundTag getCompound(CompoundTag tag, String key) { return tag.getCompoundOrEmpty(key); }

    /** The list at key, or an empty list if it is missing or its elements are not of elementType (a Tag.TAG_* id). */
    public static ListTag getList(CompoundTag tag, String key, int elementType) {
        ListTag list = tag.getListOrEmpty(key);
        for (Tag element : list) {
            if (element.getId() != elementType) return new ListTag();
        }
        return list;
    }

    /** The compound at index, or an empty compound. */
    public static CompoundTag getCompound(ListTag list, int index) { return list.getCompoundOrEmpty(index); }

    /** True if key holds a UUID (an int array of length 4). */
    public static boolean hasUUID(CompoundTag tag, String key) {
        return tag.get(key) instanceof IntArrayTag array && array.getAsIntArray().length == 4;
    }

    public static UUID getUUID(CompoundTag tag, String key) {
        if (!(tag.get(key) instanceof IntArrayTag array) || array.getAsIntArray().length != 4) {
            throw new IllegalArgumentException("Expected UUID-Tag to be of type IntArrayTag with length 4 at " + key);
        }
        return UUIDUtil.uuidFromIntArray(array.getAsIntArray());
    }

    /** Stores a UUID as an int array of length 4 (the same bytes on every version). */
    public static void putUUID(CompoundTag tag, String key, UUID value) {
        tag.putIntArray(key, UUIDUtil.uuidToIntArray(value));
    }

    public static Set<String> keys(CompoundTag tag) { return tag.keySet(); }

    /** The Tag.TAG_* id of the value at key, or 0 if it is missing. */
    public static int tagType(CompoundTag tag, String key) {
        Tag value = tag.get(key);
        return value == null ? 0 : value.getId();
    }

    /** Parses SNBT text into a compound. */
    public static CompoundTag parse(String snbt) throws CommandSyntaxException {
        return TagParser.parseCompoundFully(snbt);
    }
}
