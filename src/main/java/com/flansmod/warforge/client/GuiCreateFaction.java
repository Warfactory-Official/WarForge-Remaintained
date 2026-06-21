package com.flansmod.warforge.client;

import java.awt.Color;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.common.network.PacketCreateFaction;
import com.flansmod.warforge.common.network.PacketSetFactionColour;
import com.flansmod.warforge.common.util.DimBlockPos;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Vanilla screen for founding a faction (or recolouring an existing one). Replaces the 1.12.2
 * {@code GuiScreen} variant; the HSB picker is rendered with {@link GuiGraphics#fill} solid bars
 * instead of tinted texture blits.
 */
public class GuiCreateFaction extends Screen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "gui/citadelmenu.png");

    private final TileEntityCitadel citadel;
    private final boolean isRecolourGUI;
    private final int xSize = 176;
    private final int ySize = 56;
    private final float componentBarLength = 68.0f;
    private final float[] currentHSB = new float[3];

    private int guiLeft;
    private int guiTop;
    private EditBox inputField;

    public GuiCreateFaction(TileEntityCitadel tile, boolean isRecolour) {
        super(Component.literal(isRecolour ? "Set Faction Colour" : "Create Faction"));
        this.citadel = tile;
        Color.RGBtoHSB((tile.colour >> 16) & 0xff, (tile.colour >> 8) & 0xff, tile.colour & 0xff, this.currentHSB);
        this.isRecolourGUI = isRecolour;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        if (this.isRecolourGUI) {
            addRenderableWidget(Button.builder(Component.literal("Set Colour"), b -> sendRecolour())
                    .bounds(this.width / 2 - this.xSize / 2 + 6, this.height / 2 - 22, 80, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                    .bounds(this.width / 2 - this.xSize / 2 + 6, this.height / 2 + 2, 80, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Create"), b -> sendCreate())
                    .bounds(this.width / 2 - this.xSize / 2 + 6, this.height / 2 + 2, 40, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                    .bounds(this.width / 2 - this.xSize / 2 + 50, this.height / 2 + 2, 40, 20).build());

            this.inputField = new EditBox(this.font, this.width / 2 - this.xSize / 2 + 6, this.height / 2 - 12, 84, 20, Component.literal(""));
            this.inputField.setMaxLength(64);
            this.inputField.setBordered(false);
            this.inputField.setCanLoseFocus(false);
            this.inputField.setValue("");
            addRenderableWidget(this.inputField);
            setInitialFocus(this.inputField);
        }
    }

    private void sendCreate() {
        PacketCreateFaction packet = new PacketCreateFaction();
        packet.mCitadelPos = new DimBlockPos(this.citadel);
        packet.mFactionName = this.inputField.getValue();
        packet.mColour = Color.HSBtoRGB(this.currentHSB[0], this.currentHSB[1], this.currentHSB[2]);
        WarForgeMod.NETWORK.sendToServer(packet);
        onClose();
    }

    private void sendRecolour() {
        PacketSetFactionColour packet = new PacketSetFactionColour();
        packet.mColour = Color.HSBtoRGB(this.currentHSB[0], this.currentHSB[1], this.currentHSB[2]);
        WarForgeMod.NETWORK.sendToServer(packet);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);

        int j = this.guiLeft;
        int k = this.guiTop;
        graphics.blit(TEXTURE, j, k, 0, 182, this.xSize, this.ySize);

        // Current colour swatch
        int currentRgb = Color.HSBtoRGB(this.currentHSB[0], this.currentHSB[1], this.currentHSB[2]);
        graphics.fill(j + 153, k + 5, j + 153 + 11, k + 5 + 10, 0xFF000000 | (currentRgb & 0x00FFFFFF));

        // Hue / saturation / brightness bars
        for (int n = 0; n < this.componentBarLength; n++) {
            int rgb = Color.HSBtoRGB(n / this.componentBarLength, this.currentHSB[1], this.currentHSB[2]);
            graphics.fill(j + 103 + n, k + 17, j + 103 + n + 1, k + 17 + 10, 0xFF000000 | (rgb & 0x00FFFFFF));
        }
        for (int n = 0; n < this.componentBarLength; n++) {
            int rgb = Color.HSBtoRGB(this.currentHSB[0], n / this.componentBarLength, this.currentHSB[2]);
            graphics.fill(j + 103 + n, k + 29, j + 103 + n + 1, k + 29 + 10, 0xFF000000 | (rgb & 0x00FFFFFF));
        }
        for (int n = 0; n < this.componentBarLength; n++) {
            int rgb = Color.HSBtoRGB(this.currentHSB[0], this.currentHSB[1], n / this.componentBarLength);
            graphics.fill(j + 103 + n, k + 41, j + 103 + n + 1, k + 41 + 10, 0xFF000000 | (rgb & 0x00FFFFFF));
        }

        // Slider knobs
        graphics.blit(TEXTURE, j + 103 + (int) (this.currentHSB[0] * this.componentBarLength), k + 16, 176, 0, 3, 12);
        graphics.blit(TEXTURE, j + 103 + (int) (this.currentHSB[1] * this.componentBarLength), k + 28, 176, 0, 3, 12);
        graphics.blit(TEXTURE, j + 103 + (int) (this.currentHSB[2] * this.componentBarLength), k + 40, 176, 0, 3, 12);

        super.render(graphics, mouseX, mouseY, partialTicks);

        if (!this.isRecolourGUI) {
            graphics.drawString(this.font, "Enter Name:", j + 6, k + 6, 0xFFFFFF, true);
        }
        graphics.drawString(this.font, "Colour:", j + 106, k + 6, 0xFFFFFF, true);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        updateFromMouse(mouseX, mouseY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        updateFromMouse(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        int xInGui = (int) mouseX - this.guiLeft;
        int yInGui = (int) mouseY - this.guiTop;

        if (xInGui >= 103 && xInGui < 103 + this.componentBarLength) {
            if (yInGui >= 17 && yInGui < 27) {
                this.currentHSB[0] = (xInGui - 103) / this.componentBarLength;
            } else if (yInGui >= 29 && yInGui < 39) {
                this.currentHSB[1] = (xInGui - 103) / this.componentBarLength;
            } else if (yInGui >= 41 && yInGui < 51) {
                this.currentHSB[2] = (xInGui - 103) / this.componentBarLength;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
