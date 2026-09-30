package com.flansmod.warforge.client.render;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.PlayerNametagCache;
import com.flansmod.warforge.client.util.FullColorNameplate;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Client render hooks whose loader events differ between versions. */
public final class ClientRenderEvents {
    /** The faction plate of an entity, attached to its render state during extraction. */
    private static final ContextKey<FullColorNameplate.Plate> PLATE = new ContextKey<>(Ids.of(Tags.MODID, "faction_plate"));

    private ClientRenderEvents() {}

    /** Registers the render resources that need the mod bus (client only). */
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterRenderPipelinesEvent event) -> event.registerPipeline(BorderGpu.PIPELINE));
    }

    /** Runs after translucent blocks are drawn in the world (client only). */
    public static void onAfterTranslucentBlocks(Consumer<WorldRenderContext> handler) {
        Platform.gameBus().addListener((RenderLevelStageEvent.AfterTranslucentBlocks event) -> {
            BorderGpu.setFrameView(event.getModelViewMatrix());
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            handler.accept(new WorldRenderContext(partialTick, event.getPoseStack(), new Matrix4f(event.getModelViewMatrix())));
        });
    }

    /** Runs after the vanilla HUD is drawn (client only). The float is the partial tick. */
    public static void onHud(BiConsumer<Gui2D, Float> handler) {
        Platform.gameBus().addListener((RenderGuiEvent.Post event) ->
                handler.accept(Gui2D.of(event.getGuiGraphics()), event.getPartialTick().getGameTimeDeltaPartialTick(false)));
    }

    /**
     * Draws a faction plate above name tags (client only). plateFor(entity, name tag text) returns the plate to
     * draw, or null for none.
     *
     * <p>Before 26.x the plate was drawn in the name tag event, which fired for every rendered entity, also when
     * vanilla hid the name. 26.x only fires the draw event for names that show, so the plate is recorded on the
     * render state during extraction and drawn for every extracted entity from the custom geometry event.
     */
    public static void onPlayerNameTag(BiFunction<Entity, String, PlayerNametagCache.NamePlateData> plateFor) {
        Platform.gameBus().addListener((RenderNameTagEvent.CanRender event) -> {
            Component content = event.getContent();
            PlayerNametagCache.NamePlateData faction = content == null ? null : plateFor.apply(event.getEntity(), content.getString());
            Entity entity = event.getEntity();
            event.getEntityRenderState().setRenderData(PLATE, faction == null ? null
                    : new FullColorNameplate.Plate(faction, entity.getBbHeight(), entity.isDiscrete(), event.getEntityRenderer()));
        });
        Platform.gameBus().addListener((SubmitCustomGeometryEvent event) -> {
            var level = event.getLevelRenderState();
            for (EntityRenderState state : level.entityRenderStates) {
                FullColorNameplate.Plate plate = state.getRenderData(PLATE);
                if (plate != null) {
                    FullColorNameplate.submit(plate, state, level.cameraRenderState, event.getPoseStack(), event.getSubmitNodeCollector());
                }
            }
        });
    }
}
