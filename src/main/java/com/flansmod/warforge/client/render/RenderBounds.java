package com.flansmod.warforge.client.render;

import com.flansmod.warforge.common.blocks.TileEntityClaim;
import com.flansmod.warforge.common.blocks.TileEntityDummy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Render bounding boxes of block entities whose drawing reaches outside their block (poles, flags, beams).
 * NeoForge asks the renderer, Forge 1.20.1 asks the block entity; both use these boxes.
 */
public final class RenderBounds {
    private RenderBounds() {
    }

    /** The render box of a block entity, or null for the default (its block). */
    public static @Nullable AABB of(BlockEntity be) {
        BlockPos p = be.getBlockPos();
        if (be instanceof TileEntityClaim) {
            return new AABB(p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 2, p.getY() + 16, p.getZ() + 2);
        }
        if (be instanceof TileEntityDummy) {
            return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, 257, p.getZ() + 1);
        }
        return null;
    }
}
