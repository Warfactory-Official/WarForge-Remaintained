package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketSecurity.Bucket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The WarForge channel: every packet type, the one direction it may travel, and its rate limit.
 * The loader's network code (platform.NetworkPlatform) registers this table.
 *
 * <p>Every payload declares the one direction it may travel. The loader checks the registered direction
 * against the connection and refuses to decode a mismatch, so a modified client can neither reach a server
 * handler with a client-bound payload nor make the server run a client-bound decoder (several of which size
 * collections from the payload itself).
 *
 * <p>Client-to-server payloads also declare a {@link Bucket}: the per-player rate limit charged in
 * {@link #handle} before the handler runs, so no packet can be added without a cost attached.
 */
public final class WFNetwork {
    private WFNetwork() {
    }

    /** Protocol version. Change it when a packet's wire format changes. */
    public static final String VERSION = "1";

    public enum Direction { TO_SERVER, TO_CLIENT, BOTH }

    /**
     * One packet type. The channel id is the lowercase class name (for example "packetcreatefaction"), so the
     * table order does not matter on NeoForge; on Forge the order is the message index, so add new packets at
     * the end.
     */
    public record Entry<T extends PacketBase>(String id, Class<T> type, Supplier<T> factory, Direction direction,
                                              Bucket bucket) {
    }

    public static final List<Entry<?>> PACKETS = List.of(
            c2s(PacketCreateFaction.class, PacketCreateFaction::new, Bucket.ACTION),
            s2c(PacketOpenCreateFaction.class, PacketOpenCreateFaction::new),
            s2c(PacketOpenFlagSelect.class, PacketOpenFlagSelect::new),
            s2c(PacketOpenRecolour.class, PacketOpenRecolour::new),
            c2s(PacketRequestFactionInfo.class, PacketRequestFactionInfo::new, Bucket.QUERY),
            s2c(PacketFactionInfo.class, PacketFactionInfo::new),
            s2c(PacketSiegeCampInfo.class, PacketSiegeCampInfo::new),
            s2c(PacketSiegeCampProgressUpdate.class, PacketSiegeCampProgressUpdate::new),
            c2s(PacketStartSiege.class, PacketStartSiege::new, Bucket.ACTION),
            s2c(PacketLeaderboardInfo.class, PacketLeaderboardInfo::new),
            c2s(PacketRequestLeaderboardInfo.class, PacketRequestLeaderboardInfo::new, Bucket.QUERY),
            c2s(PacketDisbandFaction.class, PacketDisbandFaction::new, Bucket.ACTION),
            c2s(PacketRemoveClaim.class, PacketRemoveClaim::new, Bucket.ACTION),
            s2c(PacketTimeUpdates.class, PacketTimeUpdates::new),
            c2s(PacketSetFactionColour.class, PacketSetFactionColour::new, Bucket.ACTION),
            s2c(PacketClientNotification.class, PacketClientNotification::new),
            c2s(PacketMoveCitadel.class, PacketMoveCitadel::new, Bucket.ACTION),
            s2c(PacketCitadelUpgradeRequirement.class, PacketCitadelUpgradeRequirement::new),
            c2s(PacketRequestUpgradeUI.class, PacketRequestUpgradeUI::new, Bucket.QUERY),
            s2c(PacketUpgradeUI.class, PacketUpgradeUI::new),
            c2s(PacketRequestUpgrade.class, PacketRequestUpgrade::new, Bucket.ACTION),
            c2s(PacketChooseFactionFlag.class, PacketChooseFactionFlag::new, Bucket.ACTION),
            s2c(PacketFlagManifest.class, PacketFlagManifest::new),
            s2c(PacketFlagChunk.class, PacketFlagChunk::new),
            s2c(PacketEffect.class, PacketEffect::new),
            s2c(PacketNamePlateChange.class, PacketNamePlateChange::new),
            c2s(PacketRequestNamePlate.class, PacketRequestNamePlate::new, Bucket.QUERY),
            // Genuinely two-way: clients query a chunk's vein, the server answers with the same type.
            both(PacketChunkPosVeinID.class, PacketChunkPosVeinID::new, Bucket.QUERY),
            s2c(PacketVeinEntries.class, PacketVeinEntries::new),
            s2c(PacketSyncConfig.class, PacketSyncConfig::new),
            c2s(PacketRequestClaimChunks.class, PacketRequestClaimChunks::new, Bucket.MAP),
            s2c(PacketClaimChunksData.class, PacketClaimChunksData::new),
            c2s(PacketClaimChunkAction.class, PacketClaimChunkAction::new, Bucket.ACTION),
            c2s(PacketFactionMemberManagerAction.class, PacketFactionMemberManagerAction::new, Bucket.ACTION),
            c2s(PacketRequestMemberData.class, PacketRequestMemberData::new, Bucket.QUERY),
            s2c(PacketMemberData.class, PacketMemberData::new),
            c2s(PacketFactionInsuranceAction.class, PacketFactionInsuranceAction::new, Bucket.ACTION),
            c2s(PacketRequestInsurance.class, PacketRequestInsurance::new, Bucket.QUERY),
            s2c(PacketInsuranceData.class, PacketInsuranceData::new),
            s2c(PacketJourneyMapClaims.class, PacketJourneyMapClaims::new),
            s2c(PacketJourneyMapVeins.class, PacketJourneyMapVeins::new),
            c2s(PacketDeclareSiege.class, PacketDeclareSiege::new, Bucket.ACTION),
            c2s(PacketRequestTerrainColors.class, PacketRequestTerrainColors::new, Bucket.TERRAIN),
            s2c(PacketTerrainColors.class, PacketTerrainColors::new),
            c2s(PacketFactionAllianceAction.class, PacketFactionAllianceAction::new, Bucket.ACTION),
            c2s(PacketEstablishFob.class, PacketEstablishFob::new, Bucket.ACTION),
            c2s(PacketRequestFobWarp.class, PacketRequestFobWarp::new, Bucket.ACTION),
            c2s(PacketFobManagerAction.class, PacketFobManagerAction::new, Bucket.ACTION),
            c2s(PacketRequestOperations.class, PacketRequestOperations::new, Bucket.QUERY),
            s2c(PacketOperationsData.class, PacketOperationsData::new)
    );

    private static final Map<Class<?>, Entry<?>> BY_CLASS = new HashMap<>();

    static {
        for (Entry<?> entry : PACKETS) {
            BY_CLASS.put(entry.type(), entry);
        }
    }

    /** The table entry of a packet class. */
    public static Entry<?> entryOf(Class<?> type) {
        Entry<?> entry = BY_CLASS.get(type);
        if (entry == null) {
            throw new IllegalArgumentException(type.getName() + " is not registered in WFNetwork.PACKETS");
        }
        return entry;
    }

    /** Client -> server only. The loader refuses to decode one arriving on a client. */
    private static <T extends PacketBase> Entry<T> c2s(Class<T> type, Supplier<T> factory, Bucket bucket) {
        return new Entry<>(idOf(type), type, factory, Direction.TO_SERVER, bucket);
    }

    /** Server -> client only. The loader refuses to decode one arriving on the server. */
    private static <T extends PacketBase> Entry<T> s2c(Class<T> type, Supplier<T> factory) {
        return new Entry<>(idOf(type), type, factory, Direction.TO_CLIENT, Bucket.NONE);
    }

    /** Request/response payloads that legitimately travel both ways. */
    private static <T extends PacketBase> Entry<T> both(Class<T> type, Supplier<T> factory, Bucket bucket) {
        return new Entry<>(idOf(type), type, factory, Direction.BOTH, bucket);
    }

    private static String idOf(Class<?> type) {
        return type.getSimpleName().toLowerCase(Locale.ROOT);
    }

    /**
     * Runs a decoded packet. The loader calls this on the main thread.
     *
     * @param serverbound  true if the packet arrived on the server
     * @param sender       the sending player for a serverbound packet (null if the connection has no player)
     * @param clientPlayer the local player for a clientbound packet
     */
    public static void handle(PacketBase payload, boolean serverbound, @Nullable ServerPlayer sender,
                              @Nullable Player clientPlayer, Bucket bucket) {
        if (serverbound) {
            // No player on the context means there is nobody to authorise the request against.
            if (sender == null) {
                return;
            }
            if (!PacketSecurity.allow(sender, bucket, payload.getClass().getSimpleName())) {
                return;
            }
            try {
                payload.handleServerSide(sender);
            } catch (Exception e) {
                // A handler must never take the server tick down over hostile input.
                WarForgeMod.LOGGER.error("Error handling " + payload.getClass().getSimpleName()
                        + " from " + sender.getName().getString(), e);
            }
        } else {
            payload.handleClientSide(clientPlayer);
        }
    }
}
