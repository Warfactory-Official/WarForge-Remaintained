package com.flansmod.warforge.common.network;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketSecurity.Bucket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration for the WarForge channel.
 *
 * <p>Every payload declares the one direction it may travel. NeoForge checks the registered flow
 * against the connection in {@code NetworkRegistry#getCodec} and refuses to decode a mismatch, so a
 * modified client can neither reach a server handler with a client-bound payload nor make the server
 * run a client-bound decoder (several of which size collections from the payload itself).
 *
 * <p>Client-to-server payloads also declare a {@link Bucket}: the per-player rate limit charged in
 * {@link #handle} before the handler runs, so no packet can be added without a cost attached.
 */
@EventBusSubscriber(modid = Tags.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class WFNetwork {
    private WFNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");

        c2s(r, PacketCreateFaction.TYPE, PacketCreateFaction.STREAM_CODEC, Bucket.ACTION);
        s2c(r, PacketOpenCreateFaction.TYPE, PacketOpenCreateFaction.STREAM_CODEC);
        s2c(r, PacketOpenFlagSelect.TYPE, PacketOpenFlagSelect.STREAM_CODEC);
        s2c(r, PacketOpenRecolour.TYPE, PacketOpenRecolour.STREAM_CODEC);
        c2s(r, PacketRequestFactionInfo.TYPE, PacketRequestFactionInfo.STREAM_CODEC, Bucket.QUERY);
        s2c(r, PacketFactionInfo.TYPE, PacketFactionInfo.STREAM_CODEC);
        s2c(r, PacketSiegeCampInfo.TYPE, PacketSiegeCampInfo.STREAM_CODEC);
        s2c(r, PacketSiegeCampProgressUpdate.TYPE, PacketSiegeCampProgressUpdate.STREAM_CODEC);
        c2s(r, PacketStartSiege.TYPE, PacketStartSiege.STREAM_CODEC, Bucket.ACTION);
        s2c(r, PacketLeaderboardInfo.TYPE, PacketLeaderboardInfo.STREAM_CODEC);
        c2s(r, PacketRequestLeaderboardInfo.TYPE, PacketRequestLeaderboardInfo.STREAM_CODEC, Bucket.QUERY);
        c2s(r, PacketDisbandFaction.TYPE, PacketDisbandFaction.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketRemoveClaim.TYPE, PacketRemoveClaim.STREAM_CODEC, Bucket.ACTION);
        s2c(r, PacketTimeUpdates.TYPE, PacketTimeUpdates.STREAM_CODEC);
        c2s(r, PacketSetFactionColour.TYPE, PacketSetFactionColour.STREAM_CODEC, Bucket.ACTION);
        s2c(r, PacketClientNotification.TYPE, PacketClientNotification.STREAM_CODEC);
        c2s(r, PacketMoveCitadel.TYPE, PacketMoveCitadel.STREAM_CODEC, Bucket.ACTION);
        s2c(r, PacketCitadelUpgradeRequirement.TYPE, PacketCitadelUpgradeRequirement.STREAM_CODEC);
        c2s(r, PacketRequestUpgradeUI.TYPE, PacketRequestUpgradeUI.STREAM_CODEC, Bucket.QUERY);
        s2c(r, PacketUpgradeUI.TYPE, PacketUpgradeUI.STREAM_CODEC);
        c2s(r, PacketRequestUpgrade.TYPE, PacketRequestUpgrade.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketChooseFactionFlag.TYPE, PacketChooseFactionFlag.STREAM_CODEC, Bucket.ACTION);
        s2c(r, PacketFlagManifest.TYPE, PacketFlagManifest.STREAM_CODEC);
        s2c(r, PacketFlagChunk.TYPE, PacketFlagChunk.STREAM_CODEC);
        s2c(r, PacketEffect.TYPE, PacketEffect.STREAM_CODEC);
        s2c(r, PacketNamePlateChange.TYPE, PacketNamePlateChange.STREAM_CODEC);
        c2s(r, PacketRequestNamePlate.TYPE, PacketRequestNamePlate.STREAM_CODEC, Bucket.QUERY);
        // Genuinely two-way: clients query a chunk's vein, the server answers with the same type.
        both(r, PacketChunkPosVeinID.TYPE, PacketChunkPosVeinID.STREAM_CODEC, Bucket.QUERY);
        s2c(r, PacketVeinEntries.TYPE, PacketVeinEntries.STREAM_CODEC);
        s2c(r, PacketSyncConfig.TYPE, PacketSyncConfig.STREAM_CODEC);
        c2s(r, PacketRequestClaimChunks.TYPE, PacketRequestClaimChunks.STREAM_CODEC, Bucket.MAP);
        s2c(r, PacketClaimChunksData.TYPE, PacketClaimChunksData.STREAM_CODEC);
        c2s(r, PacketClaimChunkAction.TYPE, PacketClaimChunkAction.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketFactionMemberManagerAction.TYPE, PacketFactionMemberManagerAction.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketRequestMemberData.TYPE, PacketRequestMemberData.STREAM_CODEC, Bucket.QUERY);
        s2c(r, PacketMemberData.TYPE, PacketMemberData.STREAM_CODEC);
        c2s(r, PacketFactionInsuranceAction.TYPE, PacketFactionInsuranceAction.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketRequestInsurance.TYPE, PacketRequestInsurance.STREAM_CODEC, Bucket.QUERY);
        s2c(r, PacketInsuranceData.TYPE, PacketInsuranceData.STREAM_CODEC);
        s2c(r, PacketJourneyMapClaims.TYPE, PacketJourneyMapClaims.STREAM_CODEC);
        s2c(r, PacketJourneyMapVeins.TYPE, PacketJourneyMapVeins.STREAM_CODEC);
        c2s(r, PacketDeclareSiege.TYPE, PacketDeclareSiege.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketRequestTerrainColors.TYPE, PacketRequestTerrainColors.STREAM_CODEC, Bucket.TERRAIN);
        s2c(r, PacketTerrainColors.TYPE, PacketTerrainColors.STREAM_CODEC);
        c2s(r, PacketFactionAllianceAction.TYPE, PacketFactionAllianceAction.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketEstablishFob.TYPE, PacketEstablishFob.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketRequestFobWarp.TYPE, PacketRequestFobWarp.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketFobManagerAction.TYPE, PacketFobManagerAction.STREAM_CODEC, Bucket.ACTION);
        c2s(r, PacketRequestOperations.TYPE, PacketRequestOperations.STREAM_CODEC, Bucket.QUERY);
        s2c(r, PacketOperationsData.TYPE, PacketOperationsData.STREAM_CODEC);
    }

    /** Client -> server only. NeoForge refuses to decode one arriving on a client. */
    private static <T extends PacketBase> void c2s(PayloadRegistrar r, CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec, Bucket bucket) {
        r.playToServer(type, codec, (payload, context) -> handle(payload, context, bucket));
    }

    /** Server -> client only. NeoForge refuses to decode one arriving on the server. */
    private static <T extends PacketBase> void s2c(PayloadRegistrar r, CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        r.playToClient(type, codec, (payload, context) -> handle(payload, context, Bucket.NONE));
    }

    /** Request/response payloads that legitimately travel both ways. */
    private static <T extends PacketBase> void both(PayloadRegistrar r, CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec, Bucket bucket) {
        r.playBidirectional(type, codec, (payload, context) -> handle(payload, context, bucket));
    }

    private static void handle(PacketBase payload, IPayloadContext context, Bucket bucket) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.SERVERBOUND) {
                // No player on the context means there is nobody to authorise the request against.
                if (!(context.player() instanceof ServerPlayer sender)) {
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
                payload.handleClientSide(context.player());
            }
        });
    }
}
