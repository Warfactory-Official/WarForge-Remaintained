package com.flansmod.warforge.client.journeymap;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.Color4i;
import com.flansmod.warforge.client.MinimapClaimCache;
import com.flansmod.warforge.common.util.DimChunkPos;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.model.TextProperties;
import journeymap.api.v2.client.util.PolygonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * JourneyMap integration drawing a faction-coloured fill over each claimed chunk.
 * <p>
 * Consumes {@link MinimapClaimCache} only (packed colour + chunk position); faction identity is never
 * available here, matching the deliberately minimal claim payload.
 */
@ParametersAreNonnullByDefault
public class ClaimMapLayer implements MinimapClaimCache.Listener {
    private static final Logger LOGGER = LogManager.getLogger(Tags.MODNAME);
    private static final int CLAIM_OVERLAY_Y = 70;

    private final Map<DimChunkPos, PolygonOverlay> overlays = new HashMap<>();
    private IClientAPI jmAPI;

    void attach(final IClientAPI jmClientApi) {
        this.jmAPI = jmClientApi;
        // Register last so the cache replay (inside setListener) sees a live API reference.
        MinimapClaimCache.addListener(this);
    }

    private String getModId() {
        return Tags.MODID;
    }

    @Override
    public void onClaimSet(ResourceKey<Level> dim, int x, int z, int colour) {
        Minecraft.getInstance().execute(() -> {
            DimChunkPos key = new DimChunkPos(dim, x, z);
            PolygonOverlay existing = overlays.remove(key);
            if (existing != null) {
                jmAPI.remove(existing);
            }
            PolygonOverlay overlay = buildOverlay(key, colour);
            try {
                jmAPI.show(overlay);
                overlays.put(key, overlay);
            } catch (Exception e) {
                LOGGER.error("Failed to show claim overlay for {}", key, e);
            }
        });
    }

    @Override
    public void onClaimRemoved(ResourceKey<Level> dim, int x, int z) {
        Minecraft.getInstance().execute(() -> {
            PolygonOverlay overlay = overlays.remove(new DimChunkPos(dim, x, z));
            if (overlay != null) {
                jmAPI.remove(overlay);
            }
        });
    }

    @Override
    public void onCleared() {
        Minecraft.getInstance().execute(() -> {
            overlays.clear();
            jmAPI.removeAll(getModId(), DisplayType.Polygon);
        });
    }

    private PolygonOverlay buildOverlay(DimChunkPos key, int colour) {
        Color4i color = new Color4i(colour);
        int rgb = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        float fillOpacity = (color.getAlpha() & 0xFF) / 255.0f;

        ShapeProperties shapeProps = new ShapeProperties()
                .setStrokeWidth(2)
                .setStrokeColor(rgb).setStrokeOpacity(Math.max(fillOpacity, 0.7f))
                .setFillColor(rgb).setFillOpacity(Math.min(fillOpacity, 0.4f));

        TextProperties textProps = new TextProperties()
                .setColor(rgb)
                .setOpacity(1f)
                .setMinZoom(2)
                .setFontShadow(true);

        MapPolygon polygon = PolygonHelper.createChunkPolygon(key.x, CLAIM_OVERLAY_Y, key.z);

        PolygonOverlay overlay = new PolygonOverlay(getModId(), key.dim, shapeProps, polygon);
        overlay.setOverlayGroupName("WarForge Claims")
                .setLabel(String.format("Claim [%s, %s]", key.x, key.z))
                .setTextProperties(textProps);
        return overlay;
    }
}
