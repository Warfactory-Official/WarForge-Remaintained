package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketFlagChunk;
import com.flansmod.warforge.common.network.PacketFlagManifest;
import com.flansmod.warforge.common.network.SyncQueueHandler;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerFlagRegistry {
    public static final int MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;
    public static final int MAX_DIMENSION = 256;
    private static final int CHUNK_SIZE = 30_000;

    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "WarForge-FlagSync");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, CustomFlagData> customFlags = new HashMap<String, CustomFlagData>();

    public void reload() {
        customFlags.clear();
        Path dir = getCustomFlagsDir();
        try {
            Files.createDirectories(dir);
            Files.list(dir)
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(this::tryLoadCustomFlag);
        } catch (IOException e) {
            WarForgeMod.LOGGER.error("Failed to reload custom flags", e);
        }
    }

    public boolean isAvailable(String flagId) {
        if (flagId == null || flagId.isEmpty()) {
            return false;
        }
        if (flagId.startsWith("default:")) {
            return WarForgeConfig.isDefaultFlagAvailable(flagId.substring("default:".length()));
        }
        if (flagId.startsWith("custom:")) {
            return customFlags.containsKey(flagId.substring("custom:".length()));
        }
        return false;
    }

    public List<String> getAvailableFlagIds() {
        ArrayList<String> result = new ArrayList<String>();
        for (String id : WarForgeConfig.DEFAULT_FLAG_IDS) {
            if (id != null && !id.trim().isEmpty()) {
                result.add("default:" + id.trim());
            }
        }
        ArrayList<String> customIds = new ArrayList<String>(customFlags.keySet());
        Collections.sort(customIds);
        for (String id : customIds) {
            result.add("custom:" + id);
        }
        return result;
    }

    public void syncToPlayer(EntityPlayerMP player) {
        PacketFlagManifest manifest = new PacketFlagManifest();
        manifest.flagIds.addAll(getAvailableFlagIds());
        WarForgeMod.NETWORK.sendTo(manifest, player);

        for (CustomFlagData data : customFlags.values()) {
            syncExecutor.submit(() -> enqueueCustomFlagPackets(player, data));
        }
    }

    public void shutdown() {
        syncExecutor.shutdownNow();
    }

    private void enqueueCustomFlagPackets(EntityPlayerMP player, CustomFlagData data) {
        int totalChunks = Math.max(1, (int) Math.ceil(data.bytes.length / (double) CHUNK_SIZE));
        for (int index = 0; index < totalChunks; index++) {
            final int start = index * CHUNK_SIZE;
            final int end = Math.min(data.bytes.length, start + CHUNK_SIZE);
            final byte[] chunk = new byte[end - start];
            System.arraycopy(data.bytes, start, chunk, 0, chunk.length);
            final PacketFlagChunk packet = new PacketFlagChunk();
            packet.flagId = "custom:" + data.id;
            packet.width = data.width;
            packet.height = data.height;
            packet.partIndex = index;
            packet.totalParts = totalChunks;
            packet.data = chunk;
            SyncQueueHandler.enqueue(() -> {
                if (WarForgeMod.MC_SERVER.getPlayerList().getPlayerByUUID(player.getUniqueID()) != null) {
                    WarForgeMod.NETWORK.sendTo(packet, player);
                }
            });
        }
    }

    private void tryLoadCustomFlag(Path file) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return;
        }
        String id = fileName.substring(0, dot);
        if (!WarForgeConfig.isCustomFlagAvailable(id)) {
            return;
        }
        try {
            long size = Files.size(file);
            if (size <= 0 || size > MAX_FILE_SIZE_BYTES) {
                WarForgeMod.LOGGER.warn("Skipping custom flag {} due to file size {}", fileName, size);
                return;
            }
            byte[] bytes = Files.readAllBytes(file);
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                WarForgeMod.LOGGER.warn("Skipping custom flag {} because it is not a readable image", fileName);
                return;
            }
            if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
                WarForgeMod.LOGGER.warn("Skipping custom flag {} because dimensions {}x{} exceed {}x{}", fileName, image.getWidth(), image.getHeight(), MAX_DIMENSION, MAX_DIMENSION);
                return;
            }
            customFlags.put(id, new CustomFlagData(id, image.getWidth(), image.getHeight(), bytes));
        } catch (IOException e) {
            WarForgeMod.LOGGER.error("Failed to load custom flag " + fileName, e);
        }
    }

    public static Path getCustomFlagsDir() {
        return Paths.get("resources", "warforge", "flags");
    }

    private static final class CustomFlagData {
        private final String id;
        private final int width;
        private final int height;
        private final byte[] bytes;

        private CustomFlagData(String id, int width, int height, byte[] bytes) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.bytes = bytes;
        }
    }
}
