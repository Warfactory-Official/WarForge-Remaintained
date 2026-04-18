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
import com.flansmod.warforge.client.GUIUpgradePanel;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public class FactionUpgradeGuiFactory extends AbstractUIFactory<FactionUpgradeGuiData> {
    public static final FactionUpgradeGuiFactory INSTANCE = new FactionUpgradeGuiFactory();

    private static final IGuiHolder<FactionUpgradeGuiData> HOLDER = new IGuiHolder<FactionUpgradeGuiData>() {
        @Override
        public ModularPanel buildUI(FactionUpgradeGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
            if (guiData.isClient()) {
                return GUIUpgradePanel.buildPanel(guiData);
            }
            return ModularPanel.defaultPanel("citadel_upgrade_panel")
                    .width(GUIUpgradePanel.WIDTH)
                    .height(GUIUpgradePanel.HEIGHT)
                    .topRel(0.40f);
        }

        @Override
        public ModularScreen createScreen(FactionUpgradeGuiData guiData, ModularPanel mainPanel) {
            return new ModularScreen(Tags.MODID, mainPanel);
        }
    };

    private FactionUpgradeGuiFactory() {
        super("warforge:faction_upgrade");
    }

    public static void init() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    @SideOnly(Side.CLIENT)
    public void openClient(UUID factionId) {
        GuiManager.openFromClient(this, new FactionUpgradeGuiData(verifyClientSide(Platform.getClientPlayer()), factionId));
    }

    public void open(EntityPlayer player, UUID factionId) {
        EntityPlayerMP serverPlayer = verifyServerSide(player);
        GuiManager.open(this, createServerData(serverPlayer, factionId), serverPlayer);
    }

    @Override
    public @NotNull IGuiHolder<FactionUpgradeGuiData> getGuiHolder(FactionUpgradeGuiData guiData) {
        return HOLDER;
    }

    @Override
    public void writeGuiData(FactionUpgradeGuiData guiData, PacketBuffer packetBuffer) {
        if (guiData.isClient()) {
            packetBuffer.writeLong(guiData.requestedFactionId.getMostSignificantBits());
            packetBuffer.writeLong(guiData.requestedFactionId.getLeastSignificantBits());
            return;
        }

        packetBuffer.writeLong(guiData.factionId.getMostSignificantBits());
        packetBuffer.writeLong(guiData.factionId.getLeastSignificantBits());
        packetBuffer.writeString(guiData.factionName);
        packetBuffer.writeInt(guiData.level);
        packetBuffer.writeInt(guiData.color);
        packetBuffer.writeBoolean(guiData.outrankingOfficer);
    }

    @Override
    public @NotNull FactionUpgradeGuiData readGuiData(EntityPlayer entityPlayer, PacketBuffer packetBuffer) {
        if (!entityPlayer.world.isRemote) {
            UUID requestedFactionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
            return createServerData((EntityPlayerMP) entityPlayer, requestedFactionId);
        }

        FactionUpgradeGuiData data = new FactionUpgradeGuiData(entityPlayer, Faction.nullUuid);
        data.factionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
        data.factionName = packetBuffer.readString(32767);
        data.level = packetBuffer.readInt();
        data.color = packetBuffer.readInt();
        data.outrankingOfficer = packetBuffer.readBoolean();
        return data;
    }

    private FactionUpgradeGuiData createServerData(EntityPlayerMP player, UUID requestedFactionId) {
        UUID effectiveFactionId = requestedFactionId;
        if (effectiveFactionId.equals(Faction.nullUuid)) {
            Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUniqueID());
            effectiveFactionId = playerFaction == null ? Faction.nullUuid : playerFaction.uuid;
        }

        FactionUpgradeGuiData data = new FactionUpgradeGuiData(player, effectiveFactionId);
        Faction faction = WarForgeMod.FACTIONS.getFaction(effectiveFactionId);
        if (faction == null) {
            return data;
        }

        data.factionId = faction.uuid;
        data.factionName = faction.name;
        data.level = faction.citadelLevel;
        data.color = faction.colour;
        data.outrankingOfficer = faction.isPlayerRoleInFaction(player.getUniqueID(), Faction.Role.OFFICER) || WarForgeMod.isOp(player);
        return data;
    }
}
