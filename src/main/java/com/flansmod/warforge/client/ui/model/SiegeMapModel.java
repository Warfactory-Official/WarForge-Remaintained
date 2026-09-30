package com.flansmod.warforge.client.ui.model;

import com.flansmod.warforge.compat.ClientCompat;
import com.flansmod.warforge.api.Time;
import com.flansmod.warforge.api.modularui.ChunkMapTextureDaemon;
import com.flansmod.warforge.api.modularui.ChunkMapUtil;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.network.PacketStartSiege;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfoRender;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.compat.Keys;
import com.flansmod.warforge.server.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Data and actions of the siege camp target map, independent of the UI library. Built once per opened screen from
 * the siege camp packet.
 */
@OnlyIn(Dist.CLIENT)
public final class SiegeMapModel {
    public static final int RADIUS = 2;

    public final PacketSiegeCampInfo data;
    public final DimChunkPos centerChunk;
    public final String dimName;
    public final int totalSize;
    /** The possible attacks in grid order (row by row, north to south), null where there is none. */
    public final List<SiegeCampAttackInfo> orderedAttacks;
    public final boolean[][] adjacency;
    public final String textureNamespace;

    public SiegeMapModel(PacketSiegeCampInfo data) {
        this.data = data;
        this.centerChunk = data.mSiegeCampPos.toChunkPos();
        this.dimName = Keys.id(data.mSiegeCampPos.dim).getPath();
        this.totalSize = 2 * RADIUS + 1;

        HashMap<Long, SiegeCampAttackInfo> byOffset = new HashMap<>();
        for (SiegeCampAttackInfo info : data.mPossibleAttacks) {
            byOffset.put(ChunkMapUtil.key(info.mOffset.getX(), info.mOffset.getZ()), info);
        }

        orderedAttacks = new ArrayList<>(totalSize * totalSize);
        for (int z = -RADIUS; z <= RADIUS; z++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                orderedAttacks.add(byOffset.get(ChunkMapUtil.key(x, z)));
            }
        }

        HashMap<Long, Integer> tintByChunk = new HashMap<>();
        for (SiegeCampAttackInfo info : orderedAttacks) {
            if (info != null && !info.mFactionUUID.equals(Faction.nullUuid)) {
                tintByChunk.put(ChunkMapUtil.key(centerChunk.x + info.mOffset.getX(), centerChunk.z + info.mOffset.getZ()), info.mFactionColour);
            }
        }

        textureNamespace = "siegemap_" + dimName + "_" + centerChunk.x + "_" + centerChunk.z;
        ChunkMapTextureDaemon.requestMapUpdate(textureNamespace, data.mSiegeCampPos.dim, centerChunk.x, centerChunk.z, RADIUS, tintByChunk);

        adjacency = new boolean[orderedAttacks.size()][4];
        ChunkMapUtil.computeAdjacency(orderedAttacks, RADIUS, adjacency);
    }

    public String campLine() {
        return "Camp [" + centerChunk.x + ", " + centerChunk.z + "] | Dim " + dimName + " | Radius " + RADIUS;
    }

    public String momentumLine() {
        return "Momentum: " + data.momentum;
    }

    /** The render state of a possible attack (the camp's own chunk gets the camp mark). */
    public SiegeCampAttackInfoRender renderInfo(SiegeCampAttackInfo rawInfo) {
        SiegeCampAttackInfoRender chunkInfo = new SiegeCampAttackInfoRender(rawInfo);
        if (chunkInfo.mOffset.getX() == 0 && chunkInfo.mOffset.getZ() == 0) {
            chunkInfo.setCenterMarkType(SiegeCampAttackInfoRender.CenterMarkType.SIEGE_CAMP);
        }
        return chunkInfo;
    }

    public String textureName(SiegeCampAttackInfoRender chunkInfo) {
        return ChunkMapTextureDaemon.getTextureName(
                textureNamespace,
                data.mSiegeCampPos.dim,
                centerChunk.x + chunkInfo.mOffset.getX(),
                centerChunk.z + chunkInfo.mOffset.getZ()
        );
    }

    /** Starts the siege on the chunk and closes the screen. */
    public void attack(SiegeCampAttackInfoRender chunkInfo) {
        PacketStartSiege siegePacket = new PacketStartSiege();
        siegePacket.mSiegeCampPos = data.mSiegeCampPos;
        siegePacket.mOffset = chunkInfo.mOffset;
        WarForgeMod.NETWORK.sendToServer(siegePacket);
        ClientCompat.setScreen(null);
    }

    public List<Component> tooltip(SiegeCampAttackInfoRender chunkInfo) {
        List<Component> lines = new ArrayList<>();
        if (!chunkInfo.mFactionName.isEmpty()) {
            lines.add(Component.literal("Territory of " + chunkInfo.mFactionName));
        } else {
            lines.add(Component.literal("Wilderness"));
        }
        lines.add(Component.literal("Claim Type: " + UiText.formatClaimType(chunkInfo.claimType)));
        if (chunkInfo.mWarforgeVein != null) {
            lines.add(Component.literal("Ore: " + UiText.translateVeinName(chunkInfo.mWarforgeVein.translationKey,
                    I18n.get(chunkInfo.mOreQuality.getTranslationKey()) + " [" +
                            chunkInfo.mOreQuality.getMultString(chunkInfo.mWarforgeVein) + "]")));
        } else {
            lines.add(Component.literal("No ores in this chunk"));
        }
        if (chunkInfo.canAttack) {
            Integer momentumMs = WarForgeConfig.SIEGE_MOMENTUM_TIME.get(data.momentum);
            if (momentumMs != null) {
                lines.add(Component.literal("Attack time: " + new Time((long) momentumMs * 1000L).getFormattedTime(Time.TimeFormat.HOURS_MINUTES_SECONDS, Time.Verbality.SHORT)));
            }
            lines.add(Component.literal("Click to attack!"));
        }
        return lines;
    }
}
