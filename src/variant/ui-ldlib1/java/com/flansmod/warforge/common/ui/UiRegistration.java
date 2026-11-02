package com.flansmod.warforge.common.ui;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;

public final class UiRegistration {
    private UiRegistration() {
    }

    public static void register() {
        UIFactory.register(BlockUiFactory.INSTANCE);
        UIFactory.register(InsuranceUi.INSTANCE);
    }
}
