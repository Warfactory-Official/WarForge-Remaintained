package com.flansmod.warforge.client.render;

import com.flansmod.warforge.api.client.NameplatePolicy;
import com.flansmod.warforge.api.client.WarforgeClientApi;
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

import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class ClientRenderEvents {
    private static final ContextKey<FullColorNameplate.Plate> PLATE = new ContextKey<>(Ids.of(Tags.MODID, "faction_plate"));

    private static final WorldRenderContext WORLD = new WorldRenderContext();

    private ClientRenderEvents() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterRenderPipelinesEvent event) -> event.registerPipeline(BorderBackend.PIPELINE));
    }

    public static void onAfterTranslucentBlocks(Consumer<WorldRenderContext> handler) {
        Platform.gameBus().addListener((RenderLevelStageEvent.AfterTranslucentBlocks event) -> {
            BorderGpu.setFrameView(event.getModelViewMatrix());
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            handler.accept(WORLD.update(partialTick, event.getPoseStack(), event.getModelViewMatrix()));
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

    /** CanRender fires only for names that show: plate recorded at extraction, drawn per extracted entity in SubmitCustomGeometryEvent. */
    public static void onPlayerNameTag(BiFunction<Entity, String, PlayerNametagCache.NamePlateData> plateFor) {
        Platform.gameBus().addListener((RenderNameTagEvent.CanRender event) -> {
            Component content = event.getContent();
            PlayerNametagCache.NamePlateData faction = content == null ? null : plateFor.apply(event.getEntity(), content.getString());
            Entity entity = event.getEntity();
            NameplatePolicy.Mode mode = faction == null ? null : WarforgeClientApi.nameplateMode(entity);
            event.getEntityRenderState().setRenderData(PLATE, faction == null || mode == NameplatePolicy.Mode.HIDDEN ? null
                    : new FullColorNameplate.Plate(faction, entity.getBbHeight(), entity.isDiscrete(), event.getEntityRenderer(),
                    mode == NameplatePolicy.Mode.SEE_THROUGH));
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
