package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.item.ItemMatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class PacketCitadelUpgradeRequirement extends PacketBase {
    public int level;
    public HashMap<ItemMatcher, Integer> requirements;
    public int limit;
    public int insuranceSlots;
    public int loadedChunks;

    public PacketCitadelUpgradeRequirement(int level, HashMap<ItemMatcher, Integer> requirements, int limit, int insuranceSlots, int loadedChunks) {
        this.level = level;
        this.requirements = requirements;
        this.limit = limit;
        this.insuranceSlots = insuranceSlots;
        this.loadedChunks = loadedChunks;
    }

    public PacketCitadelUpgradeRequirement() {
    }

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeInt(level);
        data.writeInt(limit);
        data.writeInt(insuranceSlots);
        data.writeInt(loadedChunks);
        data.writeVarInt(requirements.size());
        for (Map.Entry<ItemMatcher, Integer> entry : requirements.entrySet()) {
            entry.getKey().write(data);
            data.writeVarInt(entry.getValue());
        }
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        level = data.readInt();
        limit = data.readInt();
        insuranceSlots = data.readInt();
        loadedChunks = data.readInt();
        requirements = new HashMap<>();

        int count = PacketSecurity.clampCount(data.readVarInt(), 4096);
        for (int i = 0; i < count; i++) {
            ItemMatcher matcher = ItemMatcher.read(data);
            int amount = data.readVarInt();
            if (matcher != null) {
                requirements.put(matcher, amount);
            }
        }
    }

    @Override
    public void handleServerSide(ServerPlayer player) {
        PacketSecurity.violation(player, "Received level requirement info on server");
    }

}
