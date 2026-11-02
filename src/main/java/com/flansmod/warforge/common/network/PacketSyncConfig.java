package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PacketSyncConfig extends PacketBase {
    private static final int MAX_TOML_LENGTH = 1 << 20;

    public WarForgeConfig.Snapshot snapshot;

    public PacketSyncConfig() {
    }

    public PacketSyncConfig(WarForgeConfig.Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        data.writeUtf(snapshot.toml(), MAX_TOML_LENGTH);
        data.writeShort(snapshot.megachunkLength());
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        snapshot = new WarForgeConfig.Snapshot(data.readUtf(MAX_TOML_LENGTH), data.readShort());
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }
}
