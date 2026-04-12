package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.flansmod.warforge.api.modularui.ChunkMapTextureDaemon;
import com.flansmod.warforge.api.modularui.ChunkMapViewport;
import com.flansmod.warforge.api.modularui.ChunkMapUtil;
import com.flansmod.warforge.api.modularui.MapDrawable;
import com.flansmod.warforge.client.util.SkinUtil;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.ClaimChunkRenderInfo;
import com.flansmod.warforge.common.network.PacketClaimChunkAction;
import com.flansmod.warforge.common.network.PacketClaimChunksData;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.StackComparable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GuiClaimManager {
    public static ModularScreen makeGUI(PacketClaimChunksData data) {
        return makeGUI(data, -1, -1);
    }

    public static ModularScreen makeGUI(PacketClaimChunksData data, int pageX, int pageZ) {
        int size = data.radius * 2 + 1;
        int padding = 6;
        int header = 22;
        ScaledResolution scaled = new ScaledResolution(Minecraft.getMinecraft());
        int cell = ChunkMapViewport.recommendedCellSize(scaled);
        ChunkMapViewport viewport = ChunkMapViewport.create(
                size,
                3,
                size,
                cell,
                scaled.getScaledWidth(),
                scaled.getScaledHeight(),
                padding * 2 + 20,
                64,
                pageX,
                pageZ
        );
        int width = viewport.visibleSize * cell + (2 * padding);
        int height = viewport.visibleSize * cell + header + (2 * padding) + 20;

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
                .pos(width - padding * 3, (padding / 2) + 1)
        );

        String claimCap = data.claimMax == Short.MAX_VALUE ? "INF" : String.valueOf(data.claimMax);
        panel.child(IKey.str("Claims " + data.claimCount + "/" + claimCap + " | Loaded " + data.forceLoadedCount + "/" + data.forceLoadedMax).asWidget()
                .pos(padding, padding));
        if (viewport.visibleSize < size) {
            panel.child(IKey.str("Map scaled to " + viewport.visibleSize + "x" + viewport.visibleSize).asWidget()
                    .pos(padding, padding + 10));
        }

        addPanButtons(panel, data, viewport, width, padding, header, cell);

        Map<Long, ClaimChunkInfo> byChunk = new HashMap<Long, ClaimChunkInfo>();
        for (ClaimChunkInfo chunk : data.chunks) {
            byChunk.put(ChunkMapUtil.key(chunk.x, chunk.z), chunk);
        }

        List<ClaimChunkInfo> orderedClaims = new ArrayList<ClaimChunkInfo>(size * size);
        List<SiegeCampAttackInfo> mapState = new ArrayList<SiegeCampAttackInfo>(size * size);

        for (int z = data.centerZ - data.radius; z <= data.centerZ + data.radius; z++) {
            for (int x = data.centerX - data.radius; x <= data.centerX + data.radius; x++) {
                ClaimChunkInfo info = byChunk.getOrDefault(ChunkMapUtil.key(x, z), new ClaimChunkInfo());
                info.x = x;
                info.z = z;
                orderedClaims.add(info);
                mapState.add(toMapState(info, data.centerX, data.centerZ));
            }
        }

        boolean[][] adjacency = new boolean[mapState.size()][4];
        ChunkMapUtil.computeAdjacency(mapState, data.radius, adjacency);
        ResourceLocation face = Minecraft.getMinecraft().player == null ? null : SkinUtil.getPlayerFace(Minecraft.getMinecraft().player.getUniqueID());

        for (int i = viewport.startX; i < viewport.startX + viewport.visibleSize; i++) {
            for (int j = viewport.startZ; j < viewport.startZ + viewport.visibleSize; j++) {
                int fullIndex = j * size + i;
                ClaimChunkInfo info = orderedClaims.get(fullIndex);
                ResourceLocation tileIcon = (info.x == data.centerX && info.z == data.centerZ) ? face : null;
                ClaimChunkRenderInfo renderInfo = new ClaimChunkRenderInfo(
                        mapState.get(fullIndex),
                        info.claimType,
                        info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED),
                        tileIcon
                );
                String textureName = ChunkMapTextureDaemon.getTextureName("claimmap", data.dim, info.x, info.z);
                final int finalIndex = fullIndex;
                int localX = i - viewport.startX;
                int localZ = j - viewport.startZ;

                panel.child(new ButtonWidget<>()
                        .overlay(new MapDrawable(textureName, renderInfo, adjacency[finalIndex]))
                        .onMousePressed(mouseButton -> {
                            byte action = determineAction(info, mouseButton);
                            if (action == -1) {
                                return false;
                            }

                            PacketClaimChunkAction packet = new PacketClaimChunkAction();
                            packet.action = action;
                            packet.chunk = new DimChunkPos(data.dim, info.x, info.z);
                            packet.center = new DimChunkPos(data.dim, data.centerX, data.centerZ);
                            packet.radius = data.radius;
                            WarForgeMod.NETWORK.sendToServer(packet);
                            panel.closeIfOpen();
                            return true;
                        })
                        .tooltip(tooltip -> {
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
                                            I18n.format(info.vein.translationKey,
                                                    I18n.format(info.oreQuality.getTranslationKey()) + " [" +
                                                            info.oreQuality.getMultString(info.vein) + "]")));
                                } else {
                                    tooltip.addLine(IKey.str("Ore In the chunk: " + I18n.format(info.vein.translationKey, "")));
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
                        .size(cell)
                        .pos((localX * cell) + padding, (localZ * cell) + padding + header));
            }
        }

        return new ModularScreen(panel);
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

    private static void addPanButtons(ModularPanel panel, PacketClaimChunksData data, ChunkMapViewport viewport, int width, int padding, int header, int cell) {
        if (!viewport.canPanNorth() && !viewport.canPanSouth() && !viewport.canPanWest() && !viewport.canPanEast()) {
            return;
        }

        if (viewport.canPanNorth()) {
            panel.child(panButton("^", width / 2 - 6, 2, data, viewport.startX, viewport.panNorth()));
        }
        if (viewport.canPanSouth()) {
            panel.child(panButton("v", width / 2 - 6, padding + header + viewport.visibleSize * cell, data, viewport.startX, viewport.panSouth()));
        }
        if (viewport.canPanWest()) {
            panel.child(panButton("<", 2, padding + header + (viewport.visibleSize * cell) / 2 - 6, data, viewport.panWest(), viewport.startZ));
        }
        if (viewport.canPanEast()) {
            panel.child(panButton(">", width - 14, padding + header + (viewport.visibleSize * cell) / 2 - 6, data, viewport.panEast(), viewport.startZ));
        }
    }

    private static ButtonWidget<?> panButton(String text, int x, int y, PacketClaimChunksData data, int pageX, int pageZ) {
        return new ButtonWidget<>()
                .background(GuiTextures.BUTTON_CLEAN)
                .overlay(IKey.str(text))
                .onMousePressed(mouseButton -> {
                    ClientGUI.open(makeGUI(data, pageX, pageZ));
                    return true;
                })
                .width(12)
                .height(12)
                .pos(x, y);
    }
}
