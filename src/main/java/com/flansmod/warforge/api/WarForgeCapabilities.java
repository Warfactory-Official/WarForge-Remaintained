package com.flansmod.warforge.api;

import com.flansmod.warforge.api.interfaces.IChunkReinforcer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

/**
 * WarForge-provided Forge capabilities. Addons (e.g. WFCore) expose these from their tiles so WarForge can
 * query them generically without a compile-time dependency on the addon.
 */
public final class WarForgeCapabilities {

    public static final Capability<IChunkReinforcer> CHUNK_REINFORCER =
            CapabilityManager.get(new CapabilityToken<>() {});

    private WarForgeCapabilities() {}

    /** Registered from a {@link RegisterCapabilitiesEvent} handler. */
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IChunkReinforcer.class);
    }
}
