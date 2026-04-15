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
import com.flansmod.warforge.client.GuiFactionStats;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.FactionDisplayInfo;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FactionStatsGuiFactory extends AbstractUIFactory<FactionStatsGuiData> {
    public static final FactionStatsGuiFactory INSTANCE = new FactionStatsGuiFactory();

    private static final IGuiHolder<FactionStatsGuiData> HOLDER = new IGuiHolder<FactionStatsGuiData>() {
        @Override
        public ModularPanel buildUI(FactionStatsGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
            if (guiData.isClient()) {
                return GuiFactionStats.buildPanel(guiData);
            }
            return ModularPanel.defaultPanel("faction_stats")
                    .width(320)
                    .height(240)
                    .topRel(0.40f);
        }

        @Override
        public ModularScreen createScreen(FactionStatsGuiData guiData, ModularPanel mainPanel) {
            return new ModularScreen(Tags.MODID, mainPanel);
        }
    };

    private FactionStatsGuiFactory() {
        super("warforge:faction_stats");
    }

    public static void init() {
        if (!GuiManager.hasFactory(INSTANCE.getFactoryName())) {
            GuiManager.registerFactory(INSTANCE);
        }
    }

    public void openClient(UUID factionId) {
        GuiManager.openFromClient(this, new FactionStatsGuiData(verifyClientSide(Platform.getClientPlayer()), factionId));
    }

    public void open(EntityPlayer player, UUID factionId) {
        EntityPlayerMP serverPlayer = verifyServerSide(player);
        GuiManager.open(this, createServerData(serverPlayer, factionId), serverPlayer);
    }

    @Override
    public @NotNull IGuiHolder<FactionStatsGuiData> getGuiHolder(FactionStatsGuiData guiData) {
        return HOLDER;
    }

    @Override
    public void writeGuiData(FactionStatsGuiData guiData, PacketBuffer packetBuffer) {
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
        packetBuffer.writeLong(guiData.leaderId.getMostSignificantBits());
        packetBuffer.writeLong(guiData.leaderId.getLeastSignificantBits());
        packetBuffer.writeString(guiData.leaderName);
        packetBuffer.writeInt(guiData.notoriety);
        packetBuffer.writeInt(guiData.wealth);
        packetBuffer.writeInt(guiData.legacy);
        packetBuffer.writeInt(guiData.total);
        packetBuffer.writeInt(guiData.notorietyRank);
        packetBuffer.writeInt(guiData.wealthRank);
        packetBuffer.writeInt(guiData.legacyRank);
        packetBuffer.writeInt(guiData.totalRank);
        packetBuffer.writeInt(guiData.claimCount);
        packetBuffer.writeInt(guiData.memberCount);
        packetBuffer.writeInt(guiData.level);
        packetBuffer.writeInt(guiData.claimLimit);
        packetBuffer.writeBoolean(guiData.isOwnFaction);
        packetBuffer.writeBoolean(guiData.canManageMembers);
        packetBuffer.writeBoolean(guiData.canUpgrade);
    }

    @Override
    public @NotNull FactionStatsGuiData readGuiData(EntityPlayer entityPlayer, PacketBuffer packetBuffer) {
        if (!entityPlayer.world.isRemote) {
            UUID requestedFactionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
            return createServerData((EntityPlayerMP) entityPlayer, requestedFactionId);
        }

        FactionStatsGuiData data = new FactionStatsGuiData(entityPlayer, Faction.nullUuid);
        data.hasFaction = packetBuffer.readBoolean();
        data.factionId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
        data.factionName = packetBuffer.readString(32767);
        data.factionColor = packetBuffer.readInt();
        data.leaderId = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
        data.leaderName = packetBuffer.readString(32767);
        data.notoriety = packetBuffer.readInt();
        data.wealth = packetBuffer.readInt();
        data.legacy = packetBuffer.readInt();
        data.total = packetBuffer.readInt();
        data.notorietyRank = packetBuffer.readInt();
        data.wealthRank = packetBuffer.readInt();
        data.legacyRank = packetBuffer.readInt();
        data.totalRank = packetBuffer.readInt();
        data.claimCount = packetBuffer.readInt();
        data.memberCount = packetBuffer.readInt();
        data.level = packetBuffer.readInt();
        data.claimLimit = packetBuffer.readInt();
        data.isOwnFaction = packetBuffer.readBoolean();
        data.canManageMembers = packetBuffer.readBoolean();
        data.canUpgrade = packetBuffer.readBoolean();
        return data;
    }

    private FactionStatsGuiData createServerData(EntityPlayerMP player, UUID requestedFactionId) {
        UUID effectiveFactionId = requestedFactionId;
        if (effectiveFactionId.equals(Faction.nullUuid)) {
            Faction playerFaction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUniqueID());
            effectiveFactionId = playerFaction == null ? Faction.nullUuid : playerFaction.uuid;
        }

        FactionStatsGuiData data = new FactionStatsGuiData(player, effectiveFactionId);
        Faction faction = WarForgeMod.FACTIONS.getFaction(effectiveFactionId);
        if (faction == null) {
            return data;
        }

        FactionDisplayInfo info = faction.createInfo();
        data.hasFaction = true;
        data.factionId = faction.uuid;
        data.factionName = info.factionName;
        data.factionColor = faction.colour;
        data.leaderId = info.mLeaderID;
        var leader = info.getPlayerInfo(info.mLeaderID);
        data.leaderName = leader == null ? "Unknown" : leader.username;
        data.notoriety = info.notoriety;
        data.wealth = info.wealth;
        data.legacy = info.legacy;
        data.total = info.notoriety + info.wealth + info.legacy;
        data.notorietyRank = info.notorietyRank;
        data.wealthRank = info.wealthRank;
        data.legacyRank = info.legacyRank;
        data.totalRank = info.totalRank;
        data.claimCount = info.mNumClaims;
        data.memberCount = info.members.size();
        data.level = info.lvl;
        data.claimLimit = WarForgeMod.UPGRADE_HANDLER.getClaimLimitForLevel(info.lvl);
        data.isOwnFaction = faction.isPlayerInFaction(player.getUniqueID());
        data.canManageMembers = WarForgeMod.isOp(player) || faction.isPlayerRoleInFaction(player.getUniqueID(), Faction.Role.OFFICER);
        data.canUpgrade = data.canManageMembers;
        return data;
    }
}
