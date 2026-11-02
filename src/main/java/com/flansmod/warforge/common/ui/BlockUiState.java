package com.flansmod.warforge.common.ui;

import com.flansmod.warforge.compat.Nbt;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.BlockBasicClaim;
import com.flansmod.warforge.common.blocks.BlockCitadel;
import com.flansmod.warforge.common.blocks.BlockFob;
import com.flansmod.warforge.common.blocks.BlockIslandCollector;
import com.flansmod.warforge.common.blocks.BasicClaimBlockEntity;
import com.flansmod.warforge.common.blocks.CitadelBlockEntity;
import com.flansmod.warforge.common.blocks.FobBlockEntity;
import com.flansmod.warforge.common.blocks.IslandCollectorBlockEntity;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What decides a block UI's widget tree, captured on the server when the UI opens and carried to the client, so both
 * sides build the same tree although the client lacks server-only data (factions, unsynced config). Builders read
 * only this tag for structure; keys with no faction are absent.
 */
public final class BlockUiState {
    public static final String KIND = "kind";
    public static final String PRESENT = "present";
    public static final String HAS_FACTION = "hasFaction";
    public static final String STRIPE = "stripe";
    public static final String TITLE = "title";
    public static final String FLAG_ID = "flagId";
    public static final String UPGRADES = "upgrades";
    public static final String SLOTS = "slots";
    public static final String ESTABLISHED = "established";
    public static final String NAME = "name";
    public static final String TICKETS = "tickets";
    public static final String MAX_TICKETS = "maxTickets";
    public static final String CAN_WARP = "canWarp";
    public static final String FACTION_LABEL = "factionLabel";
    public static final String COLOUR = "colour";

    public static final String CITADEL = "citadel";
    public static final String CLAIM = "claim";
    public static final String FOB = "fob";
    public static final String COLLECTOR = "collector";

    @Nullable
    private static BlockPos receivedPos;
    @Nullable
    private static CompoundTag received;

    private BlockUiState() {
    }

    /** Server side: the display state of the block UI at pos, or null if the block has no UI. */
    @Nullable
    public static CompoundTag capture(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        BlockEntity be = level.getBlockEntity(pos);
        CompoundTag tag = new CompoundTag();
        if (block instanceof BlockCitadel) {
            tag.putString(KIND, CITADEL);
            if (be instanceof CitadelBlockEntity citadel) {
                boolean hasFaction = WarForgeMod.FACTIONS.registry.getFaction(citadel.getFaction()) != null;
                tag.putBoolean(PRESENT, true);
                tag.putBoolean(HAS_FACTION, hasFaction);
                tag.putInt(STRIPE, citadel.colour);
                tag.putString(TITLE, hasFaction ? citadel.getClaimDisplayName() : "");
                tag.putString(FLAG_ID, citadel.factionFlagId);
                tag.putBoolean(UPGRADES, WarForgeConfig.ENABLE_CITADEL_UPGRADES);
                tag.putInt(SLOTS, CitadelBlockEntity.NUM_YIELD_STACKS);
            }
            return tag;
        }
        if (block instanceof BlockBasicClaim) {
            tag.putString(KIND, CLAIM);
            if (be instanceof BasicClaimBlockEntity claim) {
                tag.putBoolean(PRESENT, true);
                tag.putString(TITLE, claim.getClaimDisplayName());
                tag.putInt(SLOTS, claim.getSlots());
            }
            return tag;
        }
        if (block instanceof BlockFob) {
            tag.putString(KIND, FOB);
            if (be instanceof FobBlockEntity fob) {
                Faction owner = WarForgeMod.FACTIONS.registry.getFaction(fob.ownerFaction);
                tag.putBoolean(PRESENT, true);
                tag.putBoolean(ESTABLISHED, !fob.ownerFaction.equals(FactionIds.NONE) && !fob.name.isEmpty());
                tag.putString(NAME, fob.name);
                tag.putInt(TICKETS, fob.tickets);
                tag.putInt(MAX_TICKETS, fob.maxTickets);
                tag.putBoolean(CAN_WARP, fob.tickets > 0);
                if (owner != null) {
                    tag.putInt(STRIPE, owner.getColour());
                }
            }
            return tag;
        }
        if (block instanceof BlockIslandCollector) {
            tag.putString(KIND, COLLECTOR);
            if (be instanceof IslandCollectorBlockEntity collector) {
                boolean hasFaction = !collector.getFaction().equals(FactionIds.NONE);
                tag.putBoolean(PRESENT, true);
                tag.putBoolean(HAS_FACTION, hasFaction);
                tag.putString(FACTION_LABEL, hasFaction ? collector.factionName : "");
                if (hasFaction) {
                    tag.putInt(COLOUR, collector.colour);
                }
                tag.putInt(SLOTS, collector.getStorageHandler().getSlots());
            }
            return tag;
        }
        return null;
    }

    /** Client side: the state the server sent for the UI at pos. */
    public static void receive(BlockPos pos, CompoundTag state) {
        receivedPos = pos;
        received = state;
    }

    /** Client side: the state received for pos; fails if the open packet did not precede the menu. */
    public static CompoundTag received(BlockPos pos) {
        if (received == null || !pos.equals(receivedPos)) {
            throw new IllegalStateException("No block UI state received for " + pos);
        }
        return received;
    }

    /** Colour of the left stripe: the faction colour, else the default. */
    public static int stripe(CompoundTag state, int defaultStripe) {
        return state.contains(STRIPE) ? Nbt.getInt(state, STRIPE) : defaultStripe;
    }
}
