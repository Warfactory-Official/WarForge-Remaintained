package com.flansmod.warforge.client.render;

import com.flansmod.warforge.client.RenderTileEntityClaim;
import com.flansmod.warforge.client.RenderTileEntityDummy;
import com.flansmod.warforge.client.RenderTileEntityFob;
import com.flansmod.warforge.client.TileEntityLeaderboardRenderer;
import com.flansmod.warforge.common.Content;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registers the block entity renderers (their render API differs between versions). */
public final class BlockEntityRenderers {
    private BlockEntityRenderers() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(BlockEntityRenderers::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Content.TE_CITADEL.get(), ctx -> new RenderTileEntityClaim());
        event.registerBlockEntityRenderer(Content.TE_BASIC_CLAIM.get(), ctx -> new RenderTileEntityClaim());
        event.registerBlockEntityRenderer(Content.TE_SIEGE_CAMP.get(), ctx -> new RenderTileEntityClaim());
        event.registerBlockEntityRenderer(Content.TE_FOB.get(), ctx -> new RenderTileEntityFob());
        event.registerBlockEntityRenderer(Content.TE_DUMMY.get(), RenderTileEntityDummy::new);
        event.registerBlockEntityRenderer(Content.TE_LEADERBOARD.get(), TileEntityLeaderboardRenderer::new);
    }
}
