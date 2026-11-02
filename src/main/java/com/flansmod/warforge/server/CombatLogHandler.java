package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarClock;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.compat.NbtFiles;
import com.flansmod.warforge.compat.PlayerDataFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Players who log out while their faction is in a siege and stay away longer than
 * COMBAT_LOG_THRESHOLD lose their inventory at the logout position. Logging back in cancels it.
 */
public final class CombatLogHandler {
    private record Logout(DimBlockPos position, long timestamp) {
    }

    private final Map<UUID, Logout> pending = new LinkedHashMap<>();

    void onLogout(ServerPlayer player) {
        pending.put(player.getUUID(), new Logout(new DimBlockPos(player), WarClock.gameTimeMs));
    }

    void onLogin(UUID playerId) {
        pending.remove(playerId);
    }

    /** Once per server tick. */
    public void tick() {
        if (pending.isEmpty()) {
            return;
        }
        long now = WarClock.gameTimeMs;
        Iterator<Map.Entry<UUID, Logout>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Logout> entry = it.next();
            if (now - entry.getValue().timestamp() > WarForgeConfig.COMBAT_LOG_THRESHOLD) {
                it.remove();
                enforce(entry.getKey(), entry.getValue().position());
            }
        }
    }

    private static void enforce(UUID playerId, DimBlockPos logoffPos) {
        MinecraftServer server = WarForgeMod.MC_SERVER;
        Path playerDataFile = server.getWorldPath(LevelResource.ROOT).resolve("playerdata/" + playerId + ".dat");
        ServerLevel world = server.getLevel(logoffPos.dim);
        if (world == null || !Files.isRegularFile(playerDataFile)) {
            return;
        }
        try {
            CompoundTag playerData = NbtFiles.readCompressed(playerDataFile);
            List<ItemStack> items = PlayerDataFile.takeInventory(playerData, server.registryAccess());
            NbtFiles.writeCompressed(playerData, playerDataFile);
            for (ItemStack stack : items) {
                world.addFreshEntity(new ItemEntity(world, logoffPos.getX(), logoffPos.getY(), logoffPos.getZ(), stack));
            }
        } catch (IOException e) {
            WarForgeMod.LOGGER.error("Combat-log enforcement failed for {}", playerId, e);
        }
    }
}
