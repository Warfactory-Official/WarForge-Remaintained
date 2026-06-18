package com.flansmod.warforge.common;

import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

public final class ExplosionProtection {

    private ExplosionProtection() {
    }

    public static UUID igniterId(Entity exploder) {
        if (exploder instanceof EntityPlayer) {
            return exploder.getUniqueID();
        }
        return Faction.nullUuid;
    }

    public static boolean isProtected(World world, UUID igniter, BlockPos pos) {
        if (world == null || world.isRemote || pos == null) {
            return false;
        }
        try {
            DimBlockPos dimPos = new DimBlockPos(world.provider.getDimension(), pos);
            ProtectionConfig config = ProtectionsModule.GetProtections(igniter == null ? Faction.nullUuid : igniter, dimPos);
            return !config.EXPLOSION_DAMAGE || !config.BLOCK_REMOVAL;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void filter(World world, UUID igniter, Collection<BlockPos> positions) {
        if (positions == null || positions.isEmpty() || world == null || world.isRemote) {
            return;
        }
        Iterator<BlockPos> iterator = positions.iterator();
        while (iterator.hasNext()) {
            if (isProtected(world, igniter, iterator.next())) {
                iterator.remove();
            }
        }
    }

    public static void filter(World world, Entity exploder, Collection<BlockPos> positions) {
        filter(world, igniterId(exploder), positions);
    }
}
