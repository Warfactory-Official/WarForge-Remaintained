package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.ScrollArea;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.util.ItemStackElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketFactionInsuranceAction;
import com.flansmod.warforge.common.network.PacketInsuranceData;
import com.flansmod.warforge.server.Faction;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

/**
 * The insurance stash (layout of the 1.20.1 GuiFactionInsurance). This branch shows the stash read-only on the
 * client (no container, so no deposit slots and no inventory box); the leader's red x per slot voids it.
 */
@OnlyIn(Dist.CLIENT)
public final class InsuranceScreen {
    private static final int WIDTH = 356;
    private static final int HEIGHT = 180;
    private static final int BODY_Y = 54;
    private static final int SECTION_WIDTH = WIDTH - CONTENT_LEFT * 2;
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

        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, WIDTH, HEIGHT);
        panel.addWidget(WarForgeUiTheme.header(WIDTH));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, BODY_Y, SECTION_WIDTH, 118));
        panel.addWidget(WarForgeUiTheme.stripe(hasFaction ? data.factionColor : WarForgeUiTheme.STRIPE_DEFAULT, HEIGHT));
        panel.addWidget(WarForgeUiTheme.closeButton(WIDTH, click -> ClientScreens.close()));
        panel.addWidget(WarForgeUiTheme.title("Insurance Stash"));
        panel.addWidget(hasFaction
                ? WarForgeUiTheme.subtitle(data.factionName, data.factionColor, true)
                : WarForgeUiTheme.subtitle("No faction selected", WarForgeUiTheme.TEXT_SECONDARY, true));

        int textX = CONTENT_LEFT + 5;
        if (!hasFaction) {
            panel.addWidget(Label.text("No insurance stash is available.", WarForgeUiTheme.TEXT_NOTE).at(textX, BODY_Y + 5 + 6));
            ClientScreens.open(panel);
            return;
        }

        boolean canVoid = data.canWithdraw;
        int y = BODY_Y + 5;
        panel.addWidget(Label.bold("Protected Reserve", WarForgeUiTheme.TEXT_PRIMARY).at(textX, y));
        y += 9 + 4;
        panel.addWidget(Label.text("Stored items cannot be withdrawn.", WarForgeUiTheme.TEXT_MUTED)
                .clip(SECTION_WIDTH - 10).at(textX, y));
        y += 9 + 2;
        panel.addWidget(Label.text(canVoid
                        ? "Leader actions: use the red X to permanently void a slot."
                        : "Only the leader can void stored items to free space.",
                canVoid ? 0xFFCC88 : WarForgeUiTheme.TEXT_MUTED).clip(SECTION_WIDTH - 10).at(textX, y));
        y += 9 + 4;

        int listWidth = SECTION_WIDTH - 10;
        int columns = Math.max(1, listWidth / CELL_SIZE);
        List<ItemStack> stacks = data.stacks;
        ScrollArea list = new ScrollArea(textX, y, listWidth, LIST_HEIGHT);
        for (int slot = 0; slot < stacks.size(); slot++) {
            int cellX = (slot % columns) * CELL_SIZE;
            int cellY = (slot / columns) * CELL_SIZE;
            ItemStack stack = stacks.get(slot);
            list.addAt(new ItemStackElement(stack, 0, 0, CELL_SIZE, true, true), cellX, cellY);
            if (canVoid && !stack.isEmpty()) {
                int captured = slot;
                ButtonWidget voidButton = WarForgeUiTheme.smallDangerButton("x", 8, 8, click -> {
                    PacketFactionInsuranceAction packet = new PacketFactionInsuranceAction();
                    packet.slot = captured;
                    WarForgeMod.NETWORK.sendToServer(packet);
                });
                voidButton.setHoverTooltips("Click to void the slot", "Warning, this cannot be undone");
                list.addAt(voidButton, cellX + 10, cellY);
            }
        }
        panel.addWidget(list.finish());

        ClientScreens.open(panel);
    }
}
