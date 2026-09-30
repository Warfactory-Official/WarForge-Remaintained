package com.flansmod.warforge.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import com.flansmod.warforge.compat.Keys;

/**
 * A chunk position in a dimension. Has the ChunkPos methods the mod uses; {@link #toChunkPos()} gives the vanilla
 * position. (It does not extend ChunkPos, which is a final record from 26.1.)
 */
public class DimChunkPos
{
	public final int x;
	public final int z;
	public ResourceKey<Level> dim;

	public DimChunkPos(ResourceKey<Level> dim, int x, int z)
	{
		this.x = x;
		this.z = z;
		this.dim = dim;
	}

	public DimChunkPos(ResourceKey<Level> dim, BlockPos pos)
	{
		this(dim, pos.getX() >> 4, pos.getZ() >> 4);
	}

	public DimChunkPos(ResourceKey<Level> dim, ChunkPos pos)
	{
		this(dim, pos.getMinBlockX() >> 4, pos.getMinBlockZ() >> 4);
	}

	public ChunkPos toChunkPos()
	{
		return new ChunkPos(x, z);
	}

	public int getMinBlockX() { return x << 4; }
	public int getMinBlockZ() { return z << 4; }
	public int getMaxBlockX() { return (x << 4) + 15; }
	public int getMaxBlockZ() { return (z << 4) + 15; }
	public int getMiddleBlockX() { return (x << 4) + 7; }
	public int getMiddleBlockZ() { return (z << 4) + 7; }
	public int getBlockX(int offset) { return (x << 4) + offset; }
	public int getBlockZ(int offset) { return (z << 4) + offset; }
	public BlockPos getWorldPosition() { return new BlockPos(getMinBlockX(), 0, getMinBlockZ()); }
	public BlockPos getMiddleBlockPosition(int y) { return new BlockPos(getMiddleBlockX(), y, getMiddleBlockZ()); }
	public BlockPos getBlockAt(int bx, int y, int bz) { return new BlockPos(getBlockX(bx), y, getBlockZ(bz)); }
	public long toLong() { return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32; }
	public int getChessboardDistance(DimChunkPos other) { return Math.max(Math.abs(x - other.x), Math.abs(z - other.z)); }

	public boolean isSameDim(DimChunkPos other)
	{
		return other.dim.equals(dim);
	}

	@Override//Magic numbers ffs
	public int hashCode()
    {
		int i = 1664525 * x + 1013904223;
		int j = 1664525 * (z ^ -559038737) + 1013904223;
		return (i ^ j) ^ (155225 * this.dim.hashCode() + 140501023);
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
        return "[" + Keys.id(this.dim) + ": " + this.x + ", " + this.z + "]";
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
