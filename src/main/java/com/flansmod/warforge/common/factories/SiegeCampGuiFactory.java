package com.flansmod.warforge.common.factories;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.GuiSiegeCamp;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class SiegeCampGuiFactory extends AbstractUIFactory<SiegeCampGuiData> {
    public static final SiegeCampGuiFactory INSTANCE = new SiegeCampGuiFactory();

    private static final IGuiHolder<SiegeCampGuiData> HOLDER = new IGuiHolder<SiegeCampGuiData>() {
        @Override
        public ModularPanel buildUI(SiegeCampGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
            if (guiData.isClient()) {
                return GuiSiegeCamp.buildPanel(guiData);
            }
            return ModularPanel.defaultPanel("siege_main")
                    .width(380)
                    .height(508)
                    .topRel(0.40f);
        }

        @Override
        public ModularScreen createScreen(SiegeCampGuiData guiData, ModularPanel mainPanel) {
            return new ModularScreen(Tags.MODID,mainPanel);
        }
    };

    private SiegeCampGuiFactory() {
        super("warforge:siege_camp");
    }

    public static void init() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    public void open(EntityPlayer player, DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks, byte momentum, int color) {
        EntityPlayerMP serverPlayer = verifyServerSide(player);
        GuiManager.open(this, new SiegeCampGuiData(serverPlayer, siegeCampPos, possibleAttacks, momentum, color), serverPlayer);
    }

    @Override
    public @NotNull IGuiHolder<SiegeCampGuiData> getGuiHolder(SiegeCampGuiData guiData) {
        return HOLDER;
    }

    @Override
    public void writeGuiData(SiegeCampGuiData guiData, PacketBuffer packetBuffer) {
        guiData.toPacket().encodeInto(null, packetBuffer);
    }

    @Override
    public @NotNull SiegeCampGuiData readGuiData(EntityPlayer entityPlayer, PacketBuffer packetBuffer) {
        PacketSiegeCampInfo packet = new PacketSiegeCampInfo();
        packet.decodeInto(null, packetBuffer);
        return new SiegeCampGuiData(entityPlayer, packet.mSiegeCampPos, packet.mPossibleAttacks, packet.momentum, packet.color);
    }
}
