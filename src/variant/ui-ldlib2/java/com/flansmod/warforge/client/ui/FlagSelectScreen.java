package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.ui.model.UiText;
import com.flansmod.warforge.client.ClientFlagRegistry;
import com.flansmod.warforge.client.util.FlagElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketChooseFactionFlag;
import com.flansmod.warforge.server.Faction;
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

import java.util.List;
import java.util.UUID;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

/** Faction flag choice, laid out like GuiFactionFlagSelect of the 1.20.1 build. */
@OnlyIn(Dist.CLIENT)
public final class FlagSelectScreen {
    private static final int WIDTH = 332;
    private static final int HEIGHT = 248;

    private FlagSelectScreen() {
    }

    public static void open(UUID factionId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
        String factionName = faction != null ? faction.name : "";
        int factionColour = faction != null ? faction.colour : 0xFFFFFF;
        String currentFlagId = faction != null ? faction.flagId : "";
        boolean canChoose = currentFlagId.isEmpty();

        List<String> availableFlags = ClientFlagRegistry.getAvailableFlags();

        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int bodyHeight = HEIGHT - BODY_Y - 12;

        UIElement root = screenRoot(0);
        UIElement panel = panel(WIDTH, HEIGHT);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        UIElement body = section(panel, CONTENT_LEFT, BODY_Y, sectionWidth, bodyHeight);
        colorStripe(panel, factionColour, HEIGHT);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, "Faction Flag", TEXT_PRIMARY, WIDTH - CONTENT_LEFT * 2 - 18);
        if (!factionName.isEmpty()) {
            subtitle(panel, boldText(factionName, factionColour), WIDTH - CONTENT_LEFT * 2 - 18);
        }

        String statusText = canChoose
                ? "Choose once. This cannot be changed later."
                : "Flag locked: " + UiText.flagDisplayName(currentFlagId);
        Label status = width(text(statusText, TEXT_SECONDARY), sectionWidth - 10);
        status.layout(l -> l.marginBottom(6));
        body.addChild(status);

        ScrollerView list = insetList(sectionWidth - 10, bodyHeight - 30);
        for (String flagId : availableFlags) {
            list.addScrollViewChild(buildRow(flagId, factionId, currentFlagId, canChoose));
        }
        body.addChild(list);

        ClientCompat.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal("Faction Flag")));
    }

    private static UIElement buildRow(String flagId, UUID factionId, String currentFlagId, boolean canChoose) {
        UIElement row = listRow(WIDTH - 40, 28);
        row.layout(l -> l.paddingVertical(2));

        FlagElement flag = new FlagElement(flagId);
        flag.layout(l -> l.width(42).height(24).flexShrink(0));
        row.addChild(flag);

        Label name = scrollingText(literal(UiText.flagDisplayName(flagId), TEXT_PRIMARY, false), 160);
        name.layout(l -> l.marginLeft(4).marginRight(4));
        row.addChild(tooltip(name, flagId));

        Button button;
        if (flagId.equals(currentFlagId)) {
            button = tabButton("Selected", 74, true, null);
        } else {
            button = actionButton("Choose", 74, canChoose, () -> {
                PacketChooseFactionFlag packet = new PacketChooseFactionFlag();
                packet.factionId = factionId;
                packet.flagId = flagId;
                WarForgeMod.NETWORK.sendToServer(packet);
            });
        }
        row.addChild(button);
        return row;
    }
}
