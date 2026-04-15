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
import com.flansmod.warforge.client.GuiFactionFlagSelect;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public class FactionFlagSelectGuiFactory extends AbstractUIFactory<FactionFlagSelectGuiData> {
    public static final FactionFlagSelectGuiFactory INSTANCE = new FactionFlagSelectGuiFactory();

    private static final IGuiHolder<FactionFlagSelectGuiData> HOLDER = new IGuiHolder<FactionFlagSelectGuiData>() {
        @Override
        public ModularPanel buildUI(FactionFlagSelectGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
            return GuiFactionFlagSelect.buildPanel(guiData);
        }

        @Override
        public ModularScreen createScreen(FactionFlagSelectGuiData guiData, ModularPanel mainPanel) {
            return new ModularScreen(Tags.MODID, mainPanel);
        }
    };

    private FactionFlagSelectGuiFactory() {
        super("warforge:faction_flag_select");
    }

    public static void init() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    @SideOnly(Side.CLIENT)
    public void openClient(UUID factionId) {
        GuiManager.openFromClient(this, new FactionFlagSelectGuiData(verifyClientSide(Platform.getClientPlayer()), factionId));
    }

    public void open(EntityPlayer player, UUID factionId) {
        EntityPlayerMP serverPlayer = verifyServerSide(player);
        GuiManager.open(this, createServerData(serverPlayer, factionId), serverPlayer);
    }

    @Override
    public @NotNull IGuiHolder<FactionFlagSelectGuiData> getGuiHolder(FactionFlagSelectGuiData guiData) {
        return HOLDER;
    }

    @Override
    public void writeGuiData(FactionFlagSelectGuiData guiData, PacketBuffer packetBuffer) {
        if (guiData.isClient()) {
            packetBuffer.writeLong(guiData.factionId.getMostSignificantBits());
            packetBuffer.writeLong(guiData.factionId.getLeastSignificantBits());
            return;
        }

        packetBuffer.writeLong(guiData.factionId.getMostSignificantBits());
        packetBuffer.writeLong(guiData.factionId.getLeastSignificantBits());
        packetBuffer.writeString(guiData.factionName);
        packetBuffer.writeInt(guiData.factionColor);
        packetBuffer.writeString(guiData.currentFlagId);
        packetBuffer.writeBoolean(guiData.canChoose);
        packetBuffer.writeShort(guiData.availableFlags.size());
        for (String id : guiData.availableFlags) {
            packetBuffer.writeString(id);
        }
    }

    @Override
    public @NotNull FactionFlagSelectGuiData readGuiData(EntityPlayer entityPlayer, PacketBuffer packetBuffer) {
        if (!entityPlayer.world.isRemote) {
            UUID factionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
            return createServerData((EntityPlayerMP) entityPlayer, factionId);
        }

        FactionFlagSelectGuiData data = new FactionFlagSelectGuiData(entityPlayer, new UUID(packetBuffer.readLong(), packetBuffer.readLong()));
        data.factionName = packetBuffer.readString(32767);
        data.factionColor = packetBuffer.readInt();
        data.currentFlagId = packetBuffer.readString(32767);
        data.canChoose = packetBuffer.readBoolean();
        int count = packetBuffer.readShort();
        for (int i = 0; i < count; i++) {
            data.availableFlags.add(packetBuffer.readString(32767));
        }
        return data;
    }

    private FactionFlagSelectGuiData createServerData(EntityPlayerMP player, UUID factionId) {
        Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
        FactionFlagSelectGuiData data = new FactionFlagSelectGuiData(player, factionId);
        if (faction == null) {
            return data;
        }
        data.factionName = faction.name;
        data.factionColor = faction.colour;
        data.currentFlagId = faction.flagId;
        data.canChoose = (WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUniqueID(), Faction.Role.LEADER)) && faction.flagId.isEmpty();
        data.availableFlags.addAll(WarForgeMod.FLAG_REGISTRY.getAvailableFlagIds());
        return data;
    }
}
