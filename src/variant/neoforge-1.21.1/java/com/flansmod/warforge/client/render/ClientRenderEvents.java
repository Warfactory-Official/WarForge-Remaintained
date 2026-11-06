package com.flansmod.warforge.client.render;

import com.flansmod.warforge.client.PlayerNametagCache;
import com.flansmod.warforge.client.util.FullColorNameplate;
import com.flansmod.warforge.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class ClientRenderEvents {
    private static final WorldRenderContext WORLD = new WorldRenderContext();

    private ClientRenderEvents() {}

    public static void init(IEventBus modBus) {
    }

    public static void onAfterTranslucentBlocks(Consumer<WorldRenderContext> handler) {
        Platform.gameBus().addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                return;
            }
            handler.accept(WORLD.update(event.getPartialTick().getGameTimeDeltaPartialTick(false),
                    event.getPoseStack(), event.getModelViewMatrix()));
        });
    }

    public static void onHud(HudHandler handler) {
        Platform.gameBus().addListener((RenderGuiEvent.Post event) -> {
            if (FlashbackReplay.hidesHud()) {
                return;
            }
            handler.draw(Gui2D.of(event.getGuiGraphics()), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        });
    }

    public static void onPlayerNameTag(BiFunction<Entity, String, PlayerNametagCache.NamePlateData> plateFor) {
        Platform.gameBus().addListener((RenderNameTagEvent event) -> {
            PlayerNametagCache.NamePlateData faction = plateFor.apply(event.getEntity(), event.getContent().getString());
            if (faction == null) {
                return;
            }
            FullColorNameplate.drawNameplate(Minecraft.getInstance().font, FullColorNameplate.nameComponent(faction.name), event.getEntity(),
                    event.getPoseStack(), event.getMultiBufferSource(), -10, event.getEntity().isDiscrete(), faction.color,
                    faction.darkerColor, event.getPackedLight());
        });
    }
}
