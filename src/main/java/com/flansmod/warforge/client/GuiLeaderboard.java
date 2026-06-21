package com.flansmod.warforge.client;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.common.network.LeaderboardInfo;
import com.flansmod.warforge.common.network.PacketLeaderboardInfo;
import com.flansmod.warforge.common.network.PacketRequestLeaderboardInfo;
import com.flansmod.warforge.server.Leaderboard.FactionStat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GuiLeaderboard extends Screen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "gui/leaderboard.png");

    private FactionStat currentStat;
    private LeaderboardInfo info;
    private final int xSize = 256;
    private final int ySize = 191;

    private int guiLeft;
    private int guiTop;

    public GuiLeaderboard() {
        super(Component.literal("Leaderboard"));
        this.info = PacketLeaderboardInfo.sLatestInfo;
        this.currentStat = this.info != null ? this.info.stat : FactionStat.TOTAL;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        statButton("Total", FactionStat.TOTAL, this.guiLeft + 6);
        statButton("Notoriety", FactionStat.NOTORIETY, this.guiLeft + 6 + 62);
        statButton("Legacy", FactionStat.LEGACY, this.guiLeft + 6 + 124);
        statButton("Wealth", FactionStat.WEALTH, this.guiLeft + 6 + 186);
    }

    private void statButton(String label, FactionStat stat, int x) {
        Button button = Button.builder(Component.literal(label), b -> selectStat(stat))
                .bounds(x, this.guiTop + 18, 58, 20).build();
        button.active = this.currentStat != stat;
        addRenderableWidget(button);
    }

    private void selectStat(FactionStat stat) {
        this.currentStat = stat;
        this.info = null;

        PacketRequestLeaderboardInfo packet = new PacketRequestLeaderboardInfo();
        packet.firstIndex = 0;
        packet.stat = stat;
        WarForgeMod.NETWORK.sendToServer(packet);

        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);

        int j = this.guiLeft;
        int k = this.guiTop;
        graphics.blit(TEXTURE, j, k, 0, 0, this.xSize, this.ySize);

        super.render(graphics, mouseX, mouseY, partialTicks);

        String title = switch (this.currentStat) {
            case LEGACY -> "Legacy Leaderboard";
            case NOTORIETY -> "Notoriety Leaderboard";
            case TOTAL -> "Top Leaderboard";
            case WEALTH -> "Wealth Leaderboard";
        };
        graphics.drawString(this.font, title, j + this.xSize / 2 - this.font.width(title) / 2, k + 6, 0xFFFFFF, true);

        String subtitle = switch (this.currentStat) {
            case LEGACY -> "Factions that have been active longest";
            case NOTORIETY -> "Factions that are top in PvP and Siegeing";
            case TOTAL -> "Top Factions, with combined scores";
            case WEALTH -> "Factions with the most wealth in their citadel";
        };
        graphics.drawString(this.font, subtitle, j + 8, k + 40, 0xF0F0F0, true);

        if (this.info == null) {
            String text = "Waiting for server...";
            graphics.drawString(this.font, text, j + this.xSize / 2 - this.font.width(text) / 2, k + 80, 0xA0A0A0, true);
            return;
        }

        if (this.info.myFaction != null) {
            renderFactionInfo(graphics, j + 7, k + 55, this.info.myFaction, this.info.myFaction.legacyRank);
        }

        for (int i = 0; i < LeaderboardInfo.NUM_LEADERBOARD_ENTRIES_PER_PAGE; i++) {
            if (this.info.factionInfos[i] != null) {
                renderFactionInfo(graphics, j + 7, k + 72 + 12 * i, this.info.factionInfos[i], this.info.firstIndex + i + 1);
            }
        }
    }

    private void renderFactionInfo(GuiGraphics graphics, int x, int y, FactionDisplayInfo faction, int oneIndexedRank) {
        int stat = switch (this.currentStat) {
            case LEGACY -> faction.legacy;
            case NOTORIETY -> faction.notoriety;
            case WEALTH -> faction.wealth;
            case TOTAL -> faction.legacy + faction.notoriety + faction.wealth;
        };

        int colour = switch (oneIndexedRank) {
            case 1 -> 0xFFD700;
            case 2 -> 0xC0C0C0;
            case 3 -> 0xCD7F32;
            default -> 0xFFFFFF;
        };
        boolean shadow = oneIndexedRank <= 3;
        graphics.drawString(this.font, faction.factionName, x + 2, y + 1, colour, shadow);
        graphics.drawString(this.font, "" + stat, x + 150, y + 1, colour, shadow);
        graphics.drawString(this.font, "#" + oneIndexedRank, x + 200, y + 1, colour, shadow);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
