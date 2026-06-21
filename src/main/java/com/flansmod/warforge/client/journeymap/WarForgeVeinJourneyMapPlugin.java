package com.flansmod.warforge.client.journeymap;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.init.VeinUtils;
import com.flansmod.warforge.client.JourneyMapVeinCache;
import com.flansmod.warforge.client.util.LayeredItemIconCache;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.ItemMatcher;
import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.DisplayType;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * JourneyMap integration drawing each chunk's ore vein as a small item icon at the chunk centre, matching the
 * icon the claim GUI shows. Reads {@link JourneyMapVeinCache} (the compressed vein short) and resolves the icon
 * from the already-synced vein definitions.
 */
@ParametersAreNonnullByDefault
@ClientPlugin
public class WarForgeVeinJourneyMapPlugin implements IClientPlugin, JourneyMapVeinCache.Listener {
    private static final Logger LOGGER = LogManager.getLogger(Tags.MODNAME);
    private static final int VEIN_MARKER_Y = 70;
    private static final int ICON_SIZE = 16;

    private final Map<DimChunkPos, MarkerOverlay> markers = new HashMap<>();
    private IClientAPI jmAPI;

    @Override
    public void initialize(final IClientAPI jmClientApi) {
        this.jmAPI = jmClientApi;
        jmClientApi.subscribe(getModId(), EnumSet.noneOf(ClientEvent.Type.class));
        // Register last so the cache replay (inside setListener) sees a live API reference.
        JourneyMapVeinCache.setListener(this);
    }

    @Override
    public String getModId() {
        return Tags.MODID;
    }

    @Override
    public void onEvent(final ClientEvent event) {
    }

    @Override
    public void onVeinSet(ResourceKey<Level> dim, int x, int z, short veinInfo) {
        Minecraft.getInstance().execute(() -> {
            DimChunkPos key = new DimChunkPos(dim, x, z);
            MarkerOverlay existing = markers.remove(key);
            if (existing != null) {
                jmAPI.remove(existing);
            }
            ItemStack icon = iconStackFor(veinInfo);
            if (icon == null) {
                return;
            }
            MarkerOverlay overlay = buildOverlay(key, icon);
            if (overlay == null) {
                return;
            }
            try {
                jmAPI.show(overlay);
                markers.put(key, overlay);
            } catch (Exception e) {
                LOGGER.error("Failed to show vein overlay for {}", key, e);
            }
        });
    }

    @Override
    public void onVeinRemoved(ResourceKey<Level> dim, int x, int z) {
        Minecraft.getInstance().execute(() -> {
            MarkerOverlay overlay = markers.remove(new DimChunkPos(dim, x, z));
            if (overlay != null) {
                jmAPI.remove(overlay);
            }
        });
    }

    @Override
    public void onCleared() {
        Minecraft.getInstance().execute(() -> {
            markers.clear();
            jmAPI.removeAll(getModId(), DisplayType.Marker);
        });
    }

    private MarkerOverlay buildOverlay(DimChunkPos key, ItemStack icon) {
        ResourceLocation texture = LayeredItemIconCache.getIcon(icon);
        if (texture == null) {
            return null;
        }
        MapImage image = new MapImage(texture, ICON_SIZE, ICON_SIZE);
        image.centerAnchors();

        BlockPos center = new BlockPos((key.x << 4) + 8, VEIN_MARKER_Y, (key.z << 4) + 8);
        String displayId = "vein_" + key.dim.location() + "_" + key.x + "_" + key.z;

        MarkerOverlay overlay = new MarkerOverlay(getModId(), displayId, center, image);
        overlay.setDimension(key.dim)
                .setOverlayGroupName("WarForge Veins")
                .setLabel(icon.getHoverName().getString())
                .setTitle(icon.getHoverName().getString());
        return overlay;
    }

    /** Resolve the item icon a vein should display, or {@code null} if there is none. */
    private ItemStack iconStackFor(short veinInfo) {
        Pair<Vein, Quality> info = VeinUtils.decompressVeinInfo(veinInfo);
        if (info == null || info.getLeft() == null) {
            return null;
        }
        Vein vein = info.getLeft();
        if (vein.compIds == null || vein.compIds.isEmpty()) {
            return null;
        }
        ItemMatcher component = vein.compIds.iterator().next();
        ItemStack stack = component.toStack();
        if (stack.isEmpty()) {
            return null;
        }
        return stack;
    }
}
