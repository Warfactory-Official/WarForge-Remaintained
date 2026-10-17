package com.flansmod.warforge.common.factories;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRequestInsurance;
import com.flansmod.warforge.common.ui.InsuranceUi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

public final class FactionInsuranceGuiFactory {
    public static final FactionInsuranceGuiFactory INSTANCE = new FactionInsuranceGuiFactory();

    private FactionInsuranceGuiFactory() {
    }

    public static void init() {
        InsuranceUi.register();
    }

    @OnlyIn(Dist.CLIENT)
    public void openClient(UUID factionId) {
        requestInsuranceData(factionId);
    }

    @OnlyIn(Dist.CLIENT)
    public void openClientChild(Runnable reopenParent, UUID factionId) {
        requestInsuranceData(factionId);
    }

    @OnlyIn(Dist.CLIENT)
    public void openClientSibling(UUID factionId) {
        requestInsuranceData(factionId);
    }

    @OnlyIn(Dist.CLIENT)
    private void requestInsuranceData(UUID factionId) {
        PacketRequestInsurance packet = new PacketRequestInsurance();
        packet.factionId = factionId;
        WarForgeMod.NETWORK.sendToServer(packet);
    }
}
