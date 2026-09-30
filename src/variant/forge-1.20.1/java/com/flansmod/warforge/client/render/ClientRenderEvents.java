package com.flansmod.warforge.client.render;

import com.flansmod.warforge.client.PlayerNametagCache;
import com.flansmod.warforge.client.util.FullColorNameplate;
import com.flansmod.warforge.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Client render hooks whose loader events differ between versions. */
public final class ClientRenderEvents {
    private ClientRenderEvents() {}

    /** Registers the render resources that need the mod bus (client only). This version needs none. */
    public static void init(IEventBus modBus) {
    }

    /**
     * Runs after translucent blocks are drawn in the world (client only). On 1.20.1 the stage's pose stack already
     * holds the camera rotation, so it is also the view matrix.
     */
    public static void onAfterTranslucentBlocks(Consumer<WorldRenderContext> handler) {
        Platform.gameBus().addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                return;
            }
            handler.accept(new WorldRenderContext(event.getPartialTick(), event.getPoseStack(),
                    new Matrix4f(event.getPoseStack().last().pose())));
        });
    }

    /** Runs after the vanilla HUD is drawn (client only). The float is the partial tick. */
    public static void onHud(BiConsumer<Gui2D, Float> handler) {
        Platform.gameBus().addListener((RenderGuiEvent.Post event) ->
                handler.accept(Gui2D.of(event.getGuiGraphics()), event.getPartialTick()));
    }

    /**
     * Draws a faction plate above name tags (client only). plateFor(entity, name tag text) returns the plate to
     * draw, or null for none.
     */
    public static void onPlayerNameTag(BiFunction<Entity, String, PlayerNametagCache.NamePlateData> plateFor) {
        Platform.gameBus().addListener((RenderNameTagEvent event) -> {
            PlayerNametagCache.NamePlateData faction = plateFor.apply(event.getEntity(), event.getContent().getString());
            if (faction == null) {
                return;
            }
            FullColorNameplate.drawNameplate(Minecraft.getInstance().font, Component.literal(faction.name), event.getEntity(),
                    event.getPoseStack(), event.getMultiBufferSource(), -10, event.getEntity().isDiscrete(), faction.color,
                    faction.darkerColor, event.getPackedLight());
        });
    }
}
