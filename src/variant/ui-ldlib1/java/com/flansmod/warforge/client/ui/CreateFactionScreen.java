package com.flansmod.warforge.client.ui;

import static com.flansmod.warforge.common.ui.UiLang.tr;
import com.flansmod.warforge.api.modularui.Label;
import com.flansmod.warforge.api.modularui.WarForgeUiTheme;
import com.flansmod.warforge.client.util.ColorPickerElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketCreateFaction;
import com.flansmod.warforge.common.network.PacketOpenCreateFaction;
import com.flansmod.warforge.common.network.PacketSetFactionColour;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.CONTENT_LEFT;

@OnlyIn(Dist.CLIENT)
public final class CreateFactionScreen {
    private static final int WIDTH = 264;
    private static final int BODY_Y = 46;
    private static final int BAR_WIDTH = 156;
    private static final int INNER_PAD = 8;
    private static final int INNER_LEFT = CONTENT_LEFT + INNER_PAD;
    private static final int INNER_WIDTH = WIDTH - CONTENT_LEFT * 2 - INNER_PAD * 2;
    private static final int SWATCH_GAP = 12;

    private CreateFactionScreen() {
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int height = 190;
        WidgetGroup panel = frame(tr("warforge.ui.create_faction"), height);

        panel.addWidget(Label.text(tr("warforge.ui.faction_name"), WarForgeUiTheme.TEXT_SECONDARY).at(INNER_LEFT, BODY_Y + 8));
        TextFieldWidget nameField = new TextFieldWidget(INNER_LEFT, BODY_Y + 18, INNER_WIDTH, 16, null, null);
        nameField.setMaxStringLength(64);
        nameField.setBordered(false);
        nameField.setBackground(WarForgeUiTheme.insetBackdrop());
        nameField.setCurrentString("");
        panel.addWidget(nameField);

        ColorPickerElement picker = picker(panel, 0xFFFFFF, BODY_Y + 42);

        int buttonY = height - 36;
        panel.addWidget(button(tr("warforge.ui.create"), INNER_LEFT, buttonY, () -> {
            String name = nameField.getRawCurrentString().trim();
            if (name.isEmpty()) {
                return;
            }
            PacketCreateFaction packet = new PacketCreateFaction();
            packet.citadelPos = PacketOpenCreateFaction.pendingCitadelPos;
            packet.factionName = name;
            packet.colour = picker.getRGB();
            WarForgeMod.NETWORK.sendToServer(packet);
            ClientScreens.close();
        }));
        panel.addWidget(button(tr("warforge.ui.cancel"), INNER_LEFT + 82, buttonY, ClientScreens::close));

        ClientScreens.open(panel);
    }

    public static void openRecolour(int colour) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int height = 150;
        WidgetGroup panel = frame(tr("warforge.ui.set_faction_colour"), height);

        ColorPickerElement picker = picker(panel, colour, BODY_Y + 8);

        int buttonY = height - 36;
        panel.addWidget(button(tr("warforge.ui.set_colour"), INNER_LEFT, buttonY, () -> {
            PacketSetFactionColour packet = new PacketSetFactionColour();
            packet.colour = picker.getRGB();
            WarForgeMod.NETWORK.sendToServer(packet);
            ClientScreens.close();
        }));
        panel.addWidget(button(tr("warforge.ui.cancel"), INNER_LEFT + 82, buttonY, ClientScreens::close));

        ClientScreens.open(panel);
    }

    private static WidgetGroup frame(String title, int height) {
        WidgetGroup panel = WarForgeUiTheme.panel(0, 0, WIDTH, height);
        panel.addWidget(WarForgeUiTheme.header(WIDTH));
        panel.addWidget(WarForgeUiTheme.section(CONTENT_LEFT, BODY_Y, WIDTH - CONTENT_LEFT * 2, height - BODY_Y - 10));
        panel.addWidget(WarForgeUiTheme.closeButton(WIDTH, click -> ClientScreens.close()));
        panel.addWidget(WarForgeUiTheme.title(title));
        return panel;
    }

    private static ColorPickerElement picker(WidgetGroup panel, int colour, int labelY) {
        panel.addWidget(Label.text(tr("warforge.ui.colour"), WarForgeUiTheme.TEXT_SECONDARY).at(INNER_LEFT, labelY));
        int swatchX = INNER_LEFT + BAR_WIDTH + SWATCH_GAP;
        int swatchWidth = WIDTH - swatchX - CONTENT_LEFT - INNER_PAD;
        ColorPickerElement picker = new ColorPickerElement(colour, INNER_LEFT, labelY + 12, BAR_WIDTH, SWATCH_GAP, swatchWidth);
        panel.addWidget(picker);
        return picker;
    }

    private static ButtonWidget button(String label, int x, int y, Runnable action) {
        return WarForgeUiTheme.at(WarForgeUiTheme.actionButton(label, 74, click -> action.run()), x, y);
    }
}
