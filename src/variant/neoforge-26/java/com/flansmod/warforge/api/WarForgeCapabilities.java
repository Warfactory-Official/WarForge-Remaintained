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
import org.jetbrains.annotations.Nullable;

public final class WarForgeCapabilities {
    public static final BlockCapability<IChunkReinforcer, Void> CHUNK_REINFORCER =
            BlockCapability.createVoid(
                    Ids.of(Tags.MODID, "chunk_reinforcer"),
                    IChunkReinforcer.class);

    private WarForgeCapabilities() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(WarForgeCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                Content.TE_ISLAND_COLLECTOR.get(),
                (be, side) -> (ExtractOnlyItemResources) be.getItemCapability());
    }

    public static @Nullable IChunkReinforcer chunkReinforcer(Level level, BlockEntity be) {
        return level.getCapability(CHUNK_REINFORCER, be.getBlockPos(), null);
    }

    public static void invalidate(BlockEntity be) {
        be.invalidateCapabilities();
    }
}
