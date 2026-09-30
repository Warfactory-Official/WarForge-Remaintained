package com.flansmod.warforge.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/** Compressed NBT file I/O without a size limit. The NbtIo signatures differ between versions. */
public final class NbtFiles {
    private NbtFiles() {}

    public static CompoundTag readCompressed(Path file) throws IOException {
        return NbtIo.readCompressed(file.toFile());
    }

    public static CompoundTag readCompressed(InputStream in) throws IOException {
        return NbtIo.readCompressed(in);
    }

    public static void writeCompressed(CompoundTag tag, Path file) throws IOException {
        NbtIo.writeCompressed(tag, file.toFile());
    }

    public static void writeCompressed(CompoundTag tag, OutputStream out) throws IOException {
        NbtIo.writeCompressed(tag, out);
    }
}
