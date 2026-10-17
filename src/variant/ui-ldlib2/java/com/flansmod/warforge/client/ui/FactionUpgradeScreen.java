package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRequestUpgrade;
import com.flansmod.warforge.common.network.PacketUpgradeUI;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.ItemMatcher;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.flansmod.warforge.compat.Inventories;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

/** Citadel upgrade, laid out like GUIUpgradePanel of the 1.20.1 build. */
@OnlyIn(Dist.CLIENT)
public final class FactionUpgradeScreen {
    private static final int WIDTH = 398;
    private static final int HEIGHT = 350;
    private static final int ACTIONS_Y = HEIGHT - 40;
    private static final int BODY_SECTION_HEIGHT = ACTIONS_Y - BODY_Y - 10;
    private static final int OUTCOME_HEIGHT = 54;

    private FactionUpgradeScreen() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        PacketUpgradeUI data = PacketUpgradeUI.latest;
        if (mc.player == null || data == null || data.mFactionID.equals(Faction.nullUuid)) {
            return;
        }

        int color = data.color & 0xFFFFFF;
        // Requirements to reach the next level (the server checks getRequirementsFor(citadelLevel + 1));
        // none means the citadel is at its max level (as on 1.20.1).
        HashMap<ItemMatcher, Integer> requirements = WarForgeMod.UPGRADE_HANDLER.getRequirementsFor(data.level + 1);
        if (requirements == null) {
            openMaxLevel(mc, data, color);
            return;
        }

        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int contentWidth = sectionWidth - 10;

        UIElement root = screenRoot();
        UIElement panel = panel(WIDTH, HEIGHT);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        UIElement body = section(panel, CONTENT_LEFT, BODY_Y, sectionWidth, BODY_SECTION_HEIGHT);
        UIElement actionSection = section(panel, CONTENT_LEFT, ACTIONS_Y, sectionWidth, 28);
        colorStripe(panel, color, HEIGHT);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, "Citadel Upgrade", TEXT_PRIMARY);
        subtitle(panel, shadowText(data.mFactionName, color, false));

        Label prompt = fit(text("Requirements to advance from level " + data.level + " to level " + (data.level + 1), TEXT_MUTED));
        prompt.layout(l -> l.marginBottom(6));
        body.addChild(prompt);

        // Requirements, fewest first (as on 1.20.1)
        List<Map.Entry<ItemMatcher, Integer>> sorted = new ArrayList<>(requirements.entrySet());
        sorted.sort(Comparator.comparingInt(Map.Entry::getValue));

        List<ItemStack> inventory = Inventories.main(mc.player.getInventory());
        boolean allMet = true;
        ScrollerView list = insetList(contentWidth, BODY_SECTION_HEIGHT - OUTCOME_HEIGHT - 25);
        list.layout(l -> l.marginBottom(5));
        for (Map.Entry<ItemMatcher, Integer> entry : sorted) {
            ItemMatcher matcher = entry.getKey();
            int required = entry.getValue();
            ItemStack stack = matcher.toStack();
            String name = stack.getHoverName().getString();
            int has = inventory.stream()
                    .filter(s -> !s.isEmpty() && matcher.matches(s))
                    .mapToInt(ItemStack::getCount)
                    .sum();
            if (has < required) {
                allMet = false;
            }
            list.addScrollViewChild(requirementRow(stack, name, required, has, contentWidth - 10));
        }
        body.addChild(list);

        UIElement outcome = outcomePanel(data.level);
        outcome.layout(l -> l.marginLeft(2).marginRight(2).marginTop(2));
        body.addChild(outcome);

        int closeWidth = 94;
        int upgradeWidth = 104;
        UIElement actionRow = row(contentWidth, BUTTON_HEIGHT);
        actionRow.addChild(actionButton("Close", closeWidth, () -> ClientCompat.setScreen(null)));
        actionRow.addChild(box(contentWidth - closeWidth - upgradeWidth, BUTTON_HEIGHT));
        Button upgradeButton = actionButton("Upgrade", upgradeWidth, data.outrankingOfficer && allMet, () -> {
            PacketRequestUpgrade packet = new PacketRequestUpgrade();
            packet.factionID = data.mFactionID;
            WarForgeMod.NETWORK.sendToServer(packet);
        });
        if (!data.outrankingOfficer || !allMet) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("You don't meet following requirements:")
                    .withStyle(Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(0xFF5555))));
            if (!data.outrankingOfficer) {
                lines.add(Component.literal("- You need to be officer of or higher!"));
            }
            if (!allMet) {
                lines.add(Component.literal("- You don't have the materials required!"));
            }
            tooltip(upgradeButton, lines.toArray(new Component[0]));
        }
        actionRow.addChild(upgradeButton);
        actionSection.addChild(actionRow);

        show(root, mc);
    }

    private static UIElement requirementRow(ItemStack stack, String displayName, int count, int has, int rowWidth) {
        UIElement row = row(rowWidth, 30);
        row.layout(l -> l.paddingHorizontal(5).marginAll(2));
        row.style(s -> s.background(insetBackdrop()));

        int nameLeft = 27;
        int countLeft = rowWidth - 78;
        int nameWidth = Math.max(110, countLeft - nameLeft - 8);

        row.addChild(itemIcon(stack, 20));
        Label name = shadowText(displayName, 0xFFFFFF, false);
        name.layout(l -> l.width(nameWidth).marginLeft(nameLeft - 5 - 20).flexShrink(0));
        row.addChild(name);
        Label amount = shadowText(has + "/" + count, has >= count ? 0x55FF55 : 0xFF5555, false);
        amount.textStyle(t -> t.fontSize(9f * 1.5f));
        amount.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(countLeft).top(8).width(50).height(14));
        row.addChild(amount);
        if (has >= count) {
            Label check = shadowText("✔", 0x55FF55, true);
            check.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(rowWidth - 20).top(10).width(10).height(10));
            row.addChild(check);
        }
        return row;
    }

    /** "Upgrade Outcome": claims and insurance slots before and after the upgrade (createUpgradeOutcomePanel of 1.20.1). */
    private static UIElement outcomePanel(int level) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER)
                .height(OUTCOME_HEIGHT).flexShrink(0));

        Label heading = fit(text("Upgrade Outcome", TEXT_PRIMARY));
        heading.layout(l -> l.marginBottom(1));
        panel.addChild(heading);
        Label levels = fit(shadowText("Level " + level + " -> " + (level + 1), TEXT_WARNING, false));
        levels.layout(l -> l.marginBottom(2));
        panel.addChild(levels);

        UIElement deltaRow = new UIElement();
        deltaRow.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).height(20).marginTop(2));
        UIElement claims = deltaCard("Claims",
                String.valueOf(WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(level)),
                String.valueOf(WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(level + 1)),
                0xFF5555, 0x55FF55);
        claims.layout(l -> l.marginRight(1));
        UIElement insurance = deltaCard("Insurance",
                String.valueOf(WarForgeMod.UPGRADE_HANDLER.getInsuranceSlotsForLevel(level)),
                String.valueOf(WarForgeMod.UPGRADE_HANDLER.getInsuranceSlotsForLevel(level + 1)),
                0x6CB6FF, 0x8BFFB0);
        insurance.layout(l -> l.marginLeft(1));
        deltaRow.addChild(claims);
        deltaRow.addChild(insurance);
        panel.addChild(deltaRow);
        return panel;
    }

    private static UIElement deltaCard(String label, String previous, String next, int previousColor, int nextColor) {
        UIElement card = new UIElement();
        card.layout(l -> l.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER));
        card.addChild(fit(text(label, TEXT_SECONDARY)));

        UIElement values = new UIElement();
        values.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).height(11));
        Label before = fit(shadowText(previous, previousColor, false));
        before.textStyle(t -> t.fontSize(TITLE_FONT_SIZE));
        before.layout(l -> l.height(11));
        Label arrow = fit(text("→", TEXT_SECONDARY));
        arrow.textStyle(t -> t.fontSize(8f));
        arrow.layout(l -> l.marginHorizontal(1).height(8));
        Label after = fit(shadowText(next, nextColor, false));
        after.textStyle(t -> t.fontSize(TITLE_FONT_SIZE));
        after.layout(l -> l.height(11));
        values.addChild(before);
        values.addChild(arrow);
        values.addChild(after);
        card.addChild(values);
        return card;
    }

    /** The small "max level" panel of 1.20.1. */
    private static void openMaxLevel(Minecraft mc, PacketUpgradeUI data, int color) {
        int width = 220;
        int height = 86;
        UIElement root = screenRoot();
        UIElement panel = panel(width, height);
        root.addChild(panel);
        place(panel, box(width, height, headerBackdrop()), 0, 0, width, height);
        place(panel, box(6, height, fill(0xFF9F7A34)), 0, 0, 6, height);
        place(panel, shadowText("Your citadel is at it's max level!", 0xFFFFFF, false), 16, 16, width - 24, 16);
        place(panel, text(data.mFactionName + " | Level " + data.level, color), 16, 30, width - 24, 9);
        place(panel, actionButton("Close", 96, () -> ClientCompat.setScreen(null)), 16, 52, 96, BUTTON_HEIGHT);
        show(root, mc);
    }

    private static void show(UIElement root, Minecraft mc) {
        ClientCompat.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal("Citadel Upgrade")));
    }
}
