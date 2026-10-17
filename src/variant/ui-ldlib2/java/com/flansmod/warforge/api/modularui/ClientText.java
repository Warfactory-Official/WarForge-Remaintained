package com.flansmod.warforge.api.modularui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class ClientText {
    private ClientText() {
    }

    /** {width of widest line, lines * lineHeight - 1} of text split at maxWidth (ModularUI TextRenderer). */
    static int[] wrappedSize(Component text, int maxWidth) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        int width = 0;
        for (FormattedCharSequence line : lines) {
            width = Math.max(width, font.width(line));
        }
        return new int[]{width, Math.max(1, lines.size()) * font.lineHeight - 1};
    }
}
