package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.OperationsGuiData;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.Siege;
import com.flansmod.warforge.server.fob.Fob;
import com.flansmod.warforge.server.fob.FobPresence;
import com.flansmod.warforge.server.fob.FobWarpQueue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import com.flansmod.warforge.compat.Keys;

public class PacketRequestOperations extends ServerboundPacket {
    @Override
    public void encodeInto(FriendlyByteBuf data) {
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        PacketOperationsData resp = new PacketOperationsData();
        Faction faction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
        if (faction == null) {
            WarForgeMod.NETWORK.sendTo(resp, player);
            return;
        }
        resp.hasFaction = true;
        resp.factionId = faction.getUuid();
        resp.factionName = faction.getName();
        resp.factionColor = faction.getColour();
        resp.warpTicketCost = 1 + FobWarpQueue.vehicleExtraCost(player.getVehicle());

        boolean ownerInSiege = WarForgeMod.FACTIONS.sieges.isFactionInActiveSiege(faction.getUuid());

        for (Map.Entry<DimChunkPos, Siege> entry : WarForgeMod.FACTIONS.sieges.entries().entrySet()) {
            Siege siege = entry.getValue();
            boolean attacking = faction.getUuid().equals(siege.attackingFaction);
            boolean defending = faction.getUuid().equals(siege.defendingFaction);
            if (!attacking && !defending) {
                continue;
            }
            Faction attacker = WarForgeMod.FACTIONS.registry.getFaction(siege.attackingFaction);
            Faction defender = WarForgeMod.FACTIONS.registry.getFaction(siege.defendingFaction);
            OperationsGuiData.SiegeEntry siegeEntry = new OperationsGuiData.SiegeEntry();
            siegeEntry.attackerName = attacker == null ? "Unknown" : attacker.getName();
            siegeEntry.defenderName = defender == null ? "Unknown" : defender.getName();
            siegeEntry.attackerColor = attacker == null ? 0xFFFFFF : attacker.getColour();
            siegeEntry.defenderColor = defender == null ? 0xFFFFFF : defender.getColour();
            siegeEntry.chunkX = entry.getKey().x;
            siegeEntry.chunkZ = entry.getKey().z;
            siegeEntry.dimName = Keys.id(entry.getKey().dim).toString();
            siegeEntry.attacking = attacking;
            resp.sieges.add(siegeEntry);
        }

        for (Fob fob : WarForgeMod.FOBS.fobsOf(faction)) {
            OperationsGuiData.FobEntry fobEntry = new OperationsGuiData.FobEntry();
            fobEntry.pos = fob.pos;
            fobEntry.name = fob.name;
            fobEntry.tickets = fob.tickets;
            fobEntry.maxTickets = fob.maxTickets;
            fobEntry.occupied = FobPresence.enemyHeld(fob);
            fobEntry.canWarp = ownerInSiege && !fobEntry.occupied && fob.tickets >= resp.warpTicketCost;
            resp.fobs.add(fobEntry);
        }

        WarForgeMod.NETWORK.sendTo(resp, player);
    }

    @Override
    public void handleClientSide(Player clientPlayer) {
        WarForgeMod.LOGGER.error("Received a PacketRequestOperations on client side");
    }
}
