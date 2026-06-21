package com.flansmod.warforge.common;

import com.flansmod.warforge.common.blocks.models.ClaimModels;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Mod-bus listener: ModelEvent.RegisterAdditional is fired on the mod event bus in 1.20.1, so this
// instance must be registered on the mod bus (FMLJavaModLoadingContext.get().getModEventBus()).
// Registering the statue locations here makes Forge bake them into the ModelManager natively;
// RenderTileEntityClaim then fetches the baked model per-frame.
@OnlyIn(Dist.CLIENT)
public class ModelEventHandler {

    @SubscribeEvent
    public void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation rl : ClaimModels.ADDITIONAL_MODELS) {
            event.register(rl);
        }
    }
}
