package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.flansmod.warforge.api.modularui.ClaimChunkDrawable;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.PacketClaimChunkAction;
import com.flansmod.warforge.common.network.PacketClaimChunksData;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;

import java.util.HashMap;
import java.util.Map;

public class GuiClaimManager {
    public static ModularScreen makeGUI(PacketClaimChunksData data) {
        int size = data.radius * 2 + 1;
        int cell = 18;
        int padding = 8;
        int titleHeight = 26;
        int width = padding * 2 + size * cell;
        int height = padding * 2 + titleHeight + size * cell;

        ModularPanel panel = ModularPanel.defaultPanel("claim_manager")
                .width(width)
                .height(height)
                .topRel(0.35f);

        panel.child(new ButtonWidget<>()
                .background(GuiTextures.BUTTON_CLEAN)
                .overlay(GuiTextures.CLOSE)
                .onMousePressed(mouseButton -> {
                    panel.closeIfOpen();
                    return true;
                })
                .width(12)
                .height(12)
                .pos(width - 18, 5));

        String claimCap = data.claimMax == Short.MAX_VALUE ? "INF" : String.valueOf(data.claimMax);
        panel.child(IKey.str("Claims " + data.claimCount + "/" + claimCap + " | Loaded " + data.forceLoadedCount + "/" + data.forceLoadedMax)
                .asWidget()
                .pos(padding, padding));
        panel.child(IKey.str("LMB: claim/unclaim, RMB: force-load toggle")
                .asWidget()
                .pos(padding, padding + 10));

        Map<Long, ClaimChunkInfo> byChunk = new HashMap<Long, ClaimChunkInfo>();
        for (ClaimChunkInfo chunk : data.chunks) {
            byChunk.put(key(chunk.x, chunk.z), chunk);
        }

        int originY = padding + titleHeight;
        for (int dz = -data.radius; dz <= data.radius; dz++) {
            for (int dx = -data.radius; dx <= data.radius; dx++) {
                int chunkX = data.centerX + dx;
                int chunkZ = data.centerZ + dz;
                ClaimChunkInfo info = byChunk.getOrDefault(key(chunkX, chunkZ), new ClaimChunkInfo());
                info.x = chunkX;
                info.z = chunkZ;

                int x = padding + (dx + data.radius) * cell;
                int y = originY + (dz + data.radius) * cell;

                panel.child(new ButtonWidget<>()
                        .overlay(new ClaimChunkDrawable(info))
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
                            tooltip.addLine("Chunk: [" + info.x + ", " + info.z + "]");
                            if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_CLAIM)) {
                                tooltip.addLine("Left click to claim");
                            }
                            if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_UNCLAIM)) {
                                tooltip.addLine("Left click to unclaim");
                            }
                            if (info.hasFlag(ClaimChunkInfo.FLAG_CAN_TOGGLE_FORCELOAD)) {
                                tooltip.addLine((info.hasFlag(ClaimChunkInfo.FLAG_FORCE_LOADED) ? "Right click to unforce-load" : "Right click to force-load"));
                            }
                        })
                        .size(cell)
                        .pos(x, y));
            }
        }

        return new ModularScreen(panel);
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

    private static long key(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xFFFF_FFFFL);
    }
}
