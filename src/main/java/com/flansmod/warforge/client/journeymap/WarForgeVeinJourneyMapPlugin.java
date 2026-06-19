package com.flansmod.warforge.client.journeymap;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.init.VeinUtils;
import com.flansmod.warforge.client.JourneyMapVeinCache;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.StackComparable;
import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Optional JourneyMap integration: draws each chunk's ore vein as a small item icon in the chunk's corner,
 * matching the icon the claim GUI shows. Only loaded by JourneyMap's {@code @ClientPlugin} discovery, so the
 * dependency stays soft. It reads {@link JourneyMapVeinCache} (the compressed vein short) and resolves the
 * icon from the already-synced vein definitions.
 */
@ClientPlugin
public class WarForgeVeinJourneyMapPlugin implements IClientPlugin, JourneyMapVeinCache.Listener {
    private static final int ICON_SIZE = 12;

    private IClientAPI api;
    private final Map<DimChunkPos, MarkerOverlay> markers = new HashMap<>();

    @Override
    public void initialize(IClientAPI jmAPI) {
        this.api = jmAPI;
        jmAPI.subscribe(getModId(), EnumSet.of(ClientEvent.Type.MAPPING_STARTED));
        JourneyMapVeinCache.setListener(this);
    }

    @Override
    public String getModId() {
        return Tags.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        if (event.type == ClientEvent.Type.MAPPING_STARTED) {
            runOnClient(() -> {
                for (Map.Entry<DimChunkPos, MarkerOverlay> entry : markers.entrySet()) {
                    if (entry.getKey().dim == event.dimension) {
                        showSafely(entry.getValue());
                    }
                }
            });
        }
    }

    @Override
    public void onVeinSet(int dim, int x, int z, short veinInfo) {
        runOnClient(() -> {
            DimChunkPos key = new DimChunkPos(dim, x, z);
            MarkerOverlay existing = markers.remove(key);
            if (existing != null) {
                api.remove(existing);
            }
            MarkerOverlay marker = buildMarker(dim, x, z, veinInfo);
            if (marker != null) {
                markers.put(key, marker);
                showSafely(marker);
            }
        });
    }

    @Override
    public void onVeinRemoved(int dim, int x, int z) {
        runOnClient(() -> {
            MarkerOverlay marker = markers.remove(new DimChunkPos(dim, x, z));
            if (marker != null) {
                api.remove(marker);
            }
        });
    }

    @Override
    public void onCleared() {
        runOnClient(() -> {
            api.removeAll(getModId());
            markers.clear();
        });
    }

    private void showSafely(MarkerOverlay marker) {
        try {
            if (!api.exists(marker)) {
                api.show(marker);
            }
        } catch (Exception e) {
            // JourneyMap refuses overlays in some states; it will be re-shown on the next MAPPING_STARTED.
        }
    }

    private MarkerOverlay buildMarker(int dim, int x, int z, short veinInfo) {
        Pair<Vein, Quality> info = VeinUtils.decompressVeinInfo(veinInfo);
        if (info == null || info.getLeft() == null) {
            return null;
        }
        Vein vein = info.getLeft();
        if (vein.compIds == null || vein.compIds.isEmpty()) {
            return null;
        }
        StackComparable component = vein.compIds.iterator().next();
        ItemStack stack = component.toItem();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        MapImage icon = iconFor(stack);
        if (icon == null) {
            return null;
        }
        // Anchor the icon to its top-left and place it at the chunk's NW corner so it sits in the tile's corner.
        icon.setDisplayWidth(ICON_SIZE).setDisplayHeight(ICON_SIZE).setAnchorX(-ICON_SIZE / 2.0).setAnchorY(-ICON_SIZE / 2.0);
        String id = "warforge_vein_" + dim + "_" + x + "_" + z;
        MarkerOverlay marker = new MarkerOverlay(getModId(), id, new BlockPos((x << 4) + 1, 64, (z << 4) + 1), icon);
        marker.setDimension(dim);
        return marker;
    }

    private MapImage iconFor(ItemStack stack) {
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getRenderItem()
                .getItemModelMesher().getItemModel(stack).getParticleTexture();
        if (sprite == null) {
            return null;
        }
        String iconName = sprite.getIconName();
        if (iconName == null || iconName.isEmpty()) {
            return null;
        }
        int colon = iconName.indexOf(':');
        String domain = colon >= 0 ? iconName.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? iconName.substring(colon + 1) : iconName;
        ResourceLocation texture = new ResourceLocation(domain, "textures/" + path + ".png");
        return new MapImage(texture, 16, 16);
    }

    private static void runOnClient(Runnable task) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            mc.addScheduledTask(task);
        }
    }
}
