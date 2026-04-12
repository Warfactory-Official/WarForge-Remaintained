package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.flansmod.warforge.api.Color4i;
import com.flansmod.warforge.api.Time;
import com.flansmod.warforge.api.modularui.ChunkMapTextureDaemon;
import com.flansmod.warforge.api.modularui.ChunkMapUtil;
import com.flansmod.warforge.api.modularui.ChunkMapViewport;
import com.flansmod.warforge.api.modularui.MapDrawable;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.network.PacketStartSiege;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfoRender;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.StackComparable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

public class GuiSiegeCamp {

    @SideOnly(Side.CLIENT)
    public static ModularScreen makeGUI(DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks, byte momentum) {
        return makeGUI(siegeCampPos, possibleAttacks, momentum, -1, -1);
    }

    @SideOnly(Side.CLIENT)
    public static ModularScreen makeGUI(DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks, byte momentum, int pageX, int pageZ) {
        ChunkPos centerChunk = siegeCampPos.toChunkPos();

        int radius = 2;  // 2 chunks in each direction → 5x5 total area
        int centerX = centerChunk.x;
        int centerZ = centerChunk.z;
        int offset = 6;
        int VERT_OFFSET = 13;
        int totalSize = 2 * radius + 1;
        ScaledResolution scaled = new ScaledResolution(Minecraft.getMinecraft());
        int cell = ChunkMapViewport.recommendedCellSize(scaled);
        ChunkMapViewport viewport = ChunkMapViewport.create(
                totalSize,
                3,
                totalSize,
                cell,
                scaled.getScaledWidth(),
                scaled.getScaledHeight(),
                48,
                72,
                pageX,
                pageZ
        );
        int WIDTH = (cell * viewport.visibleSize) + (2 * offset);
        int HEIGHT = (cell * viewport.visibleSize) + VERT_OFFSET + (int) (2.5 * offset) + 12;
        ModularPanel panel = ModularPanel.defaultPanel("siege_main")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);

        HashMap<Long, SiegeCampAttackInfo> byOffset = new HashMap<Long, SiegeCampAttackInfo>();
        for (SiegeCampAttackInfo info : possibleAttacks) {
            byOffset.put(ChunkMapUtil.key(info.mOffset.getX(), info.mOffset.getZ()), info);
        }
        List<SiegeCampAttackInfo> orderedAttacks = new ArrayList<SiegeCampAttackInfo>(totalSize * totalSize);
        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                orderedAttacks.add(byOffset.get(ChunkMapUtil.key(x, z)));
            }
        }

        HashMap<Long, Integer> tintByChunk = new HashMap<Long, Integer>();
        for (SiegeCampAttackInfo info : orderedAttacks) {
            if (!info.mFactionUUID.equals(com.flansmod.warforge.server.Faction.nullUuid)) {
                tintByChunk.put(ChunkMapUtil.key(centerX + info.mOffset.getX(), centerZ + info.mOffset.getZ()), info.mFactionColour);
            }
        }
        String textureNamespace = "siegemap";
        ChunkMapTextureDaemon.requestMapUpdate(textureNamespace, siegeCampPos.dim, centerX, centerZ, radius, tintByChunk);

        boolean[][] adjacencyArray = new boolean[orderedAttacks.size()][4];
        ChunkMapUtil.computeAdjacency(orderedAttacks, radius, adjacencyArray);

        panel.child(new ButtonWidget<>()
                .background(GuiTextures.BUTTON_CLEAN)
                .overlay(GuiTextures.CLOSE)
                .onMousePressed(mouseButton -> {
                            panel.closeIfOpen();
                            return true;
                        }
                )
                .width(12)
                .height(12)
                .pos(WIDTH - offset * 3, (offset / 2) + 1)
        );

        panel.child(IKey.str("Select chunk to siege").asWidget()
                .pos(offset, offset)
        );

        panel.child(IKey.str("Current momentum: " + momentum).asWidget()
                .pos(WIDTH - (offset + 12 + 5 + 100), offset)
                .tooltip(richTooltip -> {
                    richTooltip.addLine("§6Momentum System§r");
                    richTooltip.addLine("§7Momentum affects siege duration.§r");
                    richTooltip.addLine("§7Gained via §aWON§7 sieges, lost on §cLOST§7 sieges.§r");
                    richTooltip.addLine("§7Expires after §b" + WarForgeConfig.SIEGE_MOMENTUM_DURATION + " minutes§7.§r");

                    //FIXME
                    for (byte level : WarForgeConfig.SIEGE_MOMENTUM_TIME.keySet()) {
                        long time = WarForgeConfig.SIEGE_MOMENTUM_TIME.get(level) * 1000;
                        richTooltip.addLine("§eLevel " + level + "§r: §a" + new Time(time).getFormattedTime(Time.TimeFormat.MINUTES_SECONDS, Time.Verbality.SHORT));
                    }
                })
        );

        addPanButtons(panel, siegeCampPos, orderedAttacks, momentum, viewport, WIDTH, offset, VERT_OFFSET, cell);

        for (int i = viewport.startX; i < viewport.startX + viewport.visibleSize; i++) {
            for (int j = viewport.startZ; j < viewport.startZ + viewport.visibleSize; j++) {
                int index = j * totalSize + i;
                int localX = i - viewport.startX;
                int localZ = j - viewport.startZ;
                SiegeCampAttackInfoRender chunkInfo = new SiegeCampAttackInfoRender(orderedAttacks.get(index));
                if (chunkInfo.mOffset.getX() == 0 && chunkInfo.mOffset.getZ() == 0) {
                    chunkInfo.setCenterMarkType(SiegeCampAttackInfoRender.CenterMarkType.SIEGE_CAMP);
                }
                panel.child(new ButtonWidget<>()
                .overlay(new MapDrawable(
                                ChunkMapTextureDaemon.getTextureName(textureNamespace, siegeCampPos.dim, centerX + chunkInfo.mOffset.getX(), centerZ + chunkInfo.mOffset.getZ()),
                                chunkInfo,
                                adjacencyArray[index]
                        ))
                        .onMousePressed(mouseButton -> {
                            if ((chunkInfo.mOffset.getX() == 0 && chunkInfo.mOffset.getZ() == 0) && !chunkInfo.canAttack)
                                return false;
                            //player.sendMessage(new TextComponentString(chunkInfo.mOffset.toString()));  debug vec dis
                            PacketStartSiege siegePacket = new PacketStartSiege();

                            siegePacket.mSiegeCampPos = siegeCampPos;
                            siegePacket.mOffset = chunkInfo.mOffset;

                            WarForgeMod.NETWORK.sendToServer(siegePacket);
                            panel.closeIfOpen();
                            return true;
                        })
                        .tooltip(richTooltip ->
                        {
                            if (!chunkInfo.mFactionName.isEmpty()) {
                                richTooltip.addLine(IKey.str("Territory of " + chunkInfo.mFactionName).color(Color4i.fromRGB(chunkInfo.mFactionColour).toARGB()));
                            } else {
                                richTooltip.addLine(IKey.str("Wilderness").style(IKey.GREEN));
                            }
                            if (chunkInfo.mWarforgeVein != null) {

                                richTooltip.addLine(new IngredientDrawable(
                                        chunkInfo.mWarforgeVein.compIds.stream()
                                                .map(StackComparable::toItem)
                                                .filter(Objects::nonNull)
                                                .toArray(ItemStack[]::new)
                                ).asIcon().size(25));
                                richTooltip.addLine(IKey.str("Ore In the chunk: " +
                                        I18n.format(chunkInfo.mWarforgeVein.translationKey,
                                                I18n.format(chunkInfo.mOreQuality.getTranslationKey()) + " [" +
                                                chunkInfo.mOreQuality.getMultString(chunkInfo.mWarforgeVein) + "]")));
                            } else {
                                richTooltip.addLine(IKey.str("No ores in this chunk"));
                            }
                            if (chunkInfo.canAttack) {
                                richTooltip.addLine(IKey.str("Total attack time: " + new Time(WarForgeConfig.SIEGE_MOMENTUM_TIME.get(momentum) * 1000).getFormattedTime(Time.TimeFormat.HOURS_MINUTES_SECONDS, Time.Verbality.SHORT)).style(IKey.RED));
                                richTooltip.addLine(IKey.str("Click to attack now!").style(IKey.BOLD, IKey.RED));
                            }
                        })
                        .size(cell)
                        .pos((localX * cell + offset), (localZ * cell + offset) + VERT_OFFSET));
            }
        }

        return new ModularScreen(panel);
    }

    private static void addPanButtons(ModularPanel panel, DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks, byte momentum, ChunkMapViewport viewport, int width, int offset, int topOffset, int cell) {
        if (!viewport.canPanNorth() && !viewport.canPanSouth() && !viewport.canPanWest() && !viewport.canPanEast()) {
            return;
        }

        if (viewport.canPanNorth()) {
            panel.child(panButton("^", width / 2 - 6, 2, siegeCampPos, possibleAttacks, momentum, viewport.startX, viewport.panNorth()));
        }
        if (viewport.canPanSouth()) {
            panel.child(panButton("v", width / 2 - 6, offset + topOffset + viewport.visibleSize * cell, siegeCampPos, possibleAttacks, momentum, viewport.startX, viewport.panSouth()));
        }
        if (viewport.canPanWest()) {
            panel.child(panButton("<", 2, offset + topOffset + (viewport.visibleSize * cell) / 2 - 6, siegeCampPos, possibleAttacks, momentum, viewport.panWest(), viewport.startZ));
        }
        if (viewport.canPanEast()) {
            panel.child(panButton(">", width - 14, offset + topOffset + (viewport.visibleSize * cell) / 2 - 6, siegeCampPos, possibleAttacks, momentum, viewport.panEast(), viewport.startZ));
        }
    }

    private static ButtonWidget<?> panButton(String text, int x, int y, DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks, byte momentum, int pageX, int pageZ) {
        return new ButtonWidget<>()
                .background(GuiTextures.BUTTON_CLEAN)
                .overlay(IKey.str(text))
                .onMousePressed(mouseButton -> {
                    ClientGUI.open(makeGUI(siegeCampPos, possibleAttacks, momentum, pageX, pageZ));
                    return true;
                })
                .width(12)
                .height(12)
                .pos(x, y);
    }

}
