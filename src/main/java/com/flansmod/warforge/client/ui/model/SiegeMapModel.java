package com.flansmod.warforge.client.ui.model;

import com.flansmod.warforge.common.SiegeMomentum;
import com.flansmod.warforge.api.Time;
import com.flansmod.warforge.client.map.ChunkMapTextureDaemon;
import com.flansmod.warforge.client.map.ChunkMapUtil;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.PacketStartSiege;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.client.ui.model.SiegeCampAttackInfoRender;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.compat.Keys;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class SiegeMapModel {
    public static final int RADIUS = 2;

    public final PacketSiegeCampInfo data;
    public final DimChunkPos centerChunk;
    public final String dimName;
    public final int totalSize;
    public final List<SiegeCampAttackInfo> orderedAttacks;
    public final boolean[][] adjacency;
    public final String textureNamespace;

    public SiegeMapModel(PacketSiegeCampInfo data) {
        this.data = data;
        this.centerChunk = data.siegeCampPos.toChunkPos();
        this.dimName = Keys.id(data.siegeCampPos.dim).getPath();
        this.totalSize = 2 * RADIUS + 1;

        Long2ObjectOpenHashMap<SiegeCampAttackInfo> byOffset = new Long2ObjectOpenHashMap<>();
        for (SiegeCampAttackInfo info : data.possibleAttacks) {
            byOffset.put(ChunkMapUtil.key(info.offset.getX(), info.offset.getZ()), info);
        }

        orderedAttacks = new ArrayList<>(totalSize * totalSize);
        for (int z = -RADIUS; z <= RADIUS; z++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                orderedAttacks.add(byOffset.get(ChunkMapUtil.key(x, z)));
            }
        }

        Long2IntOpenHashMap tintByChunk = new Long2IntOpenHashMap();
        for (SiegeCampAttackInfo info : orderedAttacks) {
            if (info != null && !info.factionUuid.equals(FactionIds.NONE)) {
                tintByChunk.put(ChunkMapUtil.key(centerChunk.x + info.offset.getX(), centerChunk.z + info.offset.getZ()), info.factionColour);
            }
        }

        textureNamespace = "siegemap_" + dimName + "_" + centerChunk.x + "_" + centerChunk.z;
        ChunkMapTextureDaemon.requestMapUpdate(textureNamespace, data.siegeCampPos.dim, centerChunk.x, centerChunk.z, RADIUS, tintByChunk);

        adjacency = new boolean[orderedAttacks.size()][4];
        ChunkMapUtil.computeAdjacency(orderedAttacks, RADIUS, adjacency);
    }

    public String campLine() {
        return I18n.get("warforge.client.siege_map.camp", centerChunk.x, centerChunk.z, dimName, RADIUS);
    }

    public String momentumLine() {
        return I18n.get("warforge.client.siege_map.momentum", data.momentum);
    }

    public SiegeCampAttackInfoRender renderInfo(SiegeCampAttackInfo rawInfo) {
        SiegeCampAttackInfoRender chunkInfo = new SiegeCampAttackInfoRender(rawInfo);
        if (chunkInfo.offset.getX() == 0 && chunkInfo.offset.getZ() == 0) {
            chunkInfo.setCenterMarkType(SiegeCampAttackInfoRender.CenterMarkType.SIEGE_CAMP);
        }
        return chunkInfo;
    }

    public String textureName(SiegeCampAttackInfoRender chunkInfo) {
        return ChunkMapTextureDaemon.getTextureName(
                textureNamespace,
                data.siegeCampPos.dim,
                centerChunk.x + chunkInfo.offset.getX(),
                centerChunk.z + chunkInfo.offset.getZ()
        );
    }

    public void attack(SiegeCampAttackInfoRender chunkInfo) {
        PacketStartSiege siegePacket = new PacketStartSiege();
        siegePacket.siegeCampPos = data.siegeCampPos;
        siegePacket.offset = chunkInfo.offset;
        WarForgeMod.NETWORK.sendToServer(siegePacket);
        ClientCompat.setScreen(null);
    }

    public List<Component> tooltip(SiegeCampAttackInfoRender chunkInfo) {
        List<Component> lines = new ArrayList<>();
        if (!chunkInfo.factionName.isEmpty()) {
            lines.add(Component.translatable("warforge.client.siege_map.territory", chunkInfo.factionName));
        } else {
            lines.add(Component.translatable("warforge.client.map.tip.wilderness"));
        }
        lines.add(Component.translatable("warforge.client.map.tip.claim_type", UiText.formatClaimType(chunkInfo.claimType)));
        if (chunkInfo.warforgeVein != null) {
            lines.add(Component.translatable("warforge.client.map.tip.ore", UiText.veinLabel(chunkInfo.warforgeVein, chunkInfo.oreQuality)));
        } else {
            lines.add(Component.translatable("warforge.client.map.tip.no_ore"));
        }
        if (chunkInfo.canAttack) {
            Integer momentumMs = SiegeMomentum.seconds(data.momentum);
            if (momentumMs != null) {
                lines.add(Component.translatable("warforge.client.siege_map.attack_time",
                        new Time((long) momentumMs * 1000L).getFormattedTime(Time.TimeFormat.HOURS_MINUTES_SECONDS, Time.Verbality.SHORT)));
            }
            lines.add(Component.translatable("warforge.client.siege_map.click_attack"));
        }
        return lines;
    }
}
