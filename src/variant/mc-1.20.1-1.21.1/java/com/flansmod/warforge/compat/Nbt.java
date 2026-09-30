package com.flansmod.warforge.compat;

import net.minecraft.nbt.CompoundTag;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;

import java.util.Set;
import java.util.UUID;

/**
 * NBT reads with the 1.21.1 meaning: a missing key (or a value of the wrong type) gives 0, "", false, an empty
 * compound or an empty list. From 1.21.5 the vanilla getters return Optional, so shared code reads through here.
 */
public final class Nbt {
    private Nbt() {}

    public static int getInt(CompoundTag tag, String key) { return tag.getInt(key); }
    public static long getLong(CompoundTag tag, String key) { return tag.getLong(key); }
    public static float getFloat(CompoundTag tag, String key) { return tag.getFloat(key); }
    public static double getDouble(CompoundTag tag, String key) { return tag.getDouble(key); }
    public static byte getByte(CompoundTag tag, String key) { return tag.getByte(key); }
    public static short getShort(CompoundTag tag, String key) { return tag.getShort(key); }
    public static boolean getBoolean(CompoundTag tag, String key) { return tag.getBoolean(key); }
    public static String getString(CompoundTag tag, String key) { return tag.getString(key); }
    public static int[] getIntArray(CompoundTag tag, String key) { return tag.getIntArray(key); }
    public static CompoundTag getCompound(CompoundTag tag, String key) { return tag.getCompound(key); }

    /** The list at key, or an empty list if it is missing or its elements are not of elementType (a Tag.TAG_* id). */
    public static ListTag getList(CompoundTag tag, String key, int elementType) { return tag.getList(key, elementType); }

    /** The compound at index, or an empty compound. */
    public static CompoundTag getCompound(ListTag list, int index) { return list.getCompound(index); }

    /** True if key holds a UUID (an int array of length 4). */
    public static boolean hasUUID(CompoundTag tag, String key) { return tag.hasUUID(key); }
    public static UUID getUUID(CompoundTag tag, String key) { return tag.getUUID(key); }
    /** Stores a UUID as an int array of length 4 (the same bytes on every version). */
    public static void putUUID(CompoundTag tag, String key, UUID value) { tag.putUUID(key, value); }

    public static Set<String> keys(CompoundTag tag) { return tag.getAllKeys(); }

    /** The Tag.TAG_* id of the value at key, or 0 if it is missing. */
    public static int tagType(CompoundTag tag, String key) { return tag.getTagType(key); }

    /** Parses SNBT text into a compound. */
    public static CompoundTag parse(String snbt) throws CommandSyntaxException {
        return TagParser.parseTag(snbt);
    }
}
