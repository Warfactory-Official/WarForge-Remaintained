package com.flansmod.warforge.client.ui;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.client.util.ColorPickerElement;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketCreateFaction;
import com.flansmod.warforge.common.network.PacketOpenCreateFaction;
import com.flansmod.warforge.common.network.PacketSetFactionColour;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import static com.flansmod.warforge.api.modularui.WarForgeUiTheme.*;

/** Create faction / set faction colour, laid out like GuiCreateFactionModular of the 1.20.1 build. */
@OnlyIn(Dist.CLIENT)
public final class CreateFactionScreen {
    private static final int WIDTH = 264;
    private static final int BODY_Y = 46;
    private static final int BAR_WIDTH = 156;
    private static final int BAR_HEIGHT = 12;
    private static final int BAR_GAP = 3;
    private static final int INNER_PAD = 8;

    private CreateFactionScreen() {
    }

    public static void open() {
        build(false, 0xFFFFFF);
    }

    public static void openRecolour(int colour) {
        build(true, colour);
    }

    private static void build(boolean recolour, int colour) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int height = recolour ? 150 : 190;
        int sectionWidth = WIDTH - CONTENT_LEFT * 2;
        int innerLeft = CONTENT_LEFT + INNER_PAD;
        int innerWidth = sectionWidth - INNER_PAD * 2;

        UIElement root = screenRoot();
        UIElement panel = panel(WIDTH, height);
        root.addChild(panel);

        headerBackdrop(panel, WIDTH);
        place(panel, box(sectionWidth, height - BODY_Y - 10, sectionBackdrop()), CONTENT_LEFT, BODY_Y, sectionWidth, height - BODY_Y - 10);
        closeButton(panel, WIDTH).setOnClick(event -> ClientCompat.setScreen(null));

        String titleText = recolour ? "Set Faction Colour" : "Create Faction";
        title(panel, titleText, TEXT_PRIMARY);

        TextField nameField = null;
        int colourLabelY = BODY_Y + 8;
        if (!recolour) {
            place(panel, text("Faction Name", TEXT_SECONDARY), innerLeft, BODY_Y + 8, innerWidth, 9);
            TextField field = new TextField().setText("").setAnyString();
            field.style(s -> s.background(insetBackdrop()));
            place(panel, field, innerLeft, BODY_Y + 18, innerWidth, 16);
            nameField = field;
            colourLabelY = BODY_Y + 42;
        }

        place(panel, text("Colour", TEXT_SECONDARY), innerLeft, colourLabelY, innerWidth, 9);

        int barsY = colourLabelY + 12;
        int swatchX = innerLeft + BAR_WIDTH + 12;
        int swatchWidth = WIDTH - swatchX - CONTENT_LEFT - INNER_PAD;
        ColorPickerElement picker = new ColorPickerElement(colour, swatchWidth, 12);
        place(panel, picker, innerLeft, barsY, BAR_WIDTH + 12 + swatchWidth, BAR_HEIGHT * 3 + BAR_GAP * 2);

        int btnY = height - 36;
        TextField finalNameField = nameField;
        place(panel, actionButton(recolour ? "Set Colour" : "Create", 74, () -> {
            if (recolour) {
                PacketSetFactionColour packet = new PacketSetFactionColour();
                packet.mColour = picker.getRGB();
                WarForgeMod.NETWORK.sendToServer(packet);
            } else {
                String name = finalNameField.getValue().trim();
                if (name.isEmpty()) {
                    return;
                }
                PacketCreateFaction packet = new PacketCreateFaction();
                packet.mCitadelPos = PacketOpenCreateFaction.pendingCitadelPos;
                packet.mFactionName = name;
                packet.mColour = picker.getRGB();
                WarForgeMod.NETWORK.sendToServer(packet);
            }
            ClientCompat.setScreen(null);
        }), innerLeft, btnY, 74, BUTTON_HEIGHT);
        place(panel, actionButton("Cancel", 74, () -> ClientCompat.setScreen(null)), innerLeft + 82, btnY, 74, BUTTON_HEIGHT);

        ClientCompat.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root), mc.player), Component.literal(titleText)));
    }
}
