package com.flansmod.warforge.compat;

import net.minecraft.nbt.CompoundTag;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;

import java.util.Set;
import java.util.UUID;

/** Missing or wrong-typed key => 0, "", false, empty compound/list (1.21.1 semantics); vanilla getters return Optional from 1.21.5. */
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

    public static ListTag getList(CompoundTag tag, String key, int elementType) { return tag.getList(key, elementType); }

    public static CompoundTag getCompound(ListTag list, int index) { return list.getCompound(index); }

    public static boolean hasUUID(CompoundTag tag, String key) { return tag.hasUUID(key); }
    public static UUID getUUID(CompoundTag tag, String key) { return tag.getUUID(key); }
    public static void putUUID(CompoundTag tag, String key, UUID value) { tag.putUUID(key, value); }

    public static Set<String> keys(CompoundTag tag) { return tag.getAllKeys(); }

    public static int tagType(CompoundTag tag, String key) { return tag.getTagType(key); }

    public static CompoundTag parse(String snbt) throws CommandSyntaxException {
        return TagParser.parseTag(snbt);
    }
}
