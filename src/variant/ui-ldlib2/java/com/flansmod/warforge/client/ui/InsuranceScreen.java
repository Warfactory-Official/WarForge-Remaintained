package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketFactionInsuranceAction;
import com.flansmod.warforge.common.network.PacketInsuranceData;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

/**
 * The faction insurance stash, laid out like GuiFactionInsurance of the 1.20.1 build: a grid of stash cells in an
 * inset list, with a small red X on each stored slot for players who may void it.
 */
@OnlyIn(Dist.CLIENT)
public final class InsuranceScreen {
    private static final int WIDTH = 356;
    private static final int HEIGHT = 180;
    private static final int CELL_SIZE = 18;
    private static final int LIST_HEIGHT = 56;

    private InsuranceScreen() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        PacketInsuranceData data = PacketInsuranceData.latest;
        if (mc.player == null || data == null) {
            return;
        }

        boolean hasFaction = data.hasFaction && !data.factionId.equals(Faction.nullUuid);
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;

        UIElement root = screenRoot(0);
        UIElement panel = panel(WIDTH, HEIGHT);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        UIElement body = section(panel, CONTENT_LEFT, BODY_Y, sectionWidth, 118);
        colorStripe(panel, hasFaction ? data.factionColor : NO_FACTION_STRIPE, HEIGHT);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));

        title(panel, "Insurance Stash", TEXT_PRIMARY, WIDTH - CONTENT_LEFT * 2 - 18);
        subtitle(panel, boldText(hasFaction ? data.factionName : "No faction selected",
                hasFaction ? data.factionColor : TEXT_SECONDARY), WIDTH - CONTENT_LEFT * 2 - 18);

        if (!hasFaction) {
            Label empty = width(text("No insurance stash is available.", TEXT_INFO), sectionWidth - 10);
            empty.layout(l -> l.marginVertical(6));
            body.addChild(empty);
            open(root, mc);
            return;
        }

        List<ItemStack> stacks = data.stacks;
        boolean canVoid = data.canWithdraw;

        Label heading = width(boldText("Protected Reserve", TEXT_PRIMARY), sectionWidth - 10);
        heading.layout(l -> l.marginBottom(4));
        body.addChild(heading);
        Label voidInfo = width(text(canVoid
                        ? "Leader actions: use the red X to permanently void a slot."
                        : "Only the leader can void stored items to free space.",
                canVoid ? 0xFFCC88 : TEXT_MUTED), sectionWidth - 10);
        voidInfo.layout(l -> l.marginBottom(4));
        body.addChild(voidInfo);

        int listWidth = sectionWidth - 10;
        int columns = Math.max(1, (listWidth - 6) / CELL_SIZE);
        ScrollerView list = insetList(listWidth, LIST_HEIGHT);
        if (stacks.isEmpty()) {
            UIElement emptyRow = row(listWidth, 18);
            emptyRow.layout(l -> l.paddingLeft(6));
            emptyRow.addChild(text("The stash is empty.", TEXT_MUTED));
            list.addScrollViewChild(emptyRow);
        }
        for (int first = 0; first < stacks.size(); first += columns) {
            UIElement row = row(listWidth, CELL_SIZE);
            for (int slot = first; slot < Math.min(stacks.size(), first + columns); slot++) {
                row.addChild(buildCell(stacks.get(slot), slot, canVoid));
            }
            list.addScrollViewChild(row);
        }
        body.addChild(list);

        open(root, mc);
    }

    private static void open(UIElement root, Minecraft mc) {
        ClientCompat.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal("Faction Stash")));
    }

    private static UIElement buildCell(ItemStack stack, int slotIndex, boolean canVoid) {
        UIElement cell = box(CELL_SIZE, CELL_SIZE, ItemSlot.ITEM_SLOT_TEXTURE);
        UIElement icon = place(cell, itemIcon(stack, 16), 1, 1, 16, 16);
        if (!stack.isEmpty()) {
            tooltip(icon, stack.getHoverName().getString() + " x" + stack.getCount());
        }
        if (canVoid && !stack.isEmpty()) {
            int captured = slotIndex;
            Button voidButton = dangerButton("x", 8, 8, () -> {
                PacketFactionInsuranceAction packet = new PacketFactionInsuranceAction();
                packet.slot = captured;
                WarForgeMod.NETWORK.sendToServer(packet);
            });
            tooltip(voidButton, "Click to void the slot", "Warning, this cannot be undone");
            place(cell, voidButton, 10, 0, 8, 8);
        }
        return cell;
    }
}
