package com.flansmod.warforge.client;

import java.util.UUID;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.util.SkinUtil;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.common.network.PlayerDisplayInfo;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.Faction.Role;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GuiFactionInfo extends Screen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "gui/factioninfo.png");

    private enum EnumTab {
        STATS,
        ALLIES,
        SIEGES,
        PLAYER,
    }

    private final FactionDisplayInfo info;
    private final int xSize = 176;
    private final int ySize = 256;

    private int guiLeft;
    private int guiTop;
    private EnumTab currentTab = EnumTab.STATS;
    private PlayerDisplayInfo lookingAt = null;

    private Button promote, demote, kick;

    public GuiFactionInfo() {
        super(Component.literal("Faction Info"));
        this.info = PacketFactionInfo.latestInfo;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        int j = this.guiLeft;
        int k = this.guiTop;

        Button stats = Button.builder(Component.literal("Stats"), b -> selectTab(EnumTab.STATS, null))
                .bounds(j + 53, k + 127, 70, 20).build();
        addRenderableWidget(stats);

        Button allies = Button.builder(Component.literal("Allies"), b -> selectTab(EnumTab.ALLIES, null))
                .bounds(j + 5, k + 127, 46, 20).build();
        allies.active = false; // TODO: Not implemented yet
        addRenderableWidget(allies);

        Button sieges = Button.builder(Component.literal("Sieges"), b -> selectTab(EnumTab.SIEGES, null))
                .bounds(j + 125, k + 127, 46, 20).build();
        sieges.active = false; // TODO: Not implemented yet
        addRenderableWidget(sieges);

        this.promote = Button.builder(Component.literal("Promote"), b -> sendPlayerCommand("promote"))
                .bounds(j + 108, k + 152, 60, 20).build();
        addRenderableWidget(this.promote);
        this.demote = Button.builder(Component.literal("Demote"), b -> sendPlayerCommand("demote"))
                .bounds(j + 108, k + 174, 60, 20).build();
        addRenderableWidget(this.demote);
        this.kick = Button.builder(Component.literal("Kick"), b -> sendPlayerCommand("kick"))
                .bounds(j + 108, k + 196, 60, 20).build();
        addRenderableWidget(this.kick);

        // Player selection tiles (invisible, clickable). Layout mirrors the face grid in render().
        int rowLength = 7;
        int columnHeight = 2;
        int leaderInfoIndex = -1;

        for (int y = 0; y < columnHeight; y++) {
            for (int x = 0; x < rowLength; x++) {
                int index = x + y * rowLength;
                if (leaderInfoIndex != -1) {
                    index++;
                }
                if (index < this.info.members.size()) {
                    PlayerDisplayInfo playerInfo = this.info.members.get(index);
                    if (leaderInfoIndex == -1
                            && (playerInfo.role == Role.LEADER || playerInfo.playerUuid.equals(this.info.mLeaderID))) {
                        leaderInfoIndex = index;
                        if (index + 1 < this.info.members.size()) {
                            index++;
                        } else {
                            continue;
                        }
                    }
                    int selectIndex = index;
                    addRenderableWidget(new GuiInvisibleButton(j + 5 + 24 * x, k + 79 + 24 * y, 22, 22,
                            b -> selectPlayer(selectIndex)));
                }
            }
        }

        if (leaderInfoIndex != -1) {
            int selectIndex = leaderInfoIndex;
            addRenderableWidget(new GuiInvisibleButton(j + 34, k + 32, 22, 22, b -> selectPlayer(selectIndex)));
        }

        selectTab(EnumTab.STATS, null);
    }

    private void selectPlayer(int index) {
        if (index >= 0 && index < this.info.members.size()) {
            selectTab(EnumTab.PLAYER, this.info.members.get(index).playerUuid);
        } else {
            WarForgeMod.LOGGER.error("Pressed a button with unknown player index");
        }
    }

    private void sendPlayerCommand(String command) {
        if (this.lookingAt == null) {
            return;
        }
        Minecraft.getInstance().player.connection.sendCommand("f " + command + " " + this.lookingAt.username);
        ClientProxy.requestFactionInfo(this.info.factionId);
    }

    private void selectTab(EnumTab tab, UUID playerID) {
        UUID myID = Minecraft.getInstance().player.getUUID();
        PlayerDisplayInfo myInfo = this.info.getPlayerInfo(myID);
        boolean isMyFaction = myInfo != null;

        this.promote.visible = tab == EnumTab.PLAYER && isMyFaction;
        this.demote.visible = tab == EnumTab.PLAYER && isMyFaction;
        this.kick.visible = tab == EnumTab.PLAYER && isMyFaction;

        if (tab == EnumTab.PLAYER) {
            this.lookingAt = this.info.getPlayerInfo(playerID);
            if (this.lookingAt != null) {
                this.promote.active = isMyFaction && myInfo.role == Role.LEADER && this.lookingAt.role == Role.MEMBER;
                this.demote.active = isMyFaction && myInfo.role == Role.LEADER && this.lookingAt.role == Role.OFFICER;
                this.kick.active = isMyFaction && this.lookingAt.role.ordinal() < myInfo.role.ordinal();
            }
        }

        this.currentTab = tab;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);

        int j = this.guiLeft;
        int k = this.guiTop;
        graphics.blit(TEXTURE, j, k, 0, 0, this.xSize, this.ySize);

        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawString(this.font, this.info.factionName,
                j + this.xSize / 2 - this.font.width(this.info.factionName) / 2, k + 13, 0xFFFFFF, true);

        int rowLength = 7;
        int columnHeight = 2;
        PlayerDisplayInfo leaderInfo = null;

        for (int y = 0; y < columnHeight; y++) {
            for (int x = 0; x < rowLength; x++) {
                int index = x + y * rowLength;
                if (leaderInfo != null) {
                    index++;
                }
                if (index < this.info.members.size()) {
                    PlayerDisplayInfo playerInfo = this.info.members.get(index);
                    if (leaderInfo == null
                            && (playerInfo.role == Role.LEADER || playerInfo.playerUuid.equals(this.info.mLeaderID))) {
                        leaderInfo = playerInfo;
                        if (index + 1 < this.info.members.size()) {
                            playerInfo = this.info.members.get(index + 1);
                        } else {
                            continue;
                        }
                    }
                    graphics.blit(TEXTURE, j + 5 + 24 * x, k + 79 + 24 * y,
                            playerInfo.role == Role.OFFICER ? 176 : 198, 0, 22, 22);
                    renderPlayerFace(graphics, j + 8 + 24 * x, k + 82 + 24 * y, playerInfo.playerUuid);
                }
            }
        }

        if (leaderInfo != null) {
            graphics.drawString(this.font, "Leader", j + 56, k + 31, 0xFFFFFF, true);
            graphics.drawString(this.font, leaderInfo.username, j + 56, k + 42, 0xFFFFFF, true);
            renderPlayerFace(graphics, j + 34, k + 32, leaderInfo.playerUuid);
        }

        switch (this.currentTab) {
            case STATS -> {
                graphics.drawString(this.font, "Notoriety:", j + 8, k + 152, 0xFFFFFF, true);
                graphics.drawString(this.font, "Wealth:", j + 8, k + 162, 0xFFFFFF, true);
                graphics.drawString(this.font, "Legacy:", j + 8, k + 172, 0xFFFFFF, true);
                graphics.drawString(this.font, "Total:", j + 8, k + 182, 0xFFFFFF, true);
                graphics.drawString(this.font, "Members:", j + 8, k + 192, 0xFFFFFF, true);

                int maxLvl = 0;
                if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
                    maxLvl = WarForgeMod.UPGRADE_HANDLER.getLEVELS().length - 1;
                }
                int levelColour = this.info.lvl >= maxLvl ? 0xFFAA00 : 0xFFFFFF;
                graphics.drawString(this.font, "Citadel Level:", j + 8, k + 202, levelColour, true);

                int column1X = 120;
                graphics.drawString(this.font, "" + this.info.notoriety, j + column1X, k + 152, 0xFFFFFF, true);
                graphics.drawString(this.font, "" + this.info.wealth, j + column1X, k + 162, 0xFFFFFF, true);
                graphics.drawString(this.font, "" + this.info.legacy, j + column1X, k + 172, 0xFFFFFF, true);
                graphics.drawString(this.font, "" + (this.info.notoriety + this.info.wealth + this.info.legacy),
                        j + column1X, k + 182, 0xFFFFFF, true);
                graphics.drawString(this.font, "" + this.info.members.size(), j + column1X, k + 192, 0xFFFFFF, true);
                if (WarForgeConfig.ENABLE_CITADEL_UPGRADES) {
                    graphics.drawString(this.font,
                            "" + this.info.lvl + " [" + WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(this.info.lvl) + "]",
                            j + column1X, k + 202, levelColour, true);
                }

                int column2X = 150;
                graphics.drawString(this.font, "#" + this.info.notorietyRank, j + column2X, k + 152, 0xFFFFFF, true);
                graphics.drawString(this.font, "#" + this.info.wealthRank, j + column2X, k + 162, 0xFFFFFF, true);
                graphics.drawString(this.font, "#" + this.info.legacyRank, j + column2X, k + 172, 0xFFFFFF, true);
                graphics.drawString(this.font, "#" + this.info.totalRank, j + column2X, k + 182, 0xFFFFFF, true);
            }
            case ALLIES, SIEGES -> {
                // TODO: not implemented yet
            }
            case PLAYER -> {
                if (this.lookingAt != null) {
                    graphics.drawString(this.font, this.lookingAt.username, j + 8, k + 152, 0xFFFFFF, true);
                    String roleName = switch (this.lookingAt.role) {
                        case GUEST -> "Guest";
                        case LEADER -> "Leader";
                        case MEMBER -> "Member";
                        case OFFICER -> "Officer";
                    };
                    graphics.drawString(this.font, roleName, j + 8, k + 162, 0xFFFFFF, true);
                }
            }
        }
    }

    private void renderPlayerFace(GuiGraphics graphics, int x, int y, UUID playerUuid) {
        // Draws the real skin's 8x8 head face + hat overlay, scaled to the 16x16 tile.
        SkinUtil.drawFace(graphics, playerUuid, x, y, 16);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
