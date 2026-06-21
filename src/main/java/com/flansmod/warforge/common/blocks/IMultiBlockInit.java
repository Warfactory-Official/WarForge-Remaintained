package com.flansmod.warforge.common.blocks;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public interface IMultiBlockInit {
    public List<MultiBlockColumn> INSTANCES = new ArrayList<>();
    static public void registerMaps(){
        INSTANCES.forEach(IMultiBlockInit::initMap);
    }

    public void setUpMultiblock(Level world, BlockPos pos, BlockState state);

    public void initMap();

}
