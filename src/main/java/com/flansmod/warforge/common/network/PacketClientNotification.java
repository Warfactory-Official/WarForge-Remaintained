package com.flansmod.warforge.common.network;

import com.flansmod.warforge.compat.ChatJson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketClientNotification extends PacketBase {
    private static final int MAX_JSON = 4096;

    public static final int COLOR_INFO = 0x708A97;
    public static final int COLOR_SUCCESS = 0x55AA55;
    public static final int COLOR_WARNING = 0xC79A3A;
    public static final int COLOR_DANGER = 0xB34747;

    public String token = "";
    public Component title = Component.empty();
    public Component subtitle = Component.empty();
    public int accentColor = 0x708A97;
    public int durationMs = 5000;
    public UUID playerId = null;

    public PacketClientNotification() {
    }

    public PacketClientNotification(String token, Component title, Component subtitle, int accentColor, int durationMs, UUID playerId) {
        this.token = token;
        this.title = title;
        this.subtitle = subtitle == null ? Component.empty() : subtitle;
        this.accentColor = accentColor;
        this.durationMs = durationMs;
        this.playerId = playerId;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data) {
        writeUTF(data, token);
        data.writeUtf(ChatJson.toJson(title), MAX_JSON);
        data.writeUtf(ChatJson.toJson(subtitle), MAX_JSON);
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
        title = ChatJson.fromJson(data.readUtf(MAX_JSON));
        subtitle = ChatJson.fromJson(data.readUtf(MAX_JSON));
        accentColor = data.readInt();
        durationMs = data.readInt();
        playerId = data.readBoolean() ? readUUID(data) : null;
    }

    @Override
    public void handleServerSide(ServerPlayer playerEntity) {
    }
}
