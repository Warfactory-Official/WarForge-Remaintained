package com.flansmod.warforge.common.potions;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.WarForgeConfig;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class PotionsModule
{
	private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Tags.MODID);
	private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, Tags.MODID);

	public final RegistryObject<MobEffect> tpRequest = EFFECTS.register("tprequest", PotionTpRequest::new);
	public final RegistryObject<MobEffect> tpAccept = EFFECTS.register("tpaccept", PotionTpAccept::new);

	public final RegistryObject<Potion> tpRequestPotionType = POTIONS.register("tprequestpotion",
		() -> new Potion(new MobEffectInstance(tpRequest.get(), 20 * 60)));
	public final RegistryObject<Potion> tpAcceptPotionType = POTIONS.register("tpacceptpotion",
		() -> new Potion(new MobEffectInstance(tpAccept.get(), 20 * 60)));

	public void register(IEventBus modBus)
	{
		if (!WarForgeConfig.ENABLE_TPA_POTIONS)
			return;

		EFFECTS.register(modBus);
		POTIONS.register(modBus);
	}

	public void preInit()
	{
	}

	public void registerBrewingRecipes()
	{
		if (!WarForgeConfig.ENABLE_TPA_POTIONS)
			return;

		Ingredient leaping = Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.LEAPING));
		BrewingRecipeRegistry.addRecipe(leaping, Ingredient.of(Items.ENDER_PEARL),
			PotionUtils.setPotion(new ItemStack(Items.POTION), tpRequestPotionType.get()));
		BrewingRecipeRegistry.addRecipe(leaping, Ingredient.of(Items.ENDER_EYE),
			PotionUtils.setPotion(new ItemStack(Items.POTION), tpAcceptPotionType.get()));
	}
}
