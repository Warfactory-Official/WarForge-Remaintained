package com.flansmod.warforge.client;

import com.flansmod.warforge.common.MineTime;
import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.ClaimChunkInfo;
import com.flansmod.warforge.common.network.SiegeCampProgressInfo;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionStorage;
import com.flansmod.warforge.server.Siege;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Client mirror of {@link ProtectionsModule}'s server BreakSpeed handler. The server stays
 * authoritative; this only predicts the MineTime slow-down so survival mining of a protected block
 * does not rubber-band. It never blocks a break outright — the client cannot perfectly resolve
 * ally/siege/defended zones, so a hard cancel here could be a false positive. Hard protection remains a
 * server-side {@code BlockEvent.BreakEvent} cancel; over-predicting a slow-down (and never a block) is
 * the only error this can make, and the server corrects it.
 */
public class ClientMineTimePredictor {

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (!level.isClientSide)
            return;
        if (player.getAbilities().instabuild)
            return;
        if (ProtectionsModule.OP_OVERRIDE && WarForgeMod.isOp(player))
            return;

        BlockPos blockPos = event.getPosition().orElse(null);
        if (blockPos == null)
            return;

        ChunkPos cp = new ChunkPos(blockPos);
        DimChunkPos chunkPos = new DimChunkPos(level.dimension(), cp.x, cp.z);

        // Siege zones override claim ownership (and may cover unclaimed land), so resolve them first
        // from the synced siege info. Without this, mining inside a Sieged zone (where the server lets
        // foes break freely) would mispredict as a denied claim and apply a phantom slow-down.
        ProtectionConfig config = siegeZoneFor(chunkPos);
        if (config == null) {
            ClaimChunkInfo info = ClientClaimChunkCache.get(chunkPos);
            if (info == null)
                info = ClientBorderCache.get(chunkPos);
            if (info == null)
                return; // unknown ownership: defer to the server (may briefly rubber-band)
            config = zoneFor(info);
        }

        Block block = event.getState().getBlock();
        if (!ProtectionsModule.breakDenied(config, block))
            return;

        MineTime.Rule rule = MineTime.resolve(block);
        if (rule == null)
            return; // hard-protected: leave to the server cancel
        if (event.getState().getDestroySpeed(level, blockPos) <= 0)
            return; // instant-break: cannot be paced

        event.setNewSpeed(MineTime.applySpeed(rule, event.getNewSpeed(), event.getState(), level, blockPos, player));
    }

    // Mirrors FactionStorage.getSiegeZone + the GetProtections siege branch from the synced siege info
    // (ClientProxy.sSiegeInfo). Returns the matching Sieged/War profile, or null if the chunk is in no
    // siege zone. Inner Sieged zone takes priority over the outer War zone across all known sieges.
    private static ProtectionConfig siegeZoneFor(DimChunkPos chunkPos) {
        SiegeCampProgressInfo warZone = null;
        for (SiegeCampProgressInfo si : ClientProxy.sSiegeInfo.values()) {
            if (si == null || si.attackingPos == null)
                continue;
            DimChunkPos camp = si.attackingPos.toChunkPos();
            if (Siege.isPlayerInRadius(camp, chunkPos, si.siegedRadius))
                return siegeProfile(true, isDefender(si));
            if (warZone == null && Siege.isPlayerInRadius(camp, chunkPos, si.battleRadius))
                warZone = si;
        }
        return warZone == null ? null : siegeProfile(false, isDefender(warZone));
    }

    private static boolean isDefender(SiegeCampProgressInfo si) {
        UUID pf = ClientClaimChunkCache.playerFactionId;
        return pf != null && !pf.equals(Faction.nullUuid)
                && si.defendingFactionId != null && pf.equals(si.defendingFactionId);
    }

    private static ProtectionConfig siegeProfile(boolean sieged, boolean defender) {
        if (sieged)
            return defender ? WarForgeConfig.SIEGED_FRIEND : WarForgeConfig.SIEGED_FOE;
        return defender ? WarForgeConfig.WAR_FRIEND : WarForgeConfig.WAR_FOE;
    }

    // Lightweight client-side equivalent of ProtectionsModule.GetProtections, limited to the zones whose
    // ownership the client can see. Ally / defended cases are unknown here and resolve to the foe
    // profile; that only ever over-predicts a slow-down (never a block), which the server then corrects.
    private static ProtectionConfig zoneFor(ClaimChunkInfo info) {
        UUID owner = info.factionId;
        if (owner == null || owner.equals(Faction.nullUuid))
            owner = info.outlineFactionId; // border-only entries carry ownership here
        if (owner == null || owner.equals(Faction.nullUuid))
            return WarForgeConfig.UNCLAIMED;
        if (owner.equals(FactionStorage.SAFE_ZONE_ID))
            return WarForgeConfig.SAFE_ZONE;
        if (owner.equals(FactionStorage.WAR_ZONE_ID))
            return WarForgeConfig.WAR_ZONE;

        boolean friend = !ClientClaimChunkCache.playerFactionId.equals(Faction.nullUuid)
                && owner.equals(ClientClaimChunkCache.playerFactionId);
        if (info.claimType == Faction.ClaimType.CITADEL)
            return friend ? WarForgeConfig.CITADEL_FRIEND : WarForgeConfig.CITADEL_FOE;
        return friend ? WarForgeConfig.CLAIM_FRIEND : WarForgeConfig.CLAIM_FOE;
    }
}
