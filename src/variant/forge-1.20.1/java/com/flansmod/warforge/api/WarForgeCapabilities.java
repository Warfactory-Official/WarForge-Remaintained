package com.flansmod.warforge.api;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.api.interfaces.IChunkReinforcer;
import com.flansmod.warforge.common.blocks.TileEntityIslandCollector;
import com.flansmod.warforge.compat.Ids;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WarForge-provided capabilities. Addons (e.g. WFCore) expose these from their tiles so WarForge can
 * query them generically without a compile-time dependency on the addon.
 */
public final class WarForgeCapabilities {

    public static final Capability<IChunkReinforcer> CHUNK_REINFORCER = CapabilityManager.get(new CapabilityToken<>() {});

    private WarForgeCapabilities() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterCapabilitiesEvent event) -> event.register(IChunkReinforcer.class));
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, WarForgeCapabilities::attach);
    }

    /** Hoppers and pipes may only pull from the island collector (its pull-only item view). */
    private static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof TileEntityIslandCollector collector)) {
            return;
        }
        PullOnlyProvider provider = new PullOnlyProvider(collector);
        event.addCapability(Ids.of(Tags.MODID, "pull_only_items"), provider);
        event.addListener(provider::invalidate);
    }

    /** The chunk reinforcer a block entity exposes, or null. */
    public static @Nullable IChunkReinforcer chunkReinforcer(Level level, BlockEntity be) {
        return be.getCapability(CHUNK_REINFORCER).resolve().orElse(null);
    }

    /** Tells the capability system that the capabilities of a block entity changed. */
    public static void invalidate(BlockEntity be) {
        be.invalidateCaps();
        be.reviveCaps();
    }

    private static final class PullOnlyProvider implements ICapabilityProvider {
        private final TileEntityIslandCollector collector;
        private LazyOptional<IItemHandler> handler = LazyOptional.empty();

        private PullOnlyProvider(TileEntityIslandCollector collector) {
            this.collector = collector;
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap != ForgeCapabilities.ITEM_HANDLER) {
                return LazyOptional.empty();
            }
            if (!handler.isPresent()) {
                handler = LazyOptional.of(collector::getPullOnlyView);
            }
            return handler.cast();
        }

        void invalidate() {
            handler.invalidate();
        }
    }
}
