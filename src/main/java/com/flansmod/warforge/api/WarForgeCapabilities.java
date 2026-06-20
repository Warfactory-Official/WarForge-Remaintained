package com.flansmod.warforge.api;

import com.flansmod.warforge.api.interfaces.IChunkReinforcer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

/**
 * WarForge-provided Forge capabilities. Addons (e.g. WFCore) expose these from their tiles so WarForge can
 * query them generically without a compile-time dependency on the addon.
 */
public final class WarForgeCapabilities {

    @CapabilityInject(IChunkReinforcer.class)
    public static Capability<IChunkReinforcer> CHUNK_REINFORCER = null;

    private WarForgeCapabilities() {}

    /** Registered from {@code WarForgeMod.preInit}. */
    public static void register() {
        CapabilityManager.INSTANCE.register(IChunkReinforcer.class, new Capability.IStorage<IChunkReinforcer>() {
            @Override
            public NBTBase writeNBT(Capability<IChunkReinforcer> capability, IChunkReinforcer instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IChunkReinforcer> capability, IChunkReinforcer instance, EnumFacing side, NBTBase nbt) {}
        }, DefaultChunkReinforcer::new);
    }

    private static final class DefaultChunkReinforcer implements IChunkReinforcer {
        @Override public boolean isReinforcementActive() { return false; }
        @Override public int getReinforcementRadius() { return 0; }
        @Override public int getReinforcementBonus() { return 0; }
    }
}
