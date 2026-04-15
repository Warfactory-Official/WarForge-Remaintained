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
import com.flansmod.warforge.client.GuiFactionInsurance;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.FactionInsuranceItemHandler;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FactionInsuranceGuiFactory extends AbstractUIFactory<FactionInsuranceGuiData> {
    public static final FactionInsuranceGuiFactory INSTANCE = new FactionInsuranceGuiFactory();

    private static final IGuiHolder<FactionInsuranceGuiData> HOLDER = new IGuiHolder<FactionInsuranceGuiData>() {
        @Override
        public ModularPanel buildUI(FactionInsuranceGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
            return GuiFactionInsurance.buildPanel(guiData);
        }

        @Override
        public ModularScreen createScreen(FactionInsuranceGuiData guiData, ModularPanel mainPanel) {
            return new ModularScreen(Tags.MODID, mainPanel);
        }
    };

    private FactionInsuranceGuiFactory() {
        super("warforge:faction_insurance");
    }

    public static void init() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    public void openClient(UUID factionId) {
        GuiManager.openFromClient(this, new FactionInsuranceGuiData(verifyClientSide(Platform.getClientPlayer()), factionId));
    }

    public void open(EntityPlayer player, UUID factionId) {
        EntityPlayerMP serverPlayer = verifyServerSide(player);
        GuiManager.open(this, createServerData(serverPlayer, factionId), serverPlayer);
    }

    @Override
    public @NotNull IGuiHolder<FactionInsuranceGuiData> getGuiHolder(FactionInsuranceGuiData guiData) {
        return HOLDER;
    }

    @Override
    public void writeGuiData(FactionInsuranceGuiData guiData, PacketBuffer packetBuffer) {
        if (guiData.isClient()) {
            packetBuffer.writeLong(guiData.requestedFactionId.getMostSignificantBits());
            packetBuffer.writeLong(guiData.requestedFactionId.getLeastSignificantBits());
            return;
        }

        packetBuffer.writeBoolean(guiData.hasFaction);
        packetBuffer.writeLong(guiData.factionId.getMostSignificantBits());
        packetBuffer.writeLong(guiData.factionId.getLeastSignificantBits());
        packetBuffer.writeString(guiData.factionName);
        packetBuffer.writeInt(guiData.factionColor);
        packetBuffer.writeBoolean(guiData.canDeposit);
        packetBuffer.writeBoolean(guiData.canVoid);
        packetBuffer.writeInt(guiData.slotCount);
    }

    @Override
    public @NotNull FactionInsuranceGuiData readGuiData(EntityPlayer entityPlayer, PacketBuffer packetBuffer) {
        if (!entityPlayer.world.isRemote) {
            UUID requestedFactionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
            return createServerData((EntityPlayerMP) entityPlayer, requestedFactionId);
        }

        FactionInsuranceGuiData data = new FactionInsuranceGuiData(entityPlayer, Faction.nullUuid);
        data.hasFaction = packetBuffer.readBoolean();
        data.factionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
        data.factionName = packetBuffer.readString(32767);
        data.factionColor = packetBuffer.readInt();
        data.canDeposit = packetBuffer.readBoolean();
        data.canVoid = packetBuffer.readBoolean();
        data.slotCount = packetBuffer.readInt();
        data.insuranceHandler = new ItemStackHandler(data.slotCount) {
            @Override
            public boolean isItemValid(int slot, net.minecraft.item.ItemStack stack) {
                return !com.flansmod.warforge.common.WarForgeConfig.isInsuranceBlacklisted(stack);
            }
        };
        return data;
    }

    private FactionInsuranceGuiData createServerData(EntityPlayerMP player, UUID requestedFactionId) {
        UUID effectiveFactionId = requestedFactionId;
        if (effectiveFactionId.equals(Faction.nullUuid)) {
            Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUniqueID());
            effectiveFactionId = playerFaction == null ? Faction.nullUuid : playerFaction.uuid;
        }

        FactionInsuranceGuiData data = new FactionInsuranceGuiData(player, effectiveFactionId);
        Faction faction = WarForgeMod.FACTIONS.getFaction(effectiveFactionId);
        if (faction == null) {
            return data;
        }

        data.hasFaction = true;
        data.factionId = faction.uuid;
        data.factionName = faction.name;
        data.factionColor = faction.colour;
        data.canDeposit = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUniqueID(), Faction.Role.OFFICER);
        data.canVoid = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUniqueID(), Faction.Role.LEADER);
        data.slotCount = faction.getInsuranceSlotCount();
        data.insuranceHandler = new FactionInsuranceItemHandler(faction);
        return data;
    }
}
