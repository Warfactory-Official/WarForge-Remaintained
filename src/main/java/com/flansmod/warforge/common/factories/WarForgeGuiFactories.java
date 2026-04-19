package com.flansmod.warforge.common.factories;

public final class WarForgeGuiFactories {
    private WarForgeGuiFactories() {
    }

    public static void init() {
        ClaimManagerGuiFactory.init();
        FactionFlagSelectGuiFactory.init();
        FactionInsuranceGuiFactory.init();
        FactionMemberManagerGuiFactory.init();
        FactionStatsGuiFactory.init();
        FactionUpgradeGuiFactory.init();
        SiegeCampGuiFactory.init();
    }
}
