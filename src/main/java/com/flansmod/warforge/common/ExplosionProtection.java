package com.flansmod.warforge.common;

import com.flansmod.warforge.common.WarForgeConfig.ProtectionConfig;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

public final class ExplosionProtection {

    private ExplosionProtection() {
    }

    public static UUID igniterId(Entity exploder) {
        if (exploder instanceof Player) {
            return exploder.getUUID();
        }
        return Faction.nullUuid;
    }

    public static boolean isProtected(Level level, UUID igniter, BlockPos pos) {
        if (level == null || level.isClientSide || pos == null) {
            return false;
        }
        try {
            DimBlockPos dimPos = new DimBlockPos(level.dimension(), pos);
            ProtectionConfig config = ProtectionsModule.GetProtections(igniter == null ? Faction.nullUuid : igniter, dimPos);
            return !config.EXPLOSION_DAMAGE || !config.BLOCK_REMOVAL;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void filter(Level level, UUID igniter, Collection<BlockPos> positions) {
        if (positions == null || positions.isEmpty() || level == null || level.isClientSide) {
            return;
        }
        Iterator<BlockPos> iterator = positions.iterator();
        while (iterator.hasNext()) {
            if (isProtected(level, igniter, iterator.next())) {
                iterator.remove();
            }
        }
    }

    public static void filter(Level level, Entity exploder, Collection<BlockPos> positions) {
        filter(level, igniterId(exploder), positions);
    }
}
