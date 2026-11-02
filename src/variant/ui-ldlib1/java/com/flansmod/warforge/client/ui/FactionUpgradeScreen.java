package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.client.ui.model.DataRevision;
import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.ScrollArea;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.util.ItemStackElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRequestUpgrade;
import com.flansmod.warforge.common.network.PacketUpgradeUI;
import com.flansmod.warforge.compat.Inventories;
import com.flansmod.warforge.common.item.ItemMatcher;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

@OnlyIn(Dist.CLIENT)
public final class FactionUpgradeScreen {
    private static final DataRevision REVISION = new DataRevision();
    private static final int WIDTH = 398;
    private static final int HEIGHT = 350;
    private static final int BODY_Y = 54;
    private static final int ACTIONS_Y = HEIGHT - 40;
    private static final int BODY_SECTION_HEIGHT = ACTIONS_Y - BODY_Y - 10;
    private static final int CONTENT_WIDTH = WIDTH - CONTENT_LEFT * 2 - 10;
    private static final int OUTCOME_HEIGHT = 54;
    private static final int LIST_HEIGHT = BODY_SECTION_HEIGHT - OUTCOME_HEIGHT - 25;
    private static final int ROW_WIDTH = CONTENT_WIDTH - 10;
    private static final int ROW_HEIGHT = 30;

    private FactionUpgradeScreen() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        PacketUpgradeUI data = PacketUpgradeUI.latest;
        if (data != null && REVISION.unchanged(Minecraft.getInstance().screen, data)) {
            return;
        }

        if (mc.player == null || data == null || data.factionId.equals(FactionIds.NONE)) {
            return;
        }

        java.util.Map<ItemMatcher, Integer> requirements = com.flansmod.warforge.client.ClientUpgradeTable.getRequirementsFor(data.level + 1);
        if (requirements == null || requirements.isEmpty()) {
            openMaxLevel();
            return;
        }

        int color = data.color & 0xFFFFFF;
        int level = data.level;

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, WIDTH, HEIGHT);
        panel.addWidget(WarForgeUiTheme.header(WIDTH));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, BODY_Y, WIDTH - CONTENT_LEFT * 2, BODY_SECTION_HEIGHT));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, ACTIONS_Y, WIDTH - CONTENT_LEFT * 2, 28));
        panel.addWidget(WarForgeUiTheme.stripe(color, HEIGHT));
        panel.addWidget(WarForgeUiTheme.closeButton(WIDTH, click -> ClientScreens.close()));
        panel.addWidget(WarForgeUiTheme.title(tr("warforge.ui.citadel_upgrade")));
        panel.addWidget(Label.bold(data.factionName, color).shadow().at(CONTENT_LEFT, WarForgeUiTheme.SUBTITLE_Y));

        int textX = CONTENT_LEFT + 5;
        panel.addWidget(Label.text(tr("warforge.ui.requirements_to_advance_from_level_d_to_level_d", level, level + 1), WarForgeUiTheme.TEXT_MUTED)
                .clip(CONTENT_WIDTH).at(textX, BODY_Y + 5));

        List<Map.Entry<ItemMatcher, Integer>> sorted = new ArrayList<>(requirements.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        List<ItemStack> inventory = Inventories.main(mc.player.getInventory());
        boolean allMet = true;
        int listY = BODY_Y + 5 + 9 + 6;
        ScrollArea list = new ScrollArea(textX, listY, CONTENT_WIDTH, LIST_HEIGHT);
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
            list.addRow(requirementRow(stack, name, required, has), 2, 2, 2);
        }
        panel.addWidget(list.finish());

        addOutcome(panel, textX + 2, listY + LIST_HEIGHT + 5 + 2, level);

        int actionY = ACTIONS_Y + 5;
        panel.addWidget(WarForgeUiTheme.at(WarForgeUiTheme.actionButton(tr("warforge.ui.close"), 94, click -> ClientScreens.close()), textX, actionY));

        ButtonWidget upgrade = WarForgeUiTheme.actionButton(tr("warforge.ui.upgrade"), 104, data.outrankingOfficer && allMet, click -> {
            PacketRequestUpgrade packet = new PacketRequestUpgrade();
            packet.factionID = data.factionId;
            WarForgeMod.NETWORK.sendToServer(packet);
        });
        if (!data.outrankingOfficer || !allMet) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(tr("warforge.ui.you_don_t_meet_following_requirements")).withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
            if (!data.outrankingOfficer) {
                tooltip.add(Component.literal(tr("warforge.ui.you_need_to_be_officer_of_or_higher")));
            }
            if (!allMet) {
                tooltip.add(Component.literal(tr("warforge.ui.you_don_t_have_the_materials_required")));
            }
            upgrade.setHoverTooltips(tooltip);
        }
        panel.addWidget(WarForgeUiTheme.at(upgrade, textX + CONTENT_WIDTH - 104, actionY));

        REVISION.shown(ClientScreens.open(panel));
    }

    private static void openMaxLevel() {
        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, 220, 76);
        panel.addWidget(WarForgeUiTheme.box(0, 0, 220, 76, WarForgeUiTheme.headerBackdrop()));
        panel.addWidget(WarForgeUiTheme.stripe(WarForgeUiTheme.UPGRADE_STRIPE, 76));
        panel.addWidget(Label.text(tr("warforge.ui.your_citadel_is_at_its_max_level"), WarForgeUiTheme.TEXT_PRIMARY).shadow().box(200, 16).at(16, 16));
        panel.addWidget(WarForgeUiTheme.at(WarForgeUiTheme.actionButton(tr("warforge.ui.close"), 96, click -> ClientScreens.close()), 16, 42));
        REVISION.shown(ClientScreens.open(panel));
    }

    private static WidgetGroup requirementRow(ItemStack stack, String name, int required, int has) {
        WidgetGroup row = new WidgetGroup(0, 0, ROW_WIDTH, ROW_HEIGHT);
        row.setBackground(WarForgeUiTheme.insetBackdrop());
        row.addWidget(new ItemStackElement(stack, 5, 5, 20, false, false));
        int countLeft = ROW_WIDTH - 78;
        int nameWidth = Math.max(110, countLeft - 27 - 8);
        row.addWidget(Label.text(name, WarForgeUiTheme.TEXT_PRIMARY).shadow().box(nameWidth, ROW_HEIGHT).at(27, 0));
        boolean met = has >= required;
        row.addWidget(Label.text(has + "/" + required, met ? 0x55FF55 : WarForgeUiTheme.TEXT_BAD).shadow().scale(1.5f)
                .at(countLeft, (ROW_HEIGHT - 14) / 2));
        if (met) {
            row.addWidget(Label.bold("✔", 0x55FF55).shadow().scale(1.5f).at(ROW_WIDTH - 5 - 14, (ROW_HEIGHT - 14) / 2));
        }
        return row;
    }

    private static void addOutcome(WidgetGroup panel, int x, int y, int level) {
        panel.addWidget(Label.bold(tr("warforge.ui.upgrade_outcome"), WarForgeUiTheme.TEXT_PRIMARY).at(x, y));
        panel.addWidget(Label.bold(tr("warforge.ui.level_d_d", level, level + 1), WarForgeUiTheme.TEXT_WARNING).shadow().at(x, y + 10));
        int cardY = y + 10 + 9 + 2 + 2;
        int next = deltaCard(panel, x, cardY, tr("warforge.ui.claims"),
                String.valueOf(com.flansmod.warforge.client.ClientUpgradeTable.getClaimLimitForLevel(level)),
                String.valueOf(com.flansmod.warforge.client.ClientUpgradeTable.getClaimLimitForLevel(level + 1)),
                WarForgeUiTheme.TEXT_BAD, 0x55FF55);
        deltaCard(panel, next + 2, cardY, tr("warforge.ui.insurance"),
                String.valueOf(com.flansmod.warforge.client.ClientUpgradeTable.getInsuranceSlotsForLevel(level)),
                String.valueOf(com.flansmod.warforge.client.ClientUpgradeTable.getInsuranceSlotsForLevel(level + 1)),
                WarForgeUiTheme.UPGRADE_BEFORE, WarForgeUiTheme.UPGRADE_AFTER);
    }

    private static int deltaCard(WidgetGroup panel, int x, int y, String label, String previous, String next,
                                 int previousColor, int nextColor) {
        Label title = Label.text(label, WarForgeUiTheme.TEXT_SECONDARY).at(x, y);
        panel.addWidget(title);
        Label prev = Label.text(previous, previousColor).shadow().scale(1.15f).at(x, y + 9);
        panel.addWidget(prev);
        int arrowX = x + prev.getSizeWidth() + 1;
        Label arrow = Label.text("→", WarForgeUiTheme.TEXT_SECONDARY).at(arrowX, y + 10);
        panel.addWidget(arrow);
        Label nextLabel = Label.bold(next, nextColor).shadow().scale(1.15f).at(arrowX + arrow.getSizeWidth() + 1, y + 9);
        panel.addWidget(nextLabel);
        int end = nextLabel.getSelfPosition().x + nextLabel.getSizeWidth();
        return Math.max(x + title.getSizeWidth(), end) + 1;
    }
}
