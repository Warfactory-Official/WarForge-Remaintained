package com.flansmod.warforge.common.factories;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.AbstractUIFactory;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.GuiClaimManager;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketClaimChunksData;
import com.flansmod.warforge.common.util.DimChunkPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClaimManagerGuiFactory extends AbstractUIFactory<ClaimManagerGuiData> {
    public static final ClaimManagerGuiFactory INSTANCE = new ClaimManagerGuiFactory();

    private static final IGuiHolder<ClaimManagerGuiData> HOLDER = new IGuiHolder<>() {
        @Override
        public ModularPanel buildUI(ClaimManagerGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
            if (guiData.isClient()) {
                return GuiClaimManager.buildPanel(guiData);
            }
            return ModularPanel.defaultPanel("claim_manager")
                    .width(640)
                    .height(640)
                    .topRel(0.40f);
        }

        @Override
        public ModularScreen createScreen(ClaimManagerGuiData guiData, ModularPanel mainPanel) {
            return new ModularScreen(Tags.MODID, mainPanel);
        }
    };

    private ClaimManagerGuiFactory() {
        super("warforge:claim_manager");
    }

    public static void init() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    public void open(EntityPlayer player, DimChunkPos center, int radius, int pageX, int pageZ) {
        EntityPlayerMP serverPlayer = verifyServerSide(player);
        GuiManager.open(this, createServerData(serverPlayer, center, radius, pageX, pageZ, true), serverPlayer);
    }

    @SideOnly(Side.CLIENT)
    public void openClient(DimChunkPos center, int radius, int pageX, int pageZ) {
        GuiManager.openFromClient(this, new ClaimManagerGuiData(verifyClientSide(Platform.getClientPlayer()), center, radius, pageX, pageZ));
    }

    @Override
    public @NotNull IGuiHolder<ClaimManagerGuiData> getGuiHolder(ClaimManagerGuiData guiData) {
        return HOLDER;
    }

    @Override
    public void writeGuiData(ClaimManagerGuiData guiData, PacketBuffer packetBuffer) {
        if (guiData.isClient()) {
            packetBuffer.writeInt(guiData.dim);
            packetBuffer.writeInt(guiData.centerX);
            packetBuffer.writeInt(guiData.centerZ);
            packetBuffer.writeByte(guiData.radius);
            packetBuffer.writeInt(guiData.pageX);
            packetBuffer.writeInt(guiData.pageZ);
            return;
        }

        packetBuffer.writeInt(guiData.pageX);
        packetBuffer.writeInt(guiData.pageZ);
        guiData.toPacket().encodeInto(null, packetBuffer);
    }

    @Override
    public @NotNull ClaimManagerGuiData readGuiData(EntityPlayer entityPlayer, PacketBuffer packetBuffer) {
        if (!entityPlayer.world.isRemote) {
            DimChunkPos center = new DimChunkPos(packetBuffer.readInt(), packetBuffer.readInt(), packetBuffer.readInt());
            int radius = packetBuffer.readByte();
            int pageX = packetBuffer.readInt();
            int pageZ = packetBuffer.readInt();
            return createServerData((EntityPlayerMP) entityPlayer, center, radius, pageX, pageZ, true);
        }

        int pageX = packetBuffer.readInt();
        int pageZ = packetBuffer.readInt();
        PacketClaimChunksData packet = new PacketClaimChunksData();
        packet.decodeInto(null, packetBuffer);
        return new ClaimManagerGuiData(entityPlayer, packet, pageX, pageZ);
    }

    private ClaimManagerGuiData createServerData(EntityPlayerMP player, DimChunkPos center, int radius, int pageX, int pageZ, boolean syncClaimCache) {
        PacketClaimChunksData packet = WarForgeMod.FACTIONS.createClaimChunksData(player, center, radius);
        if (syncClaimCache) {
            WarForgeMod.NETWORK.sendTo(packet, player);
        }
        return new ClaimManagerGuiData(player, packet, pageX, pageZ);
    }
}
