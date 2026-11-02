package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PacketRequestFactionInfo extends ServerboundPacket
{
    public UUID factionIdRequest = FactionIds.NONE;
    public String factionNameRequest = "";

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        writeUUID(data, factionIdRequest);
        writeUTF(data, factionNameRequest, PacketSecurity.MAX_NAME);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        factionIdRequest = readUUID(data);
        factionNameRequest = readUTF(data, PacketSecurity.MAX_NAME);
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity)
    {
        Faction faction = null;
        if(!factionIdRequest.equals(FactionIds.NONE))
        {
            faction = WarForgeMod.FACTIONS.registry.getFaction(factionIdRequest);
        }
        else if(!factionNameRequest.isEmpty())
        {
            faction = WarForgeMod.FACTIONS.registry.findByName(factionNameRequest);
        }
        else
        {
            PacketSecurity.violation(playerEntity, "faction info request with no valid key");
        }

        if(faction != null)
        {
            PacketFactionInfo packet = new PacketFactionInfo();
            packet.info = faction.createInfo(playerEntity.getUUID());
            WarForgeMod.NETWORK.sendTo(packet, playerEntity);
        }
        else
        {
            PacketSecurity.violation(playerEntity, "faction info request for an unknown faction");
        }
    }

    @Override
    public void handleClientSide(Player clientPlayer)
    {
        WarForgeMod.LOGGER.error("Received a faction info request client side");
    }
}
