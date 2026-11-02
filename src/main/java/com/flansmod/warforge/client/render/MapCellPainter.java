package com.flansmod.warforge.client.render;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.Color4i;
import com.flansmod.warforge.client.util.ColorUtil;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.client.ui.model.ClaimChunkRenderInfo;
import com.flansmod.warforge.client.ui.model.SiegeCampAttackInfoRender;
import com.flansmod.warforge.compat.Ids;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;

public final class MapCellPainter {

    public static final int CB_THICKNESS = 2;
    public static final int HL_THICKNESS = 1;
    public static final int HL_COLOR = 0xFFC6C6C6;
    public static final int GRID_COLOR = new Color4i(0.15f, 26, 26, 26).toARGB();
    public static final int GRID_THICKNESS = HL_THICKNESS;
    public static final int OFFSET = 2;
    public static final int SIZE = 12;

    private static final int MAP_TEX_SIZE = 64;
    private static final float TERRAIN_DIM = 0.9f;
    private static final int PLACEHOLDER_COLOR = 0xFF2A2A2A;
    private static final float OVERLAY_ALPHA = 0.5f;
    private static final float OVERLAY_ALPHA_BOTH = 0.65f;
    private static final int FACTION_TINT_ALPHA = 0x20;
    private static final int EDGE_SHIFT = 16;
    private static final int LABEL_X = 2;
    private static final int LABEL_BOTTOM = 10;
    private static final int FORCE_LOADED_RIGHT = 20;
    private static final String FORCE_LOADED_LABEL = "[F]";

    private static final int ATTACK_ICON_SIZE = 46;
    private static final int ATTACK_ICON_DRAW = 46;
    private static final int SELF_ICON_SIZE = 480;
    private static final int SELF_ICON_DRAW = 48;
    private static final int CENTER_ICON_DRAW = 24;
    private static final int CONQUERED_TEX_SIZE = 64;
    private static final int VEIN_TEX_SIZE = 16;
    private static final int CENTER_CUSTOM_ICON_SIZE = 64;

    private static final ResourceLocation ATTACK_ICON = Ids.of(Tags.MODID, "gui/icon_siege_attack.png");
    private static final ResourceLocation SELF_ICON = Ids.of(Tags.MODID, "gui/icon_siege_self.png");
    private static final ResourceLocation SELF_ICON_BASE = Ids.of(Tags.MODID, "gui/icon_siege_self_base.png");
    private static final ResourceLocation CONQUERED_OVERLAY = Ids.of(Tags.MODID, "gui/conquered.png");

    private static final Object2ObjectOpenHashMap<String, ResourceLocation> TEXTURES = new Object2ObjectOpenHashMap<>();

    private MapCellPainter() {
    }

    public static boolean isCampChunk(SiegeCampAttackInfoRender chunkState) {
        return chunkState.offset.getZ() == 0 && chunkState.offset.getX() == 0;
    }

    public static float[] factionRgb(SiegeCampAttackInfoRender chunkState) {
        return Color4i.fromRGB(chunkState.factionColour).asFloatRGB();
    }

    public static void clearTextureCache() {
        TEXTURES.clear();
    }

    /**
     * @param mapData   the cell's terrain texture path (warforge namespace)
     * @param adjacency north, east, south, west: borders to draw where the neighbour is another faction
     */
    public static void paint(Gui2D graphics, String mapData, SiegeCampAttackInfoRender chunkState, boolean[] adjacency,
                             boolean campChunk, float[] rgb, int x, int y, int width, int height, boolean hovered) {
        ResourceLocation mapTexture = TEXTURES.computeIfAbsent(mapData, MapCellPainter::resolveTexture);
        boolean claimed = !chunkState.factionUuid.equals(FactionIds.NONE);

        graphics.enableBlend();

        if (!hovered || !claimed) {
            graphics.setColor(TERRAIN_DIM, TERRAIN_DIM, TERRAIN_DIM, 1f);
        } else {
            graphics.setColor(rgb[0], rgb[1], rgb[2], 1f);
        }

        if (DynamicTextures.isRegistered(mapTexture)) {
            graphics.blit(mapTexture, x, y, width, height, 0f, 0f, MAP_TEX_SIZE, MAP_TEX_SIZE, MAP_TEX_SIZE, MAP_TEX_SIZE);
        } else {
            graphics.fill(x, y, x + width, y + height, PLACEHOLDER_COLOR);
        }

        graphics.setColor(1f, 1f, 1f, 1f);

        boolean conquered = chunkState.conquered;
        boolean battleZone = chunkState instanceof ClaimChunkRenderInfo claimInfo && claimInfo.battleZone;
        if (conquered || battleZone) {
            float alpha = conquered && battleZone ? OVERLAY_ALPHA_BOTH : OVERLAY_ALPHA;
            graphics.setColor(1f, 1f, 1f, alpha);
            graphics.blit(CONQUERED_OVERLAY, x, y, width, height, 0f, 0f, CONQUERED_TEX_SIZE, CONQUERED_TEX_SIZE, CONQUERED_TEX_SIZE, CONQUERED_TEX_SIZE);
            graphics.setColor(1f, 1f, 1f, 1f);
        }

        if (claimed) {
            int base = chunkState.factionColour & 0x00FFFFFF;
            int lightColor = ColorUtil.opaque(ColorUtil.shift(base, EDGE_SHIFT));
            int darkColor = ColorUtil.opaque(ColorUtil.shift(base, -EDGE_SHIFT));

            graphics.fill(x, y, x + width + 1, y + height + 1, ColorUtil.argb(FACTION_TINT_ALPHA, base));

            if (adjacency[0]) {
                graphics.fill(x, y, x + width + 1, y + CB_THICKNESS, lightColor);
            }
            if (adjacency[3]) {
                graphics.fill(x, y, x + CB_THICKNESS, y + height, lightColor);
            }
            if (adjacency[2]) {
                graphics.fill(x, y + height - CB_THICKNESS, x + width, y + height, darkColor);
            }
            if (adjacency[1]) {
                graphics.fill(x + width - CB_THICKNESS, y, x + width, y + height, darkColor);
            }
        } else {
            graphics.border(x, y, x + width, y + height, GRID_THICKNESS, GRID_COLOR);
        }

        if (hovered) {
            if (chunkState.canAttack && !campChunk) {
                graphics.setColor(rgb[0], rgb[1], rgb[2], 1f);
                graphics.blit(ATTACK_ICON, x + (width - ATTACK_ICON_DRAW) / 2, y + (height - ATTACK_ICON_DRAW) / 2,
                        ATTACK_ICON_DRAW, ATTACK_ICON_DRAW, 0f, 0f, ATTACK_ICON_SIZE, ATTACK_ICON_SIZE, ATTACK_ICON_SIZE, ATTACK_ICON_SIZE);
                graphics.setColor(1f, 1f, 1f, 1f);
            } else {
                graphics.border(x, y, x + width, y + height, HL_THICKNESS, HL_COLOR);
            }
        }

        switch (chunkState.getCenterMarkType()) {
            case SIEGE_CAMP -> {
                int xOffset = x + (width - SELF_ICON_DRAW) / 2;
                int yOffset = y + (height - SELF_ICON_DRAW) / 2;

                graphics.setColor(1f, 1f, 1f, 1f);
                graphics.blit(SELF_ICON_BASE, xOffset, yOffset, SELF_ICON_DRAW, SELF_ICON_DRAW, 0f, 0f, SELF_ICON_SIZE, SELF_ICON_SIZE, SELF_ICON_SIZE, SELF_ICON_SIZE);

                graphics.setColor(rgb[0], rgb[1], rgb[2], 1f);
                graphics.blit(SELF_ICON, xOffset, yOffset, SELF_ICON_DRAW, SELF_ICON_DRAW, 0f, 0f, SELF_ICON_SIZE, SELF_ICON_SIZE, SELF_ICON_SIZE, SELF_ICON_SIZE);
                graphics.setColor(1f, 1f, 1f, 1f);
            }
            case PLAYER_FACE -> {
                if (chunkState.getCenterIcon() == null) {
                    break;
                }
                graphics.setColor(1f, 1f, 1f, 1f);
                graphics.playerFace(chunkState.getCenterIcon(), x + (width - CENTER_ICON_DRAW) / 2, y + (height - CENTER_ICON_DRAW) / 2, CENTER_ICON_DRAW);
            }
            case CUSTOM_TEXTURE -> {
                if (chunkState.getCenterIcon() == null) {
                    break;
                }
                graphics.setColor(1f, 1f, 1f, 1f);
                graphics.blit(chunkState.getCenterIcon(), x + (width - CENTER_ICON_DRAW) / 2, y + (height - CENTER_ICON_DRAW) / 2,
                        CENTER_ICON_DRAW, CENTER_ICON_DRAW, 0f, 0f, CENTER_CUSTOM_ICON_SIZE, CENTER_CUSTOM_ICON_SIZE, CENTER_CUSTOM_ICON_SIZE, CENTER_CUSTOM_ICON_SIZE);
            }
            case NONE -> {
            }
        }

        if (chunkState.veinIcon != null) {
            graphics.setColor(1f, 1f, 1f, 1f);
            graphics.blit(chunkState.veinIcon, x + OFFSET, y + OFFSET, SIZE, SIZE, 0f, 0f, VEIN_TEX_SIZE, VEIN_TEX_SIZE, VEIN_TEX_SIZE, VEIN_TEX_SIZE);
        }

        if (chunkState instanceof ClaimChunkRenderInfo claimInfo && claimInfo.claimType != ClaimType.NONE) {
            Font font = Minecraft.getInstance().font;
            String label = claimInfo.claimType.shortLabel;
            if (!label.isEmpty()) {
                graphics.text(font, label, x + LABEL_X, y + height - LABEL_BOTTOM, 0xFFFFFFFF, true);
            }
            if (claimInfo.forceLoaded) {
                graphics.text(font, FORCE_LOADED_LABEL, x + width - FORCE_LOADED_RIGHT, y + height - LABEL_BOTTOM, 0xFFFFFFFF, true);
            }
        }

        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private static ResourceLocation resolveTexture(String mapData) {
        return Ids.of(Tags.MODID, mapData);
    }
}
