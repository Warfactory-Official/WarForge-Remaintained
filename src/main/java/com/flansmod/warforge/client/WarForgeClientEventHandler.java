package com.flansmod.warforge.client;

import com.flansmod.warforge.client.ui.model.ClaimMapModel;
import com.flansmod.warforge.client.ui.UiNavigation;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionMemberManagerGuiData;
import com.flansmod.warforge.common.network.PacketMoveCitadel;
import com.flansmod.warforge.common.network.PacketRequestFactionInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import static com.flansmod.warforge.client.WarforgeIconButton.WARFORGE_BUTTON_SIZE;

public final class WarForgeClientEventHandler {
    private static final int BUTTON_TOP = 4;
    private static final int BUTTON_PITCH = 22;
    private static final double MOVE_CITADEL_REACH = 10.0d;
    private static final char FORMATTING_PREFIX = '\u00a7';

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof CreativeModeInventoryScreen)) {
            return;
        }
        if (Minecraft.getInstance().player == null) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        int x = screen.getGuiLeft() + screen.getXSize() + 4;
        int top = screen.getGuiTop();
        event.addListener(new WarforgeIconButton(x, top + BUTTON_TOP, 0, "warforge.client.button.territory_map", WarForgeClientEventHandler::openClaims));
        event.addListener(new WarforgeIconButton(x, top + BUTTON_TOP + BUTTON_PITCH, WARFORGE_BUTTON_SIZE, "warforge.client.button.faction_members", WarForgeClientEventHandler::openMembers));
        event.addListener(new WarforgeIconButton(x, top + BUTTON_TOP + BUTTON_PITCH * 2, WARFORGE_BUTTON_SIZE * 2, "warforge.client.button.faction_stats", WarForgeClientEventHandler::openFactionStats));
        event.addListener(new WarforgeIconButton(x, top + BUTTON_TOP + BUTTON_PITCH * 3, WARFORGE_BUTTON_SIZE * 3, "warforge.client.button.move_citadel", WarForgeClientEventHandler::moveCitadel));
        event.addListener(new WarforgeIconButton(x, top + BUTTON_TOP + BUTTON_PITCH * 4, WARFORGE_BUTTON_SIZE * 4, "warforge.client.button.operations", WarForgeClientEventHandler::openOperations));
    }

    private static void openClaims() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        DimChunkPos center = new DimChunkPos(player.level().dimension(), player.blockPosition());
        ClaimMapModel.resetSiegeState();
        UiNavigation.openClaimManager(center, WarForgeConfig.CLAIM_MANAGER_RADIUS, -1, -1);
    }

    private static void openMembers() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        UiNavigation.requestMembers(FactionMemberManagerGuiData.Page.MEMBERS);
    }

    private static void openFactionStats() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        PacketRequestFactionInfo packet = new PacketRequestFactionInfo();
        packet.factionIdRequest = ClientClaimChunkCache.playerFactionId();
        WarForgeMod.NETWORK.sendToServer(packet);
    }

    private static void openOperations() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        UiNavigation.requestOperations();
    }

    private static void moveCitadel() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        HitResult hit = player.pick(MOVE_CITADEL_REACH, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            PacketMoveCitadel packet = new PacketMoveCitadel();
            packet.pos = new DimBlockPos(player.level().dimension(), blockHit.getBlockPos().relative(blockHit.getDirection()));
            WarForgeMod.NETWORK.sendToServer(packet);
        }
    }

    public static PlayerNametagCache.NamePlateData nameplateFor(Entity entity, String content) {
        if (!(entity instanceof Player) || entity == Minecraft.getInstance().player) {
            return null;
        }
        // Player names can carry formatting codes.
        String name = content.indexOf(FORMATTING_PREFIX) < 0 ? content : ChatFormatting.stripFormatting(content);
        return PlayerNametagCache.INSTANCE.requestIfAbsent(name);
    }
}
