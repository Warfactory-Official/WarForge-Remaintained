package com.flansmod.warforge.common;

import com.flansmod.warforge.Tags;
import com.flansmod.warforge.common.blocks.*;
import com.flansmod.warforge.server.Leaderboard.FactionStat;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.flansmod.warforge.platform.RegHandle;
import com.flansmod.warforge.platform.WFRegistry;
import net.neoforged.bus.api.IEventBus;

public class Content
{
	private static final WFRegistry REG = new WFRegistry();

	// Particles
	public static final RegHandle<SimpleParticleType> STAR_CIRCLE_PARTICLE =
			REG.register(Registries.PARTICLE_TYPE, "star_circle", () -> new SimpleParticleType(false));

	// Blocks
	public static final RegHandle<Block> CITADEL_BLOCK = REG.block("citadelblock", BlockCitadel::new);
	public static final RegHandle<Block> BASIC_CLAIM_BLOCK = REG.block("basicclaimblock", BlockBasicClaim::new);
	public static final RegHandle<Block> REINFORCED_CLAIM_BLOCK = REG.block("reinforcedclaimblock", BlockBasicClaim::new);
	public static final RegHandle<Block> SIEGE_CAMP_BLOCK = REG.block("siegecampblock", BlockSiegeCamp::new);
	public static final RegHandle<Block> ISLAND_COLLECTOR_BLOCK = REG.block("islandcollector", BlockIslandCollector::new);
	public static final RegHandle<Block> STATUE = REG.block("dummy", BlockDummy::new);
	public static final RegHandle<Block> DUMMY_TRANSLUSENT = REG.block("dummy_translusent", BlockDummyTransparent::new);
	public static final RegHandle<Block> TOP_LEADERBOARD_BLOCK = REG.block("topleaderboard", props -> new BlockLeaderboard(FactionStat.TOTAL, props));
	public static final RegHandle<Block> WEALTH_LEADERBOARD_BLOCK = REG.block("wealthleaderboard", props -> new BlockLeaderboard(FactionStat.WEALTH, props));
	public static final RegHandle<Block> NOTORIETY_LEADERBOARD_BLOCK = REG.block("notorietyleaderboard", props -> new BlockLeaderboard(FactionStat.NOTORIETY, props));
	public static final RegHandle<Block> LEGACY_LEADERBOARD_BLOCK = REG.block("legacyleaderboard", props -> new BlockLeaderboard(FactionStat.LEGACY, props));
	public static final RegHandle<Block> FOB_BLOCK = REG.block("fobblock", BlockFob::new);

	// Block items (statue/dummyTranslusent intentionally have no item)
	public static final RegHandle<Item> CITADEL_BLOCK_ITEM = REG.blockItem("citadelblock", CITADEL_BLOCK);
	public static final RegHandle<Item> BASIC_CLAIM_BLOCK_ITEM = REG.blockItem("basicclaimblock", BASIC_CLAIM_BLOCK);
	public static final RegHandle<Item> REINFORCED_CLAIM_BLOCK_ITEM = REG.blockItem("reinforcedclaimblock", REINFORCED_CLAIM_BLOCK);
	public static final RegHandle<Item> SIEGE_CAMP_BLOCK_ITEM = REG.blockItem("siegecampblock", SIEGE_CAMP_BLOCK);
	public static final RegHandle<Item> ISLAND_COLLECTOR_ITEM = REG.blockItem("islandcollector", ISLAND_COLLECTOR_BLOCK);
	public static final RegHandle<Item> TOP_LEADERBOARD_ITEM = REG.blockItem("topleaderboard", TOP_LEADERBOARD_BLOCK);
	public static final RegHandle<Item> WEALTH_LEADERBOARD_ITEM = REG.blockItem("wealthleaderboard", WEALTH_LEADERBOARD_BLOCK);
	public static final RegHandle<Item> NOTORIETY_LEADERBOARD_ITEM = REG.blockItem("notorietyleaderboard", NOTORIETY_LEADERBOARD_BLOCK);
	public static final RegHandle<Item> LEGACY_LEADERBOARD_ITEM = REG.blockItem("legacyleaderboard", LEGACY_LEADERBOARD_BLOCK);
	public static final RegHandle<Item> FOB_BLOCK_ITEM = REG.blockItem("fobblock", FOB_BLOCK);

	// Block entities. Names referenced by the BlockEntity classes via Content.TE_*.get().
	public static final RegHandle<BlockEntityType<TileEntityCitadel>> TE_CITADEL =
			REG.blockEntity("citadel", TileEntityCitadel::new,
					CITADEL_BLOCK);
	public static final RegHandle<BlockEntityType<TileEntityBasicClaim>> TE_BASIC_CLAIM =
			REG.blockEntity("basicclaim", TileEntityBasicClaim::new,
					BASIC_CLAIM_BLOCK);
	public static final RegHandle<BlockEntityType<TileEntityReinforcedClaim>> TE_REINFORCED_CLAIM =
			REG.blockEntity("reinforcedclaim", TileEntityReinforcedClaim::new,
					REINFORCED_CLAIM_BLOCK);
	public static final RegHandle<BlockEntityType<TileEntitySiegeCamp>> TE_SIEGE_CAMP =
			REG.blockEntity("siegecamp", TileEntitySiegeCamp::new,
					SIEGE_CAMP_BLOCK);
	public static final RegHandle<BlockEntityType<TileEntityIslandCollector>> TE_ISLAND_COLLECTOR =
			REG.blockEntity("islandcollector", TileEntityIslandCollector::new,
					ISLAND_COLLECTOR_BLOCK);
	public static final RegHandle<BlockEntityType<TileEntityLeaderboard>> TE_LEADERBOARD =
			REG.blockEntity("leaderboard", TileEntityLeaderboard::new,
					TOP_LEADERBOARD_BLOCK, WEALTH_LEADERBOARD_BLOCK, NOTORIETY_LEADERBOARD_BLOCK, LEGACY_LEADERBOARD_BLOCK);
	public static final RegHandle<BlockEntityType<TileEntityDummy>> TE_DUMMY =
			REG.blockEntity("tileentity_dummy", TileEntityDummy::new,
					STATUE, DUMMY_TRANSLUSENT);
	public static final RegHandle<BlockEntityType<TileEntityFob>> TE_FOB =
			REG.blockEntity("fob", TileEntityFob::new,
					FOB_BLOCK);

	// Creative tab
	public static final RegHandle<CreativeModeTab> TAB = REG.register(Registries.CREATIVE_MODE_TAB, "main",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup." + Tags.MODID))
					.icon(() -> new ItemStack(CITADEL_BLOCK.get()))
					.displayItems((params, output) -> {
						output.accept(CITADEL_BLOCK_ITEM.get());
						output.accept(BASIC_CLAIM_BLOCK_ITEM.get());
						output.accept(REINFORCED_CLAIM_BLOCK_ITEM.get());
						output.accept(SIEGE_CAMP_BLOCK_ITEM.get());
						output.accept(ISLAND_COLLECTOR_ITEM.get());
						output.accept(TOP_LEADERBOARD_ITEM.get());
						output.accept(WEALTH_LEADERBOARD_ITEM.get());
						output.accept(NOTORIETY_LEADERBOARD_ITEM.get());
						output.accept(LEGACY_LEADERBOARD_ITEM.get());
						output.accept(FOB_BLOCK_ITEM.get());
					})
					.build());

	// Plain fields the rest of the mod reads directly. Populated by bake() after registration.
	public static Block citadelBlock, basicClaimBlock, reinforcedClaimBlock, siegeCampBlock, statue, dummyTranslusent;
	public static Item citadelBlockItem, basicClaimBlockItem, reinforcedClaimBlockItem, siegeCampBlockItem;
	public static Block islandCollectorBlock;
	public static Item islandCollectorItem;

	public static Block topLeaderboardBlock, notorietyLeaderboardBlock, wealthLeaderboardBlock, legacyLeaderboardBlock;
	public static Item topLeaderboardItem, notorietyLeaderboardItem, wealthLeaderboardItem, legacyLeaderboardItem;

	public static void register(IEventBus modBus)
	{
		REG.register(modBus);
	}

	// Resolve the plain fields from the RegistryObjects once registration has run.
	public static void bake()
	{
		citadelBlock = CITADEL_BLOCK.get();
		basicClaimBlock = BASIC_CLAIM_BLOCK.get();
		reinforcedClaimBlock = REINFORCED_CLAIM_BLOCK.get();
		siegeCampBlock = SIEGE_CAMP_BLOCK.get();
		islandCollectorBlock = ISLAND_COLLECTOR_BLOCK.get();
		statue = STATUE.get();
		dummyTranslusent = DUMMY_TRANSLUSENT.get();
		topLeaderboardBlock = TOP_LEADERBOARD_BLOCK.get();
		wealthLeaderboardBlock = WEALTH_LEADERBOARD_BLOCK.get();
		notorietyLeaderboardBlock = NOTORIETY_LEADERBOARD_BLOCK.get();
		legacyLeaderboardBlock = LEGACY_LEADERBOARD_BLOCK.get();

		citadelBlockItem = CITADEL_BLOCK_ITEM.get();
		basicClaimBlockItem = BASIC_CLAIM_BLOCK_ITEM.get();
		reinforcedClaimBlockItem = REINFORCED_CLAIM_BLOCK_ITEM.get();
		siegeCampBlockItem = SIEGE_CAMP_BLOCK_ITEM.get();
		islandCollectorItem = ISLAND_COLLECTOR_ITEM.get();
		topLeaderboardItem = TOP_LEADERBOARD_ITEM.get();
		wealthLeaderboardItem = WEALTH_LEADERBOARD_ITEM.get();
		notorietyLeaderboardItem = NOTORIETY_LEADERBOARD_ITEM.get();
		legacyLeaderboardItem = LEGACY_LEADERBOARD_ITEM.get();

		WarForgeMod.LOGGER.info("Registered WarForge content");
	}

	// Retained for the existing CONTENT.preInit() call site in WarForgeMod.
	public void preInit() {}
}
