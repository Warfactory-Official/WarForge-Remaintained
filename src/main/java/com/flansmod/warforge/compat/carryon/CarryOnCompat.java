package com.flansmod.warforge.compat.carryon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;

/** Carry On queries; Carry On types stay inside {@link Access}, so nothing loads them when the mod is absent. */
public final class CarryOnCompat {
    private static final boolean LOADED = ModList.get().isLoaded("carryon");

    private CarryOnCompat() {}

    public static boolean isCarrying(Player player) {
        return LOADED && Access.isCarrying(player);
    }

    /** The block the player carries, null when none or Carry On is absent. */
    public static @Nullable Block carriedBlock(Player player) {
        return LOADED ? Access.carriedBlock(player) : null;
    }

    private static final class Access {
        static boolean isCarrying(Player player) {
            return CarryOnDataManager.getCarryData(player).isCarrying();
        }

        static @Nullable Block carriedBlock(Player player) {
            CarryOnData carry = CarryOnDataManager.getCarryData(player);
            return carry.isCarrying(CarryOnData.CarryType.BLOCK) ? carry.getBlock().getBlock() : null;
        }
    }
}
