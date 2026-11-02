package com.flansmod.warforge.client.ui;

import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.ui.model.UiText;
import com.flansmod.warforge.client.ClientFlagRegistry;
import com.flansmod.warforge.client.util.FlagElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketChooseFactionFlag;
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

@OnlyIn(Dist.CLIENT)
public final class FlagSelectScreen {
    private static final int WIDTH = 332;
    private static final int HEIGHT = 248;

    private FlagSelectScreen() {
    }

    public static void open(UUID factionId, String factionName, int factionColour, String currentFlagId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        boolean canChoose = currentFlagId.isEmpty();

        List<String> availableFlags = ClientFlagRegistry.getAvailableFlags();

        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int bodyHeight = HEIGHT - BODY_Y - 12;

        UIElement root = screenRoot();
        UIElement panel = panel(WIDTH, HEIGHT);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        UIElement body = section(panel, CONTENT_LEFT, BODY_Y, sectionWidth, bodyHeight);
        colorStripe(panel, factionColour, HEIGHT);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, tr("warforge.ui.faction_flag"), TEXT_PRIMARY);
        if (!factionName.isEmpty()) {
            subtitle(panel, text(factionName, factionColour));
        }

        String statusText = canChoose
                ? tr("warforge.ui.choose_once_this_cannot_be_changed_later")
                : tr("warforge.ui.flag_locked_s", UiText.flagDisplayName(currentFlagId));
        Label status = wrap(text(statusText, TEXT_SECONDARY), sectionWidth);
        status.layout(l -> l.marginBottom(6));
        body.addChild(status);

        ScrollerView list = insetList(sectionWidth - 10, bodyHeight - 30);
        for (String flagId : availableFlags) {
            list.addScrollViewChild(buildRow(flagId, factionId, currentFlagId, canChoose));
        }
        body.addChild(list);

        ClientCompat.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal(tr("warforge.ui.faction_flag"))));
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
            button = tabButton(tr("warforge.ui.selected"), 74, true, null);
        } else {
            button = actionButton(tr("warforge.ui.choose"), 74, canChoose, () -> {
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
