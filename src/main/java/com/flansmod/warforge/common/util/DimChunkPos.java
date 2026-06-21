package com.flansmod.warforge.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class DimChunkPos extends ChunkPos
{
	public ResourceKey<Level> dim;

	public DimChunkPos(ResourceKey<Level> dim, int x, int z)
	{
		super(x, z);
		this.dim = dim;
	}

	public DimChunkPos(ResourceKey<Level> dim, BlockPos pos)
	{
		super(pos);
		this.dim = dim;
	}

	public boolean isSameDim(DimChunkPos other)
	{
		return other.dim.equals(dim);
	}

	@Override//Magic numbers ffs
	public int hashCode()
    {
		return super.hashCode() ^ (155225 * this.dim.hashCode() + 140501023);
    }

	@Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if (!(other instanceof DimChunkPos))
            return false;

        DimChunkPos dcpos = (DimChunkPos)other;
        return this.dim.equals(dcpos.dim) && this.x == dcpos.x && this.z == dcpos.z;
    }

	@Override
    public String toString()
    {
        return "[" + this.dim.location() + ": " + this.x + ", " + this.z + "]";
    }

	public DimChunkPos Offset(Direction facing, int n)
	{
	    return new DimChunkPos(dim, x + facing.getStepX() * n, z + facing.getStepZ() * n);
	}

	public DimChunkPos Offset(Vec3i offset)
	{
		return new DimChunkPos(dim, x + offset.getX(), z + offset.getZ());
	}

	public DimChunkPos north() { return Offset(Direction.NORTH, 1); }
	public DimChunkPos south() { return Offset(Direction.SOUTH, 1); }
	public DimChunkPos east() { return Offset(Direction.EAST, 1); }
	public DimChunkPos west() { return Offset(Direction.WEST, 1); }
}
