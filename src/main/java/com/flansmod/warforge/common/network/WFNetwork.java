package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.faction.Role;
import com.flansmod.warforge.common.network.PacketSecurity.Bucket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

// Direction mismatch refused at decode: client-bound decoders size collections from payload, never reachable from a client.
public final class WFNetwork {
    private WFNetwork() {
    }

    public static final String VERSION = "1";

    /** Chebyshev chunk radius around the player a vein query may address. */
    public static final int VEIN_QUERY_RADIUS = 2;

    public enum Direction { TO_SERVER, TO_CLIENT, BOTH }

    // Channel id = lowercase class name; Forge uses table order as message index => append only.
    public record Entry<T extends PacketBase>(String id, Class<T> type, Supplier<T> factory, Direction direction,
                                              Bucket bucket, @Nullable Exposure exposure) {
    }

    public static final List<Entry<?>> PACKETS = List.of(
            c2s(PacketCreateFaction.class, PacketCreateFaction::new, Bucket.ACTION, Exposure.SELF),
            s2c(PacketOpenCreateFaction.class, PacketOpenCreateFaction::new),
            s2c(PacketOpenFlagSelect.class, PacketOpenFlagSelect::new),
            s2c(PacketOpenRecolour.class, PacketOpenRecolour::new),
            c2s(PacketRequestFactionInfo.class, PacketRequestFactionInfo::new, Bucket.QUERY, Exposure.PUBLIC),
            s2c(PacketFactionInfo.class, PacketFactionInfo::new),
            s2c(PacketSiegeCampInfo.class, PacketSiegeCampInfo::new),
            s2c(PacketSiegeCampProgressUpdate.class, PacketSiegeCampProgressUpdate::new),
            c2s(PacketStartSiege.class, PacketStartSiege::new, Bucket.ACTION, Exposure.ownFactionRole(Role.OFFICER)),
            s2c(PacketLeaderboardInfo.class, PacketLeaderboardInfo::new),
            c2s(PacketRequestLeaderboardInfo.class, PacketRequestLeaderboardInfo::new, Bucket.QUERY, Exposure.PUBLIC),
            c2s(PacketDisbandFaction.class, PacketDisbandFaction::new, Bucket.ACTION, Exposure.ownFactionRole(Role.LEADER)),
            c2s(PacketRemoveClaim.class, PacketRemoveClaim::new, Bucket.ACTION, Exposure.inRange(PacketSecurity::claimRange)),
            s2c(PacketTimeUpdates.class, PacketTimeUpdates::new),
            s2c(PacketToggleBorders.class, PacketToggleBorders::new),
            c2s(PacketSetFactionColour.class, PacketSetFactionColour::new, Bucket.ACTION, Exposure.OWN_FACTION),
            s2c(PacketClientNotification.class, PacketClientNotification::new),
            c2s(PacketMoveCitadel.class, PacketMoveCitadel::new, Bucket.ACTION, Exposure.OWN_FACTION),
            s2c(PacketCitadelUpgradeRequirement.class, PacketCitadelUpgradeRequirement::new),
            c2s(PacketRequestUpgradeUI.class, PacketRequestUpgradeUI::new, Bucket.QUERY, Exposure.OWN_FACTION),
            s2c(PacketUpgradeUI.class, PacketUpgradeUI::new),
            c2s(PacketRequestUpgrade.class, PacketRequestUpgrade::new, Bucket.ACTION, Exposure.OWN_FACTION),
            c2s(PacketChooseFactionFlag.class, PacketChooseFactionFlag::new, Bucket.ACTION, Exposure.OWN_FACTION),
            s2c(PacketFlagManifest.class, PacketFlagManifest::new),
            s2c(PacketFlagChunk.class, PacketFlagChunk::new),
            s2c(PacketEffect.class, PacketEffect::new),
            s2c(PacketNamePlateChange.class, PacketNamePlateChange::new),
            c2s(PacketRequestNamePlate.class, PacketRequestNamePlate::new, Bucket.QUERY, Exposure.PUBLIC),
            both(PacketChunkPosVeinID.class, PacketChunkPosVeinID::new, Bucket.QUERY, Exposure.inRange(() -> VEIN_QUERY_RADIUS)),
            s2c(PacketVeinEntries.class, PacketVeinEntries::new),
            s2c(PacketSyncConfig.class, PacketSyncConfig::new),
            c2s(PacketRequestClaimChunks.class, PacketRequestClaimChunks::new, Bucket.MAP, Exposure.inRange(PacketSecurity::claimMapRadius)),
            s2c(PacketClaimChunksData.class, PacketClaimChunksData::new),
            s2c(PacketClaimMapDelta.class, PacketClaimMapDelta::new),
            c2s(PacketClaimChunkAction.class, PacketClaimChunkAction::new, Bucket.ACTION, Exposure.inRange(PacketSecurity::claimRange)),
            c2s(PacketFactionMemberManagerAction.class, PacketFactionMemberManagerAction::new, Bucket.ACTION, Exposure.OWN_FACTION),
            c2s(PacketRequestMemberData.class, PacketRequestMemberData::new, Bucket.QUERY, Exposure.SELF),
            s2c(PacketMemberData.class, PacketMemberData::new),
            c2s(PacketRequestInsurance.class, PacketRequestInsurance::new, Bucket.QUERY, Exposure.OWN_FACTION),
            s2c(PacketInsuranceData.class, PacketInsuranceData::new),
            s2c(PacketBlockUiState.class, PacketBlockUiState::new),
            s2c(PacketJourneyMapVeins.class, PacketJourneyMapVeins::new),
            c2s(PacketDeclareSiege.class, PacketDeclareSiege::new, Bucket.ACTION, Exposure.ownFactionRole(Role.OFFICER)),
            c2s(PacketRequestTerrainColors.class, PacketRequestTerrainColors::new, Bucket.TERRAIN, Exposure.inRange(PacketSecurity::claimMapRadius)),
            s2c(PacketTerrainColors.class, PacketTerrainColors::new),
            c2s(PacketFactionAllianceAction.class, PacketFactionAllianceAction::new, Bucket.ACTION, Exposure.OWN_FACTION),
            c2s(PacketRequestFobWarp.class, PacketRequestFobWarp::new, Bucket.ACTION, Exposure.OWN_FACTION),
            c2s(PacketFobManagerAction.class, PacketFobManagerAction::new, Bucket.ACTION, Exposure.OWN_FACTION),
            c2s(PacketRequestOperations.class, PacketRequestOperations::new, Bucket.QUERY, Exposure.OWN_FACTION),
            s2c(PacketOperationsData.class, PacketOperationsData::new)
    );

    private static final Map<Class<?>, Entry<?>> BY_CLASS = new HashMap<>();

    static {
        for (Entry<?> entry : PACKETS) {
            BY_CLASS.put(entry.type(), entry);
        }
    }

    public static Entry<?> entryOf(Class<?> type) {
        Entry<?> entry = BY_CLASS.get(type);
        if (entry == null) {
            throw new IllegalArgumentException(type.getName() + " is not registered in WFNetwork.PACKETS");
        }
        return entry;
    }

    private static <T extends ServerboundPacket> Entry<T> c2s(Class<T> type, Supplier<T> factory, Bucket bucket, Exposure exposure) {
        return new Entry<>(idOf(type), type, factory, Direction.TO_SERVER, bucket, exposure);
    }

    private static <T extends PacketBase> Entry<T> s2c(Class<T> type, Supplier<T> factory) {
        return new Entry<>(idOf(type), type, factory, Direction.TO_CLIENT, Bucket.NONE, null);
    }

    private static <T extends ServerboundPacket> Entry<T> both(Class<T> type, Supplier<T> factory, Bucket bucket, Exposure exposure) {
        return new Entry<>(idOf(type), type, factory, Direction.BOTH, bucket, exposure);
    }

    private static String idOf(Class<?> type) {
        return type.getSimpleName().toLowerCase(Locale.ROOT);
    }


    public static void handle(PacketBase payload, boolean serverbound, @Nullable ServerPlayer sender,
                              @Nullable Player clientPlayer, Bucket bucket) {
        if (serverbound) {
            if (sender == null) {
                return;
            }
            if (!PacketSecurity.allow(sender, bucket, payload.getClass().getSimpleName())) {
                return;
            }
            if (!PacketVetting.vet(sender, (ServerboundPacket) payload, entryOf(payload.getClass()).exposure())) {
                return;
            }
            try {
                payload.handleServerSide(sender);
            } catch (RuntimeException e) {
                WarForgeMod.LOGGER.error("Error handling {} from {}", payload.getClass().getSimpleName(),
                        sender.getName().getString(), e);
            }
        } else {
            payload.handleClientSide(clientPlayer);
        }
    }
}
