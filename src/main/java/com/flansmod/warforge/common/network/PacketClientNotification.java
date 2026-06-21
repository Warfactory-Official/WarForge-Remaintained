package com.flansmod.warforge.common.network;

import com.flansmod.warforge.client.util.WarForgeNotifications;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

public class PacketClientNotification extends PacketBase {
    public String token = "";
    public String title = "";
    public String subtitle = "";
    public int accentColor = 0x708A97;
    public int durationMs = 5000;
    public UUID playerId = null;

    public PacketClientNotification() {
    }

    public PacketClientNotification(String token, String title, String subtitle, int accentColor, int durationMs, UUID playerId) {
        this.token = token;
        this.title = title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.accentColor = accentColor;
        this.durationMs = durationMs;
        this.playerId = playerId;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUTF(data, token);
        writeUTF(data, title);
        writeUTF(data, subtitle);
        data.writeInt(accentColor);
        data.writeInt(durationMs);
        data.writeBoolean(playerId != null);
        if (playerId != null) {
            writeUUID(data, playerId);
        }
    }

    @Override
    public void decodeInto(FriendlyByteBuf data) {
        token = readUTF(data);
        title = readUTF(data);
        subtitle = readUTF(data);
        accentColor = data.readInt();
        durationMs = data.readInt();
        playerId = data.readBoolean() ? readUUID(data) : null;
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleClientSide(Player clientPlayer) {
        WarForgeNotifications.show(token, title, subtitle.isEmpty() ? null : subtitle, accentColor, durationMs, playerId);
    }
}
