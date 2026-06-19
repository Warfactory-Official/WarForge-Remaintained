package com.flansmod.warforge.client.journeymap;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.JourneyMapClaimCache;
import com.flansmod.warforge.common.util.DimChunkPos;
import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.util.PolygonHelper;
import net.minecraft.client.Minecraft;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Optional JourneyMap integration: draws a faction-coloured fill over each claimed chunk.
 * <p>
 * This class is only ever loaded by JourneyMap's {@code @ClientPlugin} discovery, so when JourneyMap
 * is absent it is never referenced and nothing here runs — keeping the dependency soft. It only
 * consumes {@link JourneyMapClaimCache} (colour + position); faction identity is never available here.
 */
@ClientPlugin
@SuppressWarnings("unused")
public class WarForgeJourneyMapPlugin implements IClientPlugin, JourneyMapClaimCache.Listener {
    private IClientAPI api;
    private final Map<DimChunkPos, PolygonOverlay> overlays = new HashMap<>();

    @Override
    public void initialize(IClientAPI jmAPI) {
        this.api = jmAPI;
        jmAPI.subscribe(getModId(), EnumSet.of(ClientEvent.Type.MAPPING_STARTED));
        // Registering replays any claims already received so we catch up on late init.
        JourneyMapClaimCache.setListener(this);
    }

    @Override
    public String getModId() {
        return Tags.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        if (event.type == ClientEvent.Type.MAPPING_STARTED) {
            runOnClient(() -> {
                for (Map.Entry<DimChunkPos, PolygonOverlay> entry : overlays.entrySet()) {
                    if (entry.getKey().dim == event.dimension) {
                        showSafely(entry.getValue());
                    }
                }
            });
        }
    }

    @Override
    public void onClaimSet(int dim, int x, int z, int colour) {
        runOnClient(() -> {
            DimChunkPos key = new DimChunkPos(dim, x, z);
            PolygonOverlay existing = overlays.remove(key);
            if (existing != null) {
                api.remove(existing);
            }
            PolygonOverlay overlay = buildOverlay(dim, x, z, colour);
            overlays.put(key, overlay);
            showSafely(overlay);
        });
    }

    @Override
    public void onClaimRemoved(int dim, int x, int z) {
        runOnClient(() -> {
            PolygonOverlay overlay = overlays.remove(new DimChunkPos(dim, x, z));
            if (overlay != null) {
                api.remove(overlay);
            }
        });
    }

    @Override
    public void onCleared() {
        runOnClient(() -> {
            api.removeAll(getModId());
            overlays.clear();
        });
    }

    private void showSafely(PolygonOverlay overlay) {
        try {
            if (!api.exists(overlay)) {
                api.show(overlay);
            }
        } catch (Exception e) {
            // JourneyMap refuses overlays in some states; it will be re-shown on the next MAPPING_STARTED.
        }
    }

    private PolygonOverlay buildOverlay(int dim, int x, int z, int colour) {
        ShapeProperties shape = new ShapeProperties()
                .setStrokeColor(colour)
                .setStrokeWidth(3.0f)
                .setStrokeOpacity(1.0f)
                .setFillColor(colour)
                .setFillOpacity(0.4f);

        String id = "warforge_claim_" + dim + "_" + x + "_" + z;
        PolygonOverlay overlay = new PolygonOverlay(getModId(), id, dim, shape, PolygonHelper.createChunkPolygon(x, 0, z));
        overlay.setOverlayGroupName("WarForge Claims");
        return overlay;
    }

    private static void runOnClient(Runnable task) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            mc.addScheduledTask(task);
        }
    }
}
