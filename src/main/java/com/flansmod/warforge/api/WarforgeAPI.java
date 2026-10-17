package com.flansmod.warforge.api;

import com.flansmod.warforge.compat.Chunks;

import com.flansmod.warforge.api.interfaces.IChunkReinforcer;
import com.flansmod.warforge.api.interfaces.IClaimStrengthModifier;
import com.flansmod.warforge.api.vein.VeinWeightProvider;
import com.flansmod.warforge.common.ProtectionsModule;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.FactionStorage;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Public, addon-facing entry points for WarForge (2.1.0+). Lets other mods reason about a faction's
 * claimed and currently-loaded territory and the siege reinforcement around a besieged chunk.
 */
public final class WarforgeAPI {

    private WarforgeAPI() {}

    // ---------------------------------------------------------------------------------------------
    // Ownership
    //
    // These read the claim map only. Nothing here loads a chunk, and nothing here is a permission
    // check: knowing who owns a chunk does not tell you what you may do in it, because claim
    // ownership is overridden by siege state, neutral zones and alliance. For "may I build here",
    // use mayPlace / mayBreak below, which go through the same ProtectionsModule that actually
    // cancels the event.
    // ---------------------------------------------------------------------------------------------

    /**
     * The faction owning a chunk.
     *
     * <p>Returns {@code null} when the chunk is unclaimed, never {@link Faction#nullUuid}: the
     * sentinel the internal claim map uses reads as a real owner to a caller that has not seen it
     * before. The safe-zone and war-zone pseudo-factions are real owners and are returned as
     * themselves; test them with {@link #isNeutralZone(UUID)}.</p>
     */
    @Nullable
    public static UUID ownerOf(DimChunkPos pos) {
        if (pos == null) return null;
        UUID owner = WarForgeMod.FACTIONS.getClaim(pos);
        return owner == null || owner.equals(Faction.nullUuid) ? null : owner;
    }

    /** @see #ownerOf(DimChunkPos) */
    @Nullable
    public static UUID ownerOf(DimBlockPos pos) {
        return pos == null ? null : ownerOf(pos.toChunkPos());
    }

    /**
     * Owners of many chunks in one pass, which is the shape anything route- or region-shaped wants:
     * a long alignment crosses a thousand chunk columns and is re-queried on every edit.
     *
     * @return claimed chunks only, in iteration order. Unclaimed chunks are absent rather than
     *         mapped to null, so the result size is the count of claimed chunks.
     */
    public static Map<DimChunkPos, UUID> ownersAlong(Collection<DimChunkPos> chunks) {
        Map<DimChunkPos, UUID> out = new LinkedHashMap<>();
        if (chunks == null) return out;
        for (DimChunkPos pos : chunks) {
            UUID owner = ownerOf(pos);
            if (owner != null) out.put(pos, owner);
        }
        return out;
    }

    /**
     * What kind of claim covers a chunk. {@link Faction.ClaimType#NONE} when unclaimed, and
     * {@code ADMIN} or {@code WARZONE} for the two citadel-less neutral zones.
     */
    public static Faction.ClaimType claimTypeAt(DimChunkPos pos) {
        UUID owner = ownerOf(pos);
        if (owner == null) return Faction.ClaimType.NONE;
        Faction faction = WarForgeMod.FACTIONS.getFaction(owner);
        return faction == null ? Faction.ClaimType.NONE : faction.getClaimType(pos);
    }

    /** Whether a faction id is one of the two zone pseudo-factions rather than a player faction. */
    public static boolean isNeutralZone(@Nullable UUID factionId) {
        return factionId != null && FactionStorage.IsNeutralZone(factionId);
    }

    // ---------------------------------------------------------------------------------------------
    // Identity and standing
    //
    // Enough for an addon to decide who may see a thing, and to draw it in the owner's colours. Note
    // the deliberate split: alliance and truce are different relationships, and an addon hiding
    // information from enemies wants alliance, not a ceasefire.
    // ---------------------------------------------------------------------------------------------

    /** The faction a player belongs to, or null. */
    @Nullable
    public static UUID factionOfPlayer(@Nullable UUID playerId) {
        if (playerId == null || playerId.equals(Faction.nullUuid)) return null;
        Faction faction = WarForgeMod.FACTIONS.getFactionOfPlayer(playerId);
        return faction == null ? null : faction.uuid;
    }

    /** A faction's display name, or null when there is no such faction. */
    @Nullable
    public static String factionName(@Nullable UUID factionId) {
        Faction faction = factionOrNull(factionId);
        return faction == null ? null : faction.name;
    }

    /**
     * A faction's chosen colour as 0xRRGGBB.
     *
     * @return {@code fallback} when there is no such faction, so a caller drawing something always has
     *         a colour and never has to branch
     */
    public static int factionColour(@Nullable UUID factionId, int fallback) {
        Faction faction = factionOrNull(factionId);
        return faction == null ? fallback : (faction.colour & 0xFFFFFF);
    }

    /**
     * Whether two factions are allied, which includes a faction with itself.
     *
     * <p>Alliance only. A truce is a ceasefire between enemies and is not a reason to share anything,
     * so {@link #areAlliedOrTruced} is a separate question with a separate name.</p>
     *
     * <p>Checked in both directions. The set is documented as mutual, but a one-sided entry would
     * otherwise silently mean two players disagree about what each can see.</p>
     */
    public static boolean areAllied(@Nullable UUID a, @Nullable UUID b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        Faction fa = factionOrNull(a);
        Faction fb = factionOrNull(b);
        return (fa != null && fa.isAllyOf(b)) || (fb != null && fb.isAllyOf(a));
    }

    /** Allied, or holding a truce. The looser test, for things a ceasefire should cover. */
    public static boolean areAlliedOrTruced(@Nullable UUID a, @Nullable UUID b) {
        if (areAllied(a, b)) return true;
        Faction fa = factionOrNull(a);
        Faction fb = factionOrNull(b);
        return (fa != null && fa.isInTruceWith(b)) || (fb != null && fb.isInTruceWith(a));
    }

    /**
     * Whether a player may be shown something belonging to a faction.
     *
     * <p>The question an addon actually has when it is deciding what to send to a client. Unowned
     * content is nobody's to share, so it is not covered here: the caller decides what to do about
     * that, usually by showing it only to whoever made it.</p>
     */
    public static boolean maySeeFactionContent(@Nullable UUID viewerPlayerId, @Nullable UUID ownerFactionId) {
        if (ownerFactionId == null) return false;
        return areAllied(factionOfPlayer(viewerPlayerId), ownerFactionId);
    }

    @Nullable
    private static Faction factionOrNull(@Nullable UUID factionId) {
        if (factionId == null || factionId.equals(Faction.nullUuid)) return null;
        return WarForgeMod.FACTIONS.getFaction(factionId);
    }

    // ---------------------------------------------------------------------------------------------
    // Permission
    //
    // Thin wrappers over ProtectionsModule on purpose. It is what cancels BlockEvent.EntityPlaceEvent
    // and its sibling, so a tool that predicts permission any other way will eventually tell a player
    // they may build where they may not, and will drift the first time the protection config changes.
    // ---------------------------------------------------------------------------------------------

    /** Whether this player may place this block in this chunk, by the rule that governs the event. */
    public static boolean mayPlace(UUID playerId, DimChunkPos pos, Block block) {
        return !ProtectionsModule.placeDenied(ProtectionsModule.GetProtections(playerId, pos), block);
    }

    /** Whether this player may break this block in this chunk, by the rule that governs the event. */
    public static boolean mayBreak(UUID playerId, DimChunkPos pos, Block block) {
        return !ProtectionsModule.breakDenied(ProtectionsModule.GetProtections(playerId, pos), block);
    }

    // ---------------------------------------------------------------------------------------------
    // Territory
    // ---------------------------------------------------------------------------------------------

    /** Claimed chunks of a faction that are currently loaded on the server. */
    public static Set<DimChunkPos> getLoadedClaimedChunks(UUID factionId) {
        Set<DimChunkPos> out = new LinkedHashSet<>();
        Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (faction == null || server == null) return out;
        for (DimBlockPos claim : faction.claims.keySet()) {
            DimChunkPos cpos = claim.toChunkPos();
            if (out.contains(cpos)) continue;
            ServerLevel level = server.getLevel(cpos.dim);
            if (level != null && level.getChunkSource().getChunkNow(cpos.x, cpos.z) != null) {
                out.add(cpos);
            }
        }
        return out;
    }

    /**
     * Tests {@code predicate} against every {@link BlockEntity} in the faction's loaded, claimed chunks,
     * short-circuiting on the first match. Useful for "does this faction have X somewhere in its territory".
     */
    public static boolean anyLoadedClaimedTile(UUID factionId, Predicate<BlockEntity> predicate) {
        Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (faction == null || server == null) return false;
        Set<DimChunkPos> seen = new HashSet<>();
        for (DimBlockPos claim : faction.claims.keySet()) {
            DimChunkPos cpos = claim.toChunkPos();
            if (!seen.add(cpos)) continue;
            ServerLevel level = server.getLevel(cpos.dim);
            if (level == null) continue;
            LevelChunk chunk = level.getChunkSource().getChunkNow(cpos.x, cpos.z);
            if (chunk == null) continue;
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be != null && !be.isRemoved() && predicate.test(be)) return true;
            }
        }
        return false;
    }

    /**
     * Total extra siege difficulty reinforcing {@code besieged} from active reinforcers placed in the
     * faction's loaded claimed chunks. Non-stacking reinforcers (the default) contribute only their single
     * strongest bonus; stacking ones add on top. Honours both the {@link IChunkReinforcer} capability and the
     * legacy {@link IClaimStrengthModifier} interface.
     */
    public static int getReinforcementBonus(UUID factionId, DimChunkPos besieged) {
        Faction faction = WarForgeMod.FACTIONS.getFaction(factionId);
        MinecraftServer server = WarForgeMod.MC_SERVER;
        if (faction == null || server == null || besieged == null) return 0;

        int stackingSum = 0;
        int bestNonStacking = 0;
        Set<DimChunkPos> seen = new HashSet<>();
        for (DimBlockPos claim : faction.claims.keySet()) {
            DimChunkPos cpos = claim.toChunkPos();
            if (!cpos.dim.equals(besieged.dim) || !seen.add(cpos)) continue;
            ServerLevel level = server.getLevel(cpos.dim);
            if (level == null) continue;
            LevelChunk chunk = level.getChunkSource().getChunkNow(cpos.x, cpos.z);
            if (chunk == null) continue;

            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be == null || be.isRemoved()) continue;
                int bonus = 0;
                boolean stacks = false;

                IChunkReinforcer reinforcer = WarForgeCapabilities.chunkReinforcer(level, be);
                if (reinforcer != null) {
                    if (!reinforcer.isReinforcementActive()) continue;
                    ChunkPos rc = Chunks.of(be.getBlockPos());
                    int dist = Math.max(Math.abs(Chunks.x(rc) - besieged.x), Math.abs(Chunks.z(rc) - besieged.z));
                    if (dist > reinforcer.getReinforcementRadius()) continue;
                    bonus = reinforcer.getReinforcementBonus();
                    stacks = reinforcer.stacksWithOthers();
                } else if (be instanceof IClaimStrengthModifier legacy) {
                    if (!legacy.isActive()) continue;
                    ChunkPos rc = Chunks.of(be.getBlockPos());
                    boolean covers = false;
                    for (Vec3i vec : legacy.getEffectArea()) {
                        if (Chunks.x(rc) + vec.getX() == besieged.x && Chunks.z(rc) + vec.getZ() == besieged.z) {
                            covers = true;
                            break;
                        }
                    }
                    if (!covers) continue;
                    bonus = legacy.getClaimContribution();
                    stacks = legacy.canStack();
                } else {
                    continue;
                }

                if (bonus == 0) continue;
                if (stacks) stackingSum += bonus;
                else bestNonStacking = Math.max(bestNonStacking, bonus);
            }
        }
        return stackingSum + bestNonStacking;
    }

    // ---------------------------------------------------------------------------------------------
    // Veins
    // ---------------------------------------------------------------------------------------------

    @Nullable
    private static volatile VeinWeightProvider veinWeightProvider;

    /** Survives {@code /f vein reload}. Install before players join: JourneyMap AUTO rolls veins around them. */
    public static void setVeinWeightProvider(@Nullable VeinWeightProvider provider) {
        veinWeightProvider = provider;
    }

    @Nullable
    public static VeinWeightProvider veinWeightProvider() {
        return veinWeightProvider;
    }
}
