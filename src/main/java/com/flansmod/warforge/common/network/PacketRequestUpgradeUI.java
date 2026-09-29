package com.flansmod.warforge.common.network;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketRequestUpgradeUI extends PacketRequestFactionInfo {

    public static final CustomPacketPayload.Type<PacketRequestUpgradeUI> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Tags.MODID, "packetrequestupgradeui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequestUpgradeUI> STREAM_CODEC =
        StreamCodec.ofMember(PacketRequestUpgradeUI::encodeInto, buf -> { PacketRequestUpgradeUI p = new PacketRequestUpgradeUI(); p.decodeInto(buf); return p; });

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    @Override
    public void handleServerSide(ServerPlayer player) {
        // The upgrade panel is only meaningful for your own faction, and it reports whether you
        // outrank an officer there - so serve it strictly for the faction the player belongs to
        // rather than for whatever id the client asked about.
        Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            return;
        }
        if (!mFactionIDRequest.equals(Faction.nullUuid) && !mFactionIDRequest.equals(faction.uuid)) {
            PacketSecurity.violation(player, "upgrade UI request for another faction (" + mFactionIDRequest + ")");
            return;
        }

        UUID playerUUID = player.getUUID();
        PacketUpgradeUI packet = new PacketUpgradeUI();
        packet.mFactionID = faction.uuid;
        packet.mFactionName = faction.name;
        packet.color = faction.colour;
        packet.level = faction.citadelLevel;
        packet.outrankingOfficer = faction.isPlayerRoleInFaction(playerUUID, Faction.Role.OFFICER);
        WarForgeMod.NETWORK.sendTo(packet, player);
    }
}
