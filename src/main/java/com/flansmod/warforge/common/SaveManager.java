package com.flansmod.warforge.common;

import com.flansmod.warforge.compat.NbtFiles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** warforgefactions.dat: snapshot on the server thread, gzip + write on one writer thread. */
public final class SaveManager {
    private static final String FILE = "warforgefactions.dat";
    private static final long STOP_TIMEOUT_SECONDS = 30;

    private record Pending(CompoundTag tags, Path file, String event) {}

    private static final AtomicReference<Pending> PENDING = new AtomicReference<>();
    private static ExecutorService executor;

    private SaveManager() {}

    private static Path file() {
        return WarForgeMod.MC_SERVER.getWorldPath(LevelResource.ROOT).resolve(FILE);
    }

    private static Path backup(Path file) {
        return file.resolveSibling(FILE + ".bak");
    }

    /** Reads the factions file, else its backup. A missing file is created empty. */
    public static CompoundTag load() throws IOException {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            Files.createDirectories(file.getParent());
            write(new CompoundTag(), file);
        }
        try {
            return read(file);
        } catch (IOException | RuntimeException e) {
            Path backup = backup(file);
            if (!Files.isRegularFile(backup)) {
                throw e;
            }
            WarForgeMod.LOGGER.error("{} is unreadable; loading {}", FILE, backup.getFileName(), e);
            return read(backup);
        }
    }

    private static CompoundTag read(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            return NbtFiles.readCompressed(input);
        }
    }

    private static synchronized ExecutorService executor() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "WarForge Save Writer");
                thread.setDaemon(true);
                return thread;
            });
        }
        return executor;
    }

    private static Pending snapshot(String event) {
        CompoundTag tags = new CompoundTag();
        WarForgeMod.writeState(tags);
        return new Pending(tags, file(), event);
    }

    /** Queues a write; a snapshot still waiting is replaced by this newer one. */
    public static void save(String event) {
        PENDING.set(snapshot(event));
        executor().execute(() -> {
            Pending next = PENDING.getAndSet(null);
            if (next == null) {
                return;
            }
            try {
                write(next);
            } catch (IOException e) {
                WarForgeMod.LOGGER.error("Failed to save {} on event {}", FILE, next.event(), e);
            }
        });
    }

    /** Server stop: finishes the writer, then writes the final snapshot on this thread. */
    public static void saveAndStop(String event) {
        Pending last = snapshot(event);
        ExecutorService writer = executor();
        writer.shutdown();
        try {
            if (!writer.awaitTermination(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                WarForgeMod.LOGGER.warn("WarForge save writer did not finish within {}s of shutdown", STOP_TIMEOUT_SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        PENDING.set(null);
        try {
            write(last);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save " + FILE, e);
        }
    }

    private static void write(Pending pending) throws IOException {
        Files.createDirectories(pending.file().getParent());
        if (Files.exists(pending.file())) {
            Files.copy(pending.file(), backup(pending.file()), StandardCopyOption.REPLACE_EXISTING);
        }
        write(pending.tags(), pending.file());
        WarForgeMod.LOGGER.info("Saved {} on event {}", FILE, pending.event());
    }

    private static void write(CompoundTag tags, Path file) throws IOException {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temp)) {
            NbtFiles.writeCompressed(tags, output);
        }
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
