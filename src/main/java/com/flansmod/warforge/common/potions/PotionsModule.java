package com.flansmod.warforge.common.potions;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeConfig;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import com.flansmod.warforge.compat.Brewing;
import com.flansmod.warforge.compat.Effects;
import com.flansmod.warforge.compat.Ids;
import com.flansmod.warforge.platform.RegHandle;
import com.flansmod.warforge.platform.WFRegistry;
import net.minecraft.resources.ResourceKey;

public class PotionsModule
{
	private static final WFRegistry REG = new WFRegistry();
	private static final ResourceKey<Potion> LEAPING = ResourceKey.create(Registries.POTION, Ids.of("minecraft", "leaping"));

	public final RegHandle<MobEffect> tpRequest = REG.register(Registries.MOB_EFFECT, "tprequest", PotionTpRequest::new);
	public final RegHandle<MobEffect> tpAccept = REG.register(Registries.MOB_EFFECT, "tpaccept", PotionTpAccept::new);

	// The potion name is the registry path, which is also what vanilla uses when a potion has no name.
	public final RegHandle<Potion> tpRequestPotionType = REG.register(Registries.POTION, "tprequestpotion",
		() -> new Potion("tprequestpotion", Effects.instance(tpRequest, 20 * 60)));
	public final RegHandle<Potion> tpAcceptPotionType = REG.register(Registries.POTION, "tpacceptpotion",
		() -> new Potion("tpacceptpotion", Effects.instance(tpAccept, 20 * 60)));

	public void register(IEventBus modBus)
	{
		if (!WarForgeConfig.ENABLE_TPA_POTIONS)
			return;

		REG.register(modBus);
		Brewing.register(this::registerBrewingRecipes);
	}

	public void preInit()
	{
	}

	private void registerBrewingRecipes(Brewing.Mixer mixer)
	{
		if (!WarForgeConfig.ENABLE_TPA_POTIONS)
			return;

		mixer.addMix(LEAPING, Items.ENDER_PEARL, tpRequestPotionType);
		mixer.addMix(LEAPING, Items.ENDER_EYE, tpAcceptPotionType);
	}
}
