package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.DataRevision;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRequestUpgrade;
import com.flansmod.warforge.common.network.PacketUpgradeUI;
import com.flansmod.warforge.common.item.ItemMatcher;
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
import java.util.List;
import java.util.Map;
import com.flansmod.warforge.compat.Inventories;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

@OnlyIn(Dist.CLIENT)
public final class FactionUpgradeScreen {
    private static final DataRevision REVISION = new DataRevision();
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
        if (data != null && REVISION.unchanged(ClientCompat.screen(), data)) {
            return;
        }

        if (mc.player == null || data == null || data.factionId.equals(FactionIds.NONE)) {
            return;
        }

        int color = data.color & 0xFFFFFF;
        java.util.Map<ItemMatcher, Integer> requirements = com.flansmod.warforge.client.ClientUpgradeTable.getRequirementsFor(data.level + 1);
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

        title(panel, tr("warforge.ui.citadel_upgrade"), TEXT_PRIMARY);
        subtitle(panel, shadowText(data.factionName, color, false));

        Label prompt = fit(text(tr("warforge.ui.requirements_to_advance_from_level_d_to_level_d", data.level, data.level + 1), TEXT_MUTED));
        prompt.layout(l -> l.marginBottom(6));
        body.addChild(prompt);

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
        actionRow.addChild(actionButton(tr("warforge.ui.close"), closeWidth, () -> ClientCompat.setScreen(null)));
        actionRow.addChild(box(contentWidth - closeWidth - upgradeWidth, BUTTON_HEIGHT));
        Button upgradeButton = actionButton(tr("warforge.ui.upgrade"), upgradeWidth, data.outrankingOfficer && allMet, () -> {
            PacketRequestUpgrade packet = new PacketRequestUpgrade();
            packet.factionID = data.factionId;
            WarForgeMod.NETWORK.sendToServer(packet);
        });
        if (!data.outrankingOfficer || !allMet) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(tr("warforge.ui.you_don_t_meet_following_requirements"))
                    .withStyle(Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(TEXT_BAD))));
            if (!data.outrankingOfficer) {
                lines.add(Component.literal(tr("warforge.ui.you_need_to_be_officer_of_or_higher")));
            }
            if (!allMet) {
                lines.add(Component.literal(tr("warforge.ui.you_don_t_have_the_materials_required")));
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
        Label amount = shadowText(has + "/" + count, has >= count ? 0x55FF55 : TEXT_BAD, false);
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

    private static UIElement outcomePanel(int level) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER)
                .height(OUTCOME_HEIGHT).flexShrink(0));

        Label heading = fit(text(tr("warforge.ui.upgrade_outcome"), TEXT_PRIMARY));
        heading.layout(l -> l.marginBottom(1));
        panel.addChild(heading);
        Label levels = fit(shadowText(tr("warforge.ui.level_d_d", level, level + 1), TEXT_WARNING, false));
        levels.layout(l -> l.marginBottom(2));
        panel.addChild(levels);

        UIElement deltaRow = new UIElement();
        deltaRow.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).height(20).marginTop(2));
        UIElement claims = deltaCard(tr("warforge.ui.claims"),
                String.valueOf(com.flansmod.warforge.client.ClientUpgradeTable.getClaimLimitForLevel(level)),
                String.valueOf(com.flansmod.warforge.client.ClientUpgradeTable.getClaimLimitForLevel(level + 1)),
                TEXT_BAD, 0x55FF55);
        claims.layout(l -> l.marginRight(1));
        UIElement insurance = deltaCard(tr("warforge.ui.insurance"),
                String.valueOf(com.flansmod.warforge.client.ClientUpgradeTable.getInsuranceSlotsForLevel(level)),
                String.valueOf(com.flansmod.warforge.client.ClientUpgradeTable.getInsuranceSlotsForLevel(level + 1)),
                UPGRADE_BEFORE, UPGRADE_AFTER);
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

        private static void openMaxLevel(Minecraft mc, PacketUpgradeUI data, int color) {
        int width = 220;
        int height = 86;
        UIElement root = screenRoot();
        UIElement panel = panel(width, height);
        root.addChild(panel);
        place(panel, box(width, height, headerBackdrop()), 0, 0, width, height);
        place(panel, box(6, height, fill((0xFF000000 | UPGRADE_STRIPE))), 0, 0, 6, height);
        place(panel, shadowText(tr("warforge.ui.your_citadel_is_at_its_max_level"), 0xFFFFFF, false), 16, 16, width - 24, 16);
        place(panel, text(tr("warforge.ui.s_level_d", data.factionName, data.level), color), 16, 30, width - 24, 9);
        place(panel, actionButton(tr("warforge.ui.close"), 96, () -> ClientCompat.setScreen(null)), 16, 52, 96, BUTTON_HEIGHT);
        show(root, mc);
    }

    private static void show(UIElement root, Minecraft mc) {
        ModularUIScreen built = new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal(tr("warforge.ui.citadel_upgrade")));
        REVISION.shown(built);
        ClientCompat.setScreen(built);
    }
}
