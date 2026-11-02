package com.flansmod.warforge.client;

import com.flansmod.warforge.client.border.ClaimBorderRenderer;
import com.flansmod.warforge.client.border.PlacementOverlayRenderer;
import com.flansmod.warforge.client.hud.AreaToastHud;
import com.flansmod.warforge.client.hud.SiegeHud;
import com.flansmod.warforge.client.hud.TimersHud;
import com.flansmod.warforge.client.hud.VeinHud;
import com.flansmod.warforge.client.map.ChunkMapTextureDaemon;
import com.flansmod.warforge.client.render.BorderGpu;
import com.flansmod.warforge.client.render.Gui2D;
import com.flansmod.warforge.client.render.WorldRenderContext;
import com.flansmod.warforge.client.util.ScreenSpaceUtil;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.compat.Chunks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

public class ClientTickHandler {
    private ResourceKey<Level> standingDim;
    private int standingChunkX;
    private int standingChunkZ;

    @SubscribeEvent
    public void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        standingDim = null;
        ClientSession.reset();
    }

    @SubscribeEvent
    public void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        standingDim = null;
        ClientSession.reset();
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        DeferredGuiOpen.onScreenOpened();
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof Level level) || !level.isClientSide()) {
            return;
        }
        ChunkPos chunk = event.getChunk().getPos();
        ClaimBorderRenderer.markTerrainChanged(level.dimension(), Chunks.x(chunk), Chunks.z(chunk));
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            return;
        }

        DeferredGuiOpen.tick();
        ChunkMapTextureDaemon.flushTextureQueue();
        WarForgeMod.proxy.tickClient();
        SiegeUiDebugCommand.tick();
        ClientSiegeState.tick();
        AreaToastHud.tick();

        ResourceKey<Level> dim = player.level().dimension();
        BlockPos feet = player.blockPosition();
        int chunkX = feet.getX() >> 4;
        int chunkZ = feet.getZ() >> 4;
        if (!dim.equals(standingDim) || chunkX != standingChunkX || chunkZ != standingChunkZ) {
            if (standingDim != null) {
                AreaToastHud.onChunkChanged(standingDim, standingChunkX, standingChunkZ, dim, chunkX, chunkZ);
            }
            VeinHud.onChunkChanged(dim, chunkX, chunkZ);
            standingDim = dim;
            standingChunkX = chunkX;
            standingChunkZ = chunkZ;
        }

        BorderSyncScheduler.tick(mc, player, dim, chunkX, chunkZ, ClientCompat.screen() != null);
        ClaimBorderRenderer.tick(mc, chunkX, chunkZ);
        ClientKeybinds.tick(player, dim, chunkX, chunkZ);
    }

    public void onRenderHUD(Gui2D graphics, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        ScreenSpaceUtil.resetOffsets();

        SiegeHud.render(mc, graphics);
        if (ClientConfig.SHOW_YIELD_TIMERS) {
            TimersHud.render(mc, graphics);
        }
        AreaToastHud.render(mc, graphics);
        VeinHud.render(mc, graphics);
    }

    public void onRenderLast(WorldRenderContext event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level world = mc.level;
        if (player == null || world == null) {
            return;
        }

        // The stage's pose is relative to the camera eye; subtracting the entity's feet would shift geometry by eye height.
        Vec3 cam = ClientCompat.cameraPosition(ClientCompat.mainCamera());

        BorderGpu.beginFrame();
        ClaimBorderRenderer.render(mc, world, event.viewMatrix(), cam.x, cam.y, cam.z);
        PlacementOverlayRenderer.render(mc, player, event.viewMatrix(), cam.x, cam.y, cam.z);
        BorderGpu.endFrame();
    }
}
