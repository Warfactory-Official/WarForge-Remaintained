package com.flansmod.warforge.common;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Set;

/** Exact blocks plus block tags. Tags bind at server start, so they are tested per query. */
public record BlockSet(Set<Block> blocks, List<TagKey<Block>> tags) {
    public boolean contains(Block block) {
        if (blocks.contains(block)) {
            return true;
        }
        for (TagKey<Block> tag : tags) {
            if (block.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        return false;
    }
}
