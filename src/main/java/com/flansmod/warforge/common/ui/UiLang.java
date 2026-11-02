package com.flansmod.warforge.common.ui;

import net.minecraft.locale.Language;

/**
 * Translation for UI text, usable on both sides. The server never draws, and its language has no mod keys, so a
 * missing key returns the key itself. Call at build time; do not cache the result across language changes.
 */
public final class UiLang {
    private UiLang() {
    }

    public static String tr(String key) {
        return Language.getInstance().getOrDefault(key);
    }

    public static String tr(String key, Object... args) {
        return String.format(Language.getInstance().getOrDefault(key), args);
    }
}
