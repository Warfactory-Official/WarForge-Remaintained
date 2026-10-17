package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.ui.model.UiText;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.factories.OperationsGuiData;
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

/** The war room (sieges and FOBs), laid out like GuiOperations of the 1.20.1 build. */
@OnlyIn(Dist.CLIENT)
public final class OperationsScreen {
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
        title(panel, "War Room", TEXT_PRIMARY);

        if (!hasFaction) {
            subtitle(panel, text("You are not currently in a faction", TEXT_SECONDARY));
            show(root, mc);
            return;
        }

        subtitle(panel, text(data.factionName, data.factionColor));

        UIElement siegeSection = section(panel, CONTENT_LEFT, SIEGE_Y, sectionWidth, siegeSectionHeight);
        UIElement fobSection = section(panel, CONTENT_LEFT, FOB_Y, sectionWidth, fobSectionHeight);

        Label siegeTitle = fit(text("Active Sieges", TEXT_MUTED));
        siegeTitle.layout(l -> l.marginBottom(4));
        siegeSection.addChild(siegeTitle);
        ScrollerView siegeList = insetList(sectionWidth - 10, siegeSectionHeight - 28);
        if (data.sieges.isEmpty()) {
            siegeList.addScrollViewChild(emptyLine("Your faction is not involved in any siege."));
        } else {
            for (OperationsGuiData.SiegeEntry siege : data.sieges) {
                siegeList.addScrollViewChild(siegeRow(siege));
            }
        }
        siegeSection.addChild(siegeList);

        Label fobTitle = fit(text("Forward Operating Bases", TEXT_MUTED));
        fobTitle.layout(l -> l.marginBottom(4));
        fobSection.addChild(fobTitle);
        String fobInfo = "Warp is available only while your faction is under siege. Tickets regenerate each siege cycle.";
        Label fobDescription = wrap(text(fobInfo, TEXT_MUTED), sectionWidth);
        fobDescription.layout(l -> l.marginBottom(6));
        fobSection.addChild(fobDescription);
        ScrollerView fobList = insetList(sectionWidth - 10, fobSectionHeight - 46);
        if (data.fobs.isEmpty()) {
            fobList.addScrollViewChild(emptyLine("No FOBs established yet."));
        } else {
            for (OperationsGuiData.FobEntry fob : data.fobs) {
                fobList.addScrollViewChild(fobRow(fob, data.warpTicketCost));
            }
        }
        fobSection.addChild(fobList);

        show(root, mc);
    }

    private static void show(UIElement root, Minecraft mc) {
        ClientCompat.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal("Operations")));
    }

    private static UIElement emptyLine(String message) {
        UIElement line = box(ROW_WIDTH, 21);
        place(line, fit(text(message, TEXT_PRIMARY)), 6, 6);
        return line;
    }

    private static UIElement siegeRow(OperationsGuiData.SiegeEntry siege) {
        UIElement row = listRow(ROW_WIDTH, 24);
        row.addChild(width(text(siege.attacking ? "Attacking" : "Defending",
                siege.attacking ? UiText.STATUS_ORANGE : TEXT_SUCCESS), 66));
        String versus = siege.attackerName + " vs " + siege.defenderName;
        Label versusLabel = scrollingText(literal(versus, TEXT_PRIMARY, false), 158);
        versusLabel.layout(l -> l.marginHorizontal(5));
        row.addChild(tooltip(versusLabel, versus));
        row.addChild(width(text("[" + siege.chunkX + ", " + siege.chunkZ + "]", TEXT_SECONDARY), 70));
        return row;
    }

    private static UIElement fobRow(OperationsGuiData.FobEntry fob, int warpTicketCost) {
        UIElement row = listRow(ROW_WIDTH, 24);
        int color = UiText.fobStatusColor(fob);
        Label name = scrollingText(literal(fob.name.isEmpty() ? "FOB" : fob.name, color, false), 150);
        name.layout(l -> l.marginHorizontal(5));
        row.addChild(tooltip(name, fob.occupied ? "Occupied by enemies" : fob.pos.toFancyString()));
        row.addChild(width(text(fob.tickets + " / " + fob.maxTickets, color), 58));

        DimBlockPos pos = fob.pos;
        Button warp = actionButton("Warp", 52, fob.canWarp, () -> {
            PacketRequestFobWarp packet = new PacketRequestFobWarp();
            packet.mPos = pos;
            WarForgeMod.NETWORK.sendToServer(packet);
        });
        row.addChild(tooltip(warp, "Warp cost: " + warpTicketCost + (warpTicketCost == 1 ? " ticket" : " tickets")));
        return row;
    }
}
