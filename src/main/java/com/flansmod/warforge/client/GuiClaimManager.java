package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.flansmod.warforge.api.modularui.ChunkMapUtil;
import com.flansmod.warforge.api.modularui.ChunkMapViewport;
import com.flansmod.warforge.api.modularui.MapDrawable;
import com.flansmod.warforge.client.util.SkinUtil;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.ClaimManagerGuiData;
import com.flansmod.warforge.common.factories.ClaimManagerGuiFactory;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.ClaimChunkRenderInfo;
import com.flansmod.warforge.common.network.PacketClaimChunkAction;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.StackComparable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.translation.I18n;

import java.util.Objects;

public final class GuiClaimManager {
    private static final int CELL_SIZE = 64;
    private static final int MAX_VISIBLE_RADIUS = 5;
    private static final int PADDING = 6;
    private static final int HEADER = 22;
    private static final int SIDE_GUTTER = 18;

    private GuiClaimManager() {
    }

    public static ModularPanel buildPanel(ClaimManagerGuiData data) {
        int size = data.radius * 2 + 1;
        ScaledResolution scaled = new ScaledResolution(Minecraft.getMinecraft());
        ChunkMapViewport viewport = ChunkMapViewport.create(
                size,
                3,
                Math.min(size, MAX_VISIBLE_RADIUS * 2 + 1),
                CELL_SIZE,
                scaled.getScaledWidth(),
                scaled.getScaledHeight(),
                PADDING * 2 + 20,
                64,
                data.pageX,
                data.pageZ
        );
        int width = viewport.visibleSize * CELL_SIZE + (2 * PADDING) + (2 * SIDE_GUTTER);
        int height = viewport.visibleSize * CELL_SIZE + HEADER + (2 * PADDING) + 20;
        int mapLeft = SIDE_GUTTER + PADDING;

        ModularPanel panel = ModularPanel.defaultPanel("claim_manager")
                .width(width)
                .height(height)
                .topRel(0.40f);

        panel.child(new ButtonWidget<>()
                .background(GuiTextures.BUTTON_CLEAN)
                .overlay(GuiTextures.CLOSE)
                .onMousePressed(mouseButton -> {
                    panel.closeIfOpen();
                    return true;
                })
                .width(12)
                .height(12)
                .pos(width - PADDING * 3, (PADDING / 2) + 1));

        panel.child(IKey.dynamic(() -> {
                    String claimCap = ClientClaimChunkCache.claimMax == Short.MAX_VALUE ? "INF" : String.valueOf(ClientClaimChunkCache.claimMax);
                    return "Claims " + ClientClaimChunkCache.claimCount + "/" + claimCap + " | Loaded " + ClientClaimChunkCache.forceLoadedCount + "/" + ClientClaimChunkCache.forceLoadedMax;
                }).asWidget()
                .pos(mapLeft, PADDING));
        if (viewport.visibleSize < size) {
            panel.child(IKey.str("Map paged to " + viewport.visibleSize + "x" + viewport.visibleSize).asWidget()
                    .pos(mapLeft, PADDING + 10));
        }

        addPanButtons(panel, data, viewport, width);

        for (int i = viewport.startX; i < viewport.startX + viewport.visibleSize; i++) {
            for (int j = viewport.startZ; j < viewport.startZ + viewport.visibleSize; j++) {
                final int chunkX = data.centerX - data.radius + i;
                final int chunkZ = data.centerZ - data.radius + j;
                final int localX = i - viewport.startX;
                final int localZ = j - viewport.startZ;

                panel.child(new ButtonWidget<>()
                        .overlay(liveChunkOverlay(data, chunkX, chunkZ))
                        .onMousePressed(mouseButton -> {
                            ClaimChunkInfo info = getLiveInfo(data, chunkX, chunkZ);
                            byte action = determineAction(info, mouseButton);
                            if (action == -1) {
                                return false;
                            }

                            PacketClaimChunkAction packet = new PacketClaimChunkAction();
                            packet.action = action;
                            packet.chunk = new DimChunkPos(data.dim, chunkX, chunkZ);
                            packet.center = data.getCenter();
                            packet.radius = data.radius;
                            WarForgeMod.NETWORK.sendToServer(packet);
                            return true;
                        })
                        .tooltipDynamic(tooltip -> {
                            ClaimChunkInfo info = getLiveInfo(data, chunkX, chunkZ);
                            if (info.factionId.equals(Faction.nullUuid)) {
                                tooltip.addLine("Wilderness");
                            } else {
                                tooltip.addLine("Faction: " + info.factionName);
                            }
                            tooltip.addLine("Claim Type: " + formatClaimType(info.claimType));
                            tooltip.addLine("Chunk: [" + info.x + ", " + info.z + "]");
                            if (info.vein != null) {
                                tooltip.addLine(new IngredientDrawable(
                                        info.vein.compIds.stream()
                                                .map(StackComparable::toItem)
                                                .filter(Objects::nonNull)
                                                .toArray(ItemStack[]::new)
                                ).asIcon().size(25));
                                if (info.oreQuality != null) {
                                    tooltip.addLine(IKey.str("Ore In the chunk: " +
                                            translateVeinName(info.vein.translationKey,
                                                    I18n.translateToLocal(info.oreQuality.getTranslationKey()) + " [" +
                                                            info.oreQuality.getMultString(info.vein) + "]")));
                                } else {
                                    tooltip.addLine(IKey.str("Ore In the chunk: " + translateVeinName(info.vein.translationKey, "")));
                                }
                            } else {
                                tooltip.addLine(IKey.str("No ores in this chunk"));
                            }
                            if (info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED)) {
                                tooltip.addLine("Force-loaded");
                            }
                            if (info.hasFlag(ClaimChunkInfo.FLAG_HAS_COLLECTOR)) {
                                tooltip.addLine("Has Faction Yield Storage");
                            }
                            if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_CLAIM)) {
                                tooltip.addLine("Left click to claim using first claim block in inventory");
                            }
                            if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_UNCLAIM)) {
                                tooltip.addLine("Left click to unclaim and refund claim block");
                            }
                            if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD)) {
                                tooltip.addLine(info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED) ? "Right click to unforce-load" : "Right click to force-load");
                            }
                        })
                        .size(CELL_SIZE)
                        .pos((localX * CELL_SIZE) + mapLeft, (localZ * CELL_SIZE) + PADDING + HEADER));
            }
        }

        return panel;
    }

    private static void addPanButtons(ModularPanel panel, ClaimManagerGuiData data, ChunkMapViewport viewport, int width) {
        if (!viewport.canPanNorth() && !viewport.canPanSouth() && !viewport.canPanWest() && !viewport.canPanEast()) {
            return;
        }

        if (viewport.canPanNorth()) {
            panel.child(panButton("^", width / 2 - 6, 2, data, viewport.startX, viewport.panNorth()));
        }
        if (viewport.canPanSouth()) {
            panel.child(panButton("v", width / 2 - 6, PADDING + HEADER + viewport.visibleSize * CELL_SIZE, data, viewport.startX, viewport.panSouth()));
        }
        if (viewport.canPanWest()) {
            panel.child(panButton("<", 4, PADDING + HEADER + (viewport.visibleSize * CELL_SIZE) / 2 - 6, data, viewport.panWest(), viewport.startZ));
        }
        if (viewport.canPanEast()) {
            panel.child(panButton(">", width - 16, PADDING + HEADER + (viewport.visibleSize * CELL_SIZE) / 2 - 6, data, viewport.panEast(), viewport.startZ));
        }
    }

    private static ButtonWidget<?> panButton(String text, int x, int y, ClaimManagerGuiData data, int pageX, int pageZ) {
        return new ButtonWidget<>()
                .background(GuiTextures.BUTTON_CLEAN)
                .overlay(IKey.str(text))
                .onMousePressed(mouseButton -> {
                    ClaimManagerGuiFactory.INSTANCE.openClient(data.getCenter(), data.radius, pageX, pageZ);
                    return true;
                })
                .width(12)
                .height(12)
                .pos(x, y);
    }

    private static SiegeCampAttackInfo toMapState(ClaimChunkInfo info, int centerX, int centerZ) {
        SiegeCampAttackInfo state = new SiegeCampAttackInfo();
        state.canAttack = false;
        state.mOffset = new Vec3i(info.x - centerX, 0, info.z - centerZ);
        state.mFactionUUID = info.factionId;
        state.mFactionName = info.factionName;
        state.mFactionColour = info.colour;
        state.mWarforgeVein = info.vein;
        state.mOreQuality = info.oreQuality;
        return state;
    }

    private static IDrawable liveChunkOverlay(ClaimManagerGuiData data, int chunkX, int chunkZ) {
        return (GuiContext context, int x, int y, int width, int height, WidgetTheme theme) -> {
            ClaimChunkInfo info = getLiveInfo(data, chunkX, chunkZ);
            ClaimChunkRenderInfo renderInfo = new ClaimChunkRenderInfo(
                    toMapState(info, data.centerX, data.centerZ),
                    info.claimType,
                    info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED),
                    info.outlineStyle == ClaimChunkInfo.OUTLINE_CONQUERED,
                    isBattleZoneChunk(data.dim, chunkX, chunkZ),
                    getCenterIcon(data, chunkX, chunkZ)
            );
            new MapDrawable(textureName(data.dim, chunkX, chunkZ), renderInfo, getLiveAdjacency(data, chunkX, chunkZ))
                    .draw(context, x, y, width, height, theme);
        };
    }

    private static ClaimChunkInfo getLiveInfo(ClaimManagerGuiData data, int chunkX, int chunkZ) {
        ClaimChunkInfo info = ClientClaimChunkCache.get(new DimChunkPos(data.dim, chunkX, chunkZ));
        if (info != null) {
            return info;
        }

        ClaimChunkInfo fallback = new ClaimChunkInfo();
        fallback.x = chunkX;
        fallback.z = chunkZ;
        return fallback;
    }

    private static boolean[] getLiveAdjacency(ClaimManagerGuiData data, int chunkX, int chunkZ) {
        boolean[] adjacency = new boolean[4];
        ClaimChunkInfo current = getLiveInfo(data, chunkX, chunkZ);

        if (chunkZ > data.centerZ - data.radius) {
            adjacency[0] = !current.factionId.equals(getLiveInfo(data, chunkX, chunkZ - 1).factionId);
        }
        if (chunkX < data.centerX + data.radius) {
            adjacency[1] = !current.factionId.equals(getLiveInfo(data, chunkX + 1, chunkZ).factionId);
        }
        if (chunkZ < data.centerZ + data.radius) {
            adjacency[2] = !current.factionId.equals(getLiveInfo(data, chunkX, chunkZ + 1).factionId);
        }
        if (chunkX > data.centerX - data.radius) {
            adjacency[3] = !current.factionId.equals(getLiveInfo(data, chunkX - 1, chunkZ).factionId);
        }

        return adjacency;
    }

    private static ResourceLocation getCenterIcon(ClaimManagerGuiData data, int chunkX, int chunkZ) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) {
            return null;
        }
        if (chunkX != data.centerX || chunkZ != data.centerZ) {
            return null;
        }
        return SkinUtil.getPlayerFace(minecraft.player.getUniqueID());
    }

    private static boolean isBattleZoneChunk(int dim, int chunkX, int chunkZ) {
        ChunkPos target = new ChunkPos(chunkX, chunkZ);
        for (var siegeInfo : ClientProxy.sSiegeInfo.values()) {
            if (siegeInfo == null || siegeInfo.attackingPos == null || siegeInfo.attackingPos.dim != dim || siegeInfo.warzoneChunks == null) {
                continue;
            }
            if (siegeInfo.warzoneChunks.contains(target)) {
                return true;
            }
        }
        return false;
    }

    private static byte determineAction(ClaimChunkInfo info, int mouseButton) {
        if (mouseButton == 1 && info.hasFlag(ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD)) {
            return PacketClaimChunkAction.ACTION_TOGGLE_FORCELOAD;
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_CLAIM)) {
            return PacketClaimChunkAction.ACTION_CLAIM;
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_UNCLAIM)) {
            return PacketClaimChunkAction.ACTION_UNCLAIM;
        }
        if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD)) {
            return PacketClaimChunkAction.ACTION_TOGGLE_FORCELOAD;
        }
        return -1;
    }

    private static String formatClaimType(Faction.ClaimType claimType) {
        return switch (claimType) {
            case BASIC -> "Basic";
            case REINFORCED -> "Reinforced";
            case CITADEL -> "Citadel";
            case ADMIN -> "Admin";
            case SIEGE -> "Siege";
            default -> "None";
        };
    }

    private static String translateVeinName(String translationKey, Object... args) {
        String resolvedKey = translationKey;
        if (!I18n.canTranslate(resolvedKey) && I18n.canTranslate(resolvedKey + ".name")) {
            resolvedKey += ".name";
        }
        return I18n.translateToLocalFormatted(resolvedKey, args);
    }

    private static String textureName(int dim, int x, int z) {
        return "claimmap/" + dim + "_" + x + "_" + z;
    }
}
