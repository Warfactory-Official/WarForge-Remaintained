package com.flansmod.warforge.client.render;

import com.flansmod.warforge.common.blocks.ClaimBlockEntity;
import com.flansmod.warforge.common.blocks.DummyBlockEntity;
import com.flansmod.warforge.compat.Levels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** NeoForge asks the renderer, Forge 1.20.1 asks the block entity; both use these boxes. */
public final class RenderBounds {
    private RenderBounds() {
    }

    public static @Nullable AABB of(BlockEntity be) {
        BlockPos p = be.getBlockPos();
        if (be instanceof ClaimBlockEntity) {
            return new AABB(p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 2, p.getY() + 16, p.getZ() + 2);
        }
        if (be instanceof DummyBlockEntity) {
            return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, Levels.maxYExclusive(be.getLevel()), p.getZ() + 1);
        }
        return null;
    }
}
