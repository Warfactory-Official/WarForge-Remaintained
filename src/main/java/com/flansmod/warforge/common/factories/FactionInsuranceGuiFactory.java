package com.flansmod.warforge.common.factories;

import brachy.modularui.api.IUIHolder;
import brachy.modularui.api.MCHelper;
import brachy.modularui.factory.AbstractUIFactory;
import brachy.modularui.factory.GuiManager;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import com.flansmod.warforge.Tags;
import com.flansmod.warforge.client.GuiFactionInsurance;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.FactionInsuranceItemHandler;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FactionInsuranceGuiFactory extends AbstractUIFactory<FactionInsuranceGuiData> {
    public static final FactionInsuranceGuiFactory INSTANCE = new FactionInsuranceGuiFactory();

    private static final IUIHolder<FactionInsuranceGuiData> HOLDER = new IUIHolder<>() {
        @Override
        public ModularPanel buildUI(FactionInsuranceGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
            return GuiFactionInsurance.buildPanel(guiData, syncManager);
        }

        @Override
        public ModularScreen createScreen(FactionInsuranceGuiData guiData, ModularPanel mainPanel) {
            return new ModularScreen(Tags.MODID, mainPanel);
        }
    };

    private FactionInsuranceGuiFactory() {
        super(new ResourceLocation(Tags.MODID, "faction_insurance"));
    }

    public static void init() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void openClient(UUID factionId) {
        com.flansmod.warforge.client.DeferredGuiOpen.open(() ->
                GuiManager.openFromClient(this, new FactionInsuranceGuiData(verifyClientSide(MCHelper.getPlayer()), factionId)));
    }

    @OnlyIn(Dist.CLIENT)
    public void openClientChild(Runnable reopenParent, UUID factionId) {
        com.flansmod.warforge.client.DeferredGuiOpen.openChild(reopenParent, () ->
                GuiManager.openFromClient(this, new FactionInsuranceGuiData(verifyClientSide(MCHelper.getPlayer()), factionId)));
    }

    @OnlyIn(Dist.CLIENT)
    public void openClientSibling(UUID factionId) {
        com.flansmod.warforge.client.DeferredGuiOpen.openSibling(() ->
                GuiManager.openFromClient(this, new FactionInsuranceGuiData(verifyClientSide(MCHelper.getPlayer()), factionId)));
    }

    public void open(Player player, UUID factionId) {
        ServerPlayer serverPlayer = verifyServerSide(player);
        GuiManager.open(this, createServerData(serverPlayer, factionId), serverPlayer);
    }

    @Override
    public @NotNull IUIHolder<FactionInsuranceGuiData> getGuiHolder(FactionInsuranceGuiData guiData) {
        return HOLDER;
    }

    @Override
    public void writeGuiData(FactionInsuranceGuiData guiData, FriendlyByteBuf buffer) {
        if (guiData.isClient()) {
            buffer.writeLong(guiData.requestedFactionId.getMostSignificantBits());
            buffer.writeLong(guiData.requestedFactionId.getLeastSignificantBits());
            return;
        }

        buffer.writeBoolean(guiData.hasFaction);
        buffer.writeLong(guiData.factionId.getMostSignificantBits());
        buffer.writeLong(guiData.factionId.getLeastSignificantBits());
        buffer.writeUtf(guiData.factionName);
        buffer.writeInt(guiData.factionColor);
        buffer.writeBoolean(guiData.canDeposit);
        buffer.writeBoolean(guiData.canVoid);
        buffer.writeInt(guiData.slotCount);
    }

    @Override
    public @NotNull FactionInsuranceGuiData readGuiData(Player player, FriendlyByteBuf buffer) {
        if (!player.level().isClientSide) {
            UUID requestedFactionId = new UUID(buffer.readLong(), buffer.readLong());
            return createServerData((ServerPlayer) player, requestedFactionId);
        }

        FactionInsuranceGuiData data = new FactionInsuranceGuiData(player, Faction.nullUuid);
        data.hasFaction = buffer.readBoolean();
        data.factionId = new UUID(buffer.readLong(), buffer.readLong());
        data.factionName = buffer.readUtf(32767);
        data.factionColor = buffer.readInt();
        data.canDeposit = buffer.readBoolean();
        data.canVoid = buffer.readBoolean();
        data.slotCount = buffer.readInt();
        data.insuranceHandler = new ItemStackHandler(data.slotCount) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return !WarForgeConfig.isInsuranceBlacklisted(stack);
            }
        };
        return data;
    }

    private FactionInsuranceGuiData createServerData(ServerPlayer player, UUID requestedFactionId) {
        UUID effectiveFactionId = requestedFactionId;
        if (effectiveFactionId.equals(Faction.nullUuid)) {
            Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUUID());
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
        data.canDeposit = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUUID(), Faction.Role.OFFICER);
        data.canVoid = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUUID(), Faction.Role.LEADER);
        data.slotCount = faction.getInsuranceSlotCount();
        data.insuranceHandler = new FactionInsuranceItemHandler(faction);
        return data;
    }
}
