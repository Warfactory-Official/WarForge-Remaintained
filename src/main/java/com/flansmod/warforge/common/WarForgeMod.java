package com.flansmod.warforge.common;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.WarForgeCapabilities;
import com.flansmod.warforge.common.blocks.BlockFob;
import com.flansmod.warforge.common.blocks.IMultiBlockInit;
import com.flansmod.warforge.common.blocks.ClaimBlockEntity;
import com.flansmod.warforge.common.blocks.FobBlockEntity;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.PacketHandler;
import com.flansmod.warforge.common.ui.UiRegistration;
import com.flansmod.warforge.compat.Chunks;
import com.flansmod.warforge.compat.Profiles;
import com.flansmod.warforge.platform.BlockBreak;
import com.flansmod.warforge.platform.GameEvents;
import com.flansmod.warforge.platform.NetworkPlatform;
import com.flansmod.warforge.platform.Platform;
import com.flansmod.warforge.server.CommandFactions;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionChunkLoadingManager;
import com.flansmod.warforge.server.FactionStorage;
import com.flansmod.warforge.server.JourneyMapVeinSync;
import com.flansmod.warforge.server.Leaderboard;
import com.flansmod.warforge.server.ServerFlagRegistry;
import com.flansmod.warforge.server.ServerTickHandler;
import com.flansmod.warforge.server.SpawnModule;
import com.flansmod.warforge.server.TeleportsModule;
import com.flansmod.warforge.server.UpgradeHandler;
import com.flansmod.warforge.server.fob.FobManager;
import com.flansmod.warforge.server.vein.VeinConfigHandler;
import com.flansmod.warforge.server.vein.VeinUtils;
import com.flansmod.warforge.common.util.FactionDisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

/** The mod. Created by the loader entry point (platform.WarForgeNeoForge / platform.WarForgeForge). */
public class WarForgeMod {
    public static final PacketHandler NETWORK = new PacketHandler();
    public static final Leaderboard LEADERBOARD = new Leaderboard();
    public static final FactionStorage FACTIONS = new FactionStorage();
    public static final JourneyMapVeinSync JOURNEYMAP_VEIN_SYNC = new JourneyMapVeinSync();
    public static final Content CONTENT = new Content();
    public static final ProtectionsModule PROTECTIONS = new ProtectionsModule();
    public static final TeleportsModule TELEPORTS = new TeleportsModule();
    public static final UpgradeHandler UPGRADE_HANDLER = new UpgradeHandler();
    public static final FactionChunkLoadingManager CHUNK_LOADING_MANAGER = new FactionChunkLoadingManager();
    public static final FobManager FOBS = new FobManager();
    public static final ServerFlagRegistry FLAG_REGISTRY = new ServerFlagRegistry();
    public static VeinUtils VEIN_HANDLER = null;

    public static WarForgeMod INSTANCE;
    public static CommonProxy proxy = new CommonProxy();

    public static Logger LOGGER;
    public static MinecraftServer MC_SERVER = null;

    /**
     * Flashback plays replays through its own integrated server. WarForge stays inert there: the recording already
     * carries every packet the viewer should see, and live logic on stale or empty state would send it bogus HUD data.
     */
    public static boolean inReplayServer() {
        return MC_SERVER != null && MC_SERVER.getClass().getName().startsWith("com.moulberry.flashback.");
    }

    public static Random rand = new Random();

    public WarForgeMod(IEventBus modBus) {
        INSTANCE = this;
        LOGGER = LogManager.getLogger(Tags.MODID);
        WarClock.start();

        Content.register(modBus);
        Sounds.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener((ModConfigEvent event) -> {
            if (event.getConfig().getSpec() == WarForgeConfig.SPEC) {
                WarForgeConfig.bake();
            }
        });
        WarForgeCapabilities.register(modBus);
        CHUNK_LOADING_MANAGER.initialize(modBus);
        NetworkPlatform.register(modBus);

        Platform.gameBus().register(this);
        GameEvents.onServerTickEnd(new ServerTickHandler()::onTick);
        Platform.gameBus().register(PROTECTIONS);
        GameEvents.onBlockBreak(this::blockRemoved);
        GameEvents.onBlockBreak(PROTECTIONS::blockRemoved);
        GameEvents.onLivingDamage(PROTECTIONS::onDamage);
        Platform.gameBus().register(new SpawnModule());
        Platform.gameBus().register(WarForgeTabListHandler.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UiRegistration.register();
            Content.bake();
            IMultiBlockInit.registerMaps();
            WarForgeConfig.onRegistriesReady();
            loadUpgradeConfig();
            if (ModList.get().isLoaded("golemcore")) com.flansmod.warforge.compat.golem.GolemProtection.register();
            if (ModList.get().isLoaded("ftbteams")) com.flansmod.warforge.compat.ftbteams.FtbTeamsLink.register();
        });
    }

    public static String reloadServerConfig() {
        WarForgeConfig.reloadFromDisk();

        String levelStatus;
        try {
            loadUpgradeConfig();
            levelStatus = "levels ok";
        } catch (RuntimeException e) {
            LOGGER.error("Failed to reload citadel upgrade levels", e);
            levelStatus = "levels FAILED";
        }

        String veinStatus = reloadVeins();

        FLAG_REGISTRY.reload();

        if (MC_SERVER != null) {
            for (ServerPlayer player : MC_SERVER.getPlayerList().getPlayers()) {
                PlayerSyncService.resync(player);
            }
        }

        return "main config ok, " + levelStatus + ", " + veinStatus;
    }

    private static String reloadVeins() {
        try {
            CompoundTag snapshot = new CompoundTag();
            VEIN_HANDLER.writeToNBT(snapshot);
            VeinConfigHandler.writeStubIfEmpty();
            VeinUtils reloaded = VeinConfigHandler.loadVeins();
            reloaded.readFromNBT(snapshot);
            VEIN_HANDLER = reloaded;
            FACTIONS.recalculateAllWealth();
            return "veins ok";
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to reload veins", e);
            return "veins FAILED";
        }
    }

    private static void loadUpgradeConfig() {
        if (!WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
            return;
        }

        Path configFile = Platform.configDir().resolve(Tags.MODID).resolve("upgrade_levels.toml");
        try {
            UpgradeHandler.writeStubIfEmpty(configFile);
            UpgradeHandler.parseConfig(configFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load citadel upgrade config", e);
        }
    }

    public static boolean isOp(Player player) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && Profiles.isOp(server, player);
    }

    static void readState(CompoundTag tags) {
        FACTIONS.readFromNBT(tags);
        if (VEIN_HANDLER != null) {
            VEIN_HANDLER.readFromNBT(tags);
        }
        WarClock.read(tags);
    }

    static void writeState(CompoundTag tags) {
        FACTIONS.writeToNBT(tags);
        if (VEIN_HANDLER != null) {
            VEIN_HANDLER.writeToNBT(tags);
        }
        WarClock.write(tags);
    }

    @SubscribeEvent
    public void fobPickupInteract(RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!player.isSecondaryUseActive()) {
            return;
        }
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof BlockFob)
                || !(level.getBlockEntity(event.getPos()) instanceof FobBlockEntity fobTe)) {
            return;
        }
        event.setCanceled(true);
        BlockFob.tryPickUp(level, event.getPos(), player, fobTe);
    }

    @SubscribeEvent
    public void playerDied(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            FACTIONS.siegeService.onPlayerDied(player, event.getSource());
            FACTIONS.sieges.removeAttackerPresence(player.getUUID());
        }
    }

    @SubscribeEvent
    public void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            FACTIONS.sieges.updateAttackerPresence(player);
        }
    }

    @SubscribeEvent
    public void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            FACTIONS.sieges.updateAttackerPresence(player);
        }
    }

    public void blockRemoved(BlockBreak event) {
        BlockState state = event.getState();
        if (Content.isClaimBlock(state.getBlock())) {
            Level level = (Level) event.getLevel();
            if (level.getBlockEntity(event.getPos()) instanceof ClaimBlockEntity te && te.getFaction().equals(FactionIds.NONE)) {
                return;
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void preBlockPlaced(RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        Component denial = ClaimPlacementRules.check(player, level, event.getItemStack(), event.getPos(), event.getFace());
        if (denial != null) {
            player.sendSystemMessage(denial);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void playerLeftGame(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            FACTIONS.membership.onLoggedOut(player);
            JOURNEYMAP_VEIN_SYNC.onPlayerLogout(player);
            com.flansmod.warforge.common.network.PacketSecurity.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onChunkObserved(ChunkWatchEvent.Watch event) {
        if (inReplayServer()) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        ChunkPos chunkPos = event.getPos();
        ResourceKey<Level> dim = event.getLevel().dimension();
        JOURNEYMAP_VEIN_SYNC.onChunkObserved(player, dim, Chunks.x(chunkPos), Chunks.z(chunkPos));
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (!WarForgeConfig.FACTION_PREFIX_IN_CHAT) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        Faction faction = FACTIONS.registry.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            return;
        }
        Component prefix = FactionDisplay.factionPrefix(faction);
        if (prefix == null) {
            return;
        }
        Component line = Component.empty()
                .append(prefix)
                .append(Component.literal("<").append(player.getDisplayName()).append("> "))
                .append(event.getMessage());
        event.setCanceled(true);
        MC_SERVER.getPlayerList().broadcastSystemMessage(line, false);
    }

    @SubscribeEvent
    public void playerJoinedGame(PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide() || inReplayServer()) {
            return;
        }

        if (!Double.isFinite(player.getX()) || !Double.isFinite(player.getY()) || !Double.isFinite(player.getZ())) {
            player.teleportTo(0d, 256d, 0d);
            LOGGER.info("Player {} had a non-finite position; moved to 0,256,0", player.getName().getString());
        }

        PlayerSyncService.onLogin(player);
    }

    @SubscribeEvent
    public void serverAboutToStart(ServerAboutToStartEvent event) {
        MC_SERVER = event.getServer();
        if (!MC_SERVER.isDedicatedServer()) {
            WarForgeConfig.reloadFromDisk();
        }
        FLAG_REGISTRY.reload();

        try {
            VeinConfigHandler.writeStubIfEmpty();
            VEIN_HANDLER = VeinConfigHandler.loadVeins();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load veins.toml; running without veins", e);
            VEIN_HANDLER = VeinConfigHandler.emptyHandler();
        }

        try {
            readState(SaveManager.load());
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to load warforgefactions.dat", e);
        }
        LOGGER.info("Loaded warforgefactions.dat");
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        CHUNK_LOADING_MANAGER.refreshAllFactions(FACTIONS.registry.getAllFactions());
        FACTIONS.recalculateAllWealth();
        if (WarForgeConfig.DYNMAP_ENABLED && ModList.get().isLoaded("dynmap")) {
            com.flansmod.warforge.compat.dynmap.DynmapBridge.start(FACTIONS);
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandFactions.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void saveEvent(LevelEvent.Save event) {
        // Fires per dimension; state is global => one snapshot per save pass.
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && !inReplayServer()) {
            SaveManager.save("Level Save");
        }
    }

    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent event) {
        com.flansmod.warforge.compat.dynmap.DynmapBridge.stop();
        if (inReplayServer()) {
            CHUNK_LOADING_MANAGER.shutdown();
            MC_SERVER = null;
            return;
        }
        SaveManager.saveAndStop("Server Stop");
        CHUNK_LOADING_MANAGER.shutdown();
        MC_SERVER = null;
    }
}
