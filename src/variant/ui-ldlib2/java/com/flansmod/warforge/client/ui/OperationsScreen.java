package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.DataRevision;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.ui.model.UiText;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.OperationsGuiData;
import com.flansmod.warforge.common.network.PacketOperationsData;
import com.flansmod.warforge.common.network.PacketRequestFobWarp;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

@OnlyIn(Dist.CLIENT)
public final class OperationsScreen {
    private static final DataRevision REVISION = new DataRevision();
    private static final int WIDTH = 372;
    private static final int HEIGHT = 268;
    private static final int SIEGE_Y = 48;
    private static final int FOB_Y = 150;
    private static final int ROW_WIDTH = WIDTH - 44;

    private OperationsScreen() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        PacketOperationsData data = PacketOperationsData.latest;
        if (data != null && REVISION.unchanged(ClientCompat.screen(), data)) {
            return;
        }

        boolean hasFaction = data != null && data.hasFaction;
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int siegeSectionHeight = FOB_Y - SIEGE_Y - 6;
        int fobSectionHeight = HEIGHT - FOB_Y - 12;

        UIElement root = screenRoot();
        UIElement panel = panel(WIDTH, HEIGHT);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        colorStripe(panel, hasFaction ? data.factionColor : NO_FACTION_STRIPE, HEIGHT);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));
        title(panel, tr("warforge.ui.war_room"), TEXT_PRIMARY);

        if (!hasFaction) {
            subtitle(panel, text(tr("warforge.ui.you_are_not_currently_in_a_faction"), TEXT_SECONDARY));
            show(root, mc);
            return;
        }

        subtitle(panel, text(data.factionName, data.factionColor));

        UIElement siegeSection = section(panel, CONTENT_LEFT, SIEGE_Y, sectionWidth, siegeSectionHeight);
        UIElement fobSection = section(panel, CONTENT_LEFT, FOB_Y, sectionWidth, fobSectionHeight);

        Label siegeTitle = fit(text(tr("warforge.ui.active_sieges"), TEXT_MUTED));
        siegeTitle.layout(l -> l.marginBottom(4));
        siegeSection.addChild(siegeTitle);
        ScrollerView siegeList = insetList(sectionWidth - 10, siegeSectionHeight - 28);
        if (data.sieges.isEmpty()) {
            siegeList.addScrollViewChild(emptyLine(tr("warforge.ui.your_faction_is_not_involved_in_any_siege")));
        } else {
            for (OperationsGuiData.SiegeEntry siege : data.sieges) {
                siegeList.addScrollViewChild(siegeRow(siege));
            }
        }
        siegeSection.addChild(siegeList);

        Label fobTitle = fit(text(tr("warforge.ui.forward_operating_bases"), TEXT_MUTED));
        fobTitle.layout(l -> l.marginBottom(4));
        fobSection.addChild(fobTitle);
        String fobInfo = tr("warforge.ui.warp_is_available_only_while_your_faction_is_und");
        Label fobDescription = wrap(text(fobInfo, TEXT_MUTED), sectionWidth);
        fobDescription.layout(l -> l.marginBottom(6));
        fobSection.addChild(fobDescription);
        ScrollerView fobList = insetList(sectionWidth - 10, fobSectionHeight - 46);
        if (data.fobs.isEmpty()) {
            fobList.addScrollViewChild(emptyLine(tr("warforge.ui.no_fobs_established_yet")));
        } else {
            for (OperationsGuiData.FobEntry fob : data.fobs) {
                fobList.addScrollViewChild(fobRow(fob, data.warpTicketCost));
            }
        }
        fobSection.addChild(fobList);

        show(root, mc);
    }

    private static void show(UIElement root, Minecraft mc) {
        ModularUIScreen built = new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal(tr("warforge.ui.operations")));
        REVISION.shown(built);
        ClientCompat.setScreen(built);
    }

    private static UIElement emptyLine(String message) {
        UIElement line = box(ROW_WIDTH, 21);
        place(line, fit(text(message, TEXT_PRIMARY)), 6, 6);
        return line;
    }

    private static UIElement siegeRow(OperationsGuiData.SiegeEntry siege) {
        UIElement row = listRow(ROW_WIDTH, 24);
        row.addChild(width(text(siege.attacking ? tr("warforge.ui.attacking") : tr("warforge.ui.defending"),
                siege.attacking ? UiText.STATUS_ORANGE : TEXT_SUCCESS), 66));
        String versus = tr("warforge.ui.s_vs_s", siege.attackerName, siege.defenderName);
        Label versusLabel = scrollingText(literal(versus, TEXT_PRIMARY, false), 158);
        versusLabel.layout(l -> l.marginHorizontal(5));
        row.addChild(tooltip(versusLabel, versus));
        row.addChild(width(text("[" + siege.chunkX + ", " + siege.chunkZ + "]", TEXT_SECONDARY), 70));
        return row;
    }

    private static UIElement fobRow(OperationsGuiData.FobEntry fob, int warpTicketCost) {
        UIElement row = listRow(ROW_WIDTH, 24);
        int color = UiText.fobStatusColor(fob);
        Label name = scrollingText(literal(fob.name.isEmpty() ? tr("warforge.ui.fob") : fob.name, color, false), 150);
        name.layout(l -> l.marginHorizontal(5));
        row.addChild(tooltip(name, fob.occupied ? tr("warforge.ui.occupied_by_enemies") : fob.pos.toFancyString()));
        row.addChild(width(text(fob.tickets + " / " + fob.maxTickets, color), 58));

        DimBlockPos pos = fob.pos;
        Button warp = actionButton(tr("warforge.ui.warp"), 52, fob.canWarp, () -> {
            PacketRequestFobWarp packet = new PacketRequestFobWarp();
            packet.pos = pos;
            WarForgeMod.NETWORK.sendToServer(packet);
        });
        row.addChild(tooltip(warp, (warpTicketCost == 1 ? tr("warforge.ui.warp_cost_d_ticket", warpTicketCost) : tr("warforge.ui.warp_cost_d_tickets", warpTicketCost))));
        return row;
    }
}
