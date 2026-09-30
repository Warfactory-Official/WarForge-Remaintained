package com.flansmod.warforge.api;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.interfaces.IChunkReinforcer;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.platform.ExtractOnlyItemResources;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * WarForge-provided capabilities. Addons (e.g. WFCore) expose these from their tiles so WarForge can
 * query them generically without a compile-time dependency on the addon.
 */
public final class WarForgeCapabilities {

    public static final BlockCapability<IChunkReinforcer, Void> CHUNK_REINFORCER =
            BlockCapability.createVoid(
                    Ids.of(Tags.MODID, "chunk_reinforcer"),
                    IChunkReinforcer.class);

    /**
     * One adapter per pull-only view, so that every query in a transaction sees the same pending extractions. The
     * values are weak too (an adapter holds its view): an adapter in use is kept alive by its transaction.
     */
    private static final Map<IItemHandler, WeakReference<ExtractOnlyItemResources>> ITEM_RESOURCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private WarForgeCapabilities() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(WarForgeCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                Content.TE_ISLAND_COLLECTOR.get(),
                (be, side) -> itemResources(be.getPullOnlyView()));
    }

    private static ExtractOnlyItemResources itemResources(IItemHandler view) {
        synchronized (ITEM_RESOURCES) {
            WeakReference<ExtractOnlyItemResources> ref = ITEM_RESOURCES.get(view);
            ExtractOnlyItemResources resources = ref == null ? null : ref.get();
            if (resources == null) {
                resources = new ExtractOnlyItemResources(view);
                ITEM_RESOURCES.put(view, new WeakReference<>(resources));
            }
            return resources;
        }
    }

    /** The chunk reinforcer a block entity exposes, or null. */
    public static @Nullable IChunkReinforcer chunkReinforcer(Level level, BlockEntity be) {
        return level.getCapability(CHUNK_REINFORCER, be.getBlockPos(), null);
    }

    /** Tells the capability system that the capabilities of a block entity changed. */
    public static void invalidate(BlockEntity be) {
        be.invalidateCapabilities();
    }
}
