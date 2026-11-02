package com.flansmod.warforge.client.journeymap;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.ClaimMapStyle;
import com.flansmod.warforge.client.ClientBorderCache;
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

@ParametersAreNonnullByDefault
public class ClaimMapLayer implements ClientBorderCache.Listener {
    private static final Logger LOGGER = LogManager.getLogger(Tags.MODNAME);
    private static final int CLAIM_OVERLAY_Y = 70;
    private static final int STROKE_WIDTH = 2;
    private static final int LABEL_MIN_ZOOM = 2;

    private final Map<DimChunkPos, PolygonOverlay> overlays = new HashMap<>();
    private IClientAPI jmAPI;

    void attach(final IClientAPI jmClientApi) {
        this.jmAPI = jmClientApi;
        // Last: the cache replay inside addListener needs jmAPI.
        ClientBorderCache.addListener(this);
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
        int rgb = colour & 0xFFFFFF;

        ShapeProperties shapeProps = new ShapeProperties()
                .setStrokeWidth(STROKE_WIDTH)
                .setStrokeColor(rgb).setStrokeOpacity(ClaimMapStyle.borderOpacity(colour))
                .setFillColor(rgb).setFillOpacity(ClaimMapStyle.fillOpacity(colour));

        TextProperties textProps = new TextProperties()
                .setColor(rgb)
                .setOpacity(1f)
                .setMinZoom(LABEL_MIN_ZOOM)
                .setFontShadow(true);

        MapPolygon polygon = PolygonHelper.createChunkPolygon(key.x, CLAIM_OVERLAY_Y, key.z);

        PolygonOverlay overlay = new PolygonOverlay(getModId(), key.dim, shapeProps, polygon);
        overlay.setOverlayGroupName("WarForge Claims")
                .setLabel(ClaimMapStyle.label(key.x, key.z).getString())
                .setTextProperties(textProps);
        return overlay;
    }
}
