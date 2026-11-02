package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.ui.BlockUiActions;
import com.flansmod.warforge.common.faction.FactionIds;


import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static com.flansmod.warforge.common.Content.dummyTranslusent;
import static com.flansmod.warforge.common.Content.statue;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;
import static com.flansmod.warforge.common.blocks.BlockDummy.DummyModel.KING;
import static com.flansmod.warforge.common.blocks.BlockDummy.DummyModel.TRANSLUCENT;

public class BlockCitadel extends MultiBlockColumn implements EntityBlock, IMultiBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockCitadel(BlockBehaviour.Properties properties) {
        super(properties
                .strength(-1.0F, 30000000.0F)
                .noLootTable());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public void initMap() {
        multiBlockMap = Collections.unmodifiableMap(new HashMap<>() {{
            put(statue.defaultBlockState().setValue(MODEL, KING), new Vec3i(0, 1, 0));
            put(dummyTranslusent.defaultBlockState().setValue(MODEL, TRANSLUCENT), new Vec3i(0, 2, 0));
        }});
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CitadelBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction facing = placer.getDirection().getOpposite();
        world.setBlock(pos, state.setValue(FACING, facing), 2);
        BlockEntity te = world.getBlockEntity(pos);
        if (te != null) {
            CitadelBlockEntity citadel = (CitadelBlockEntity) te;
            citadel.onPlacedBy(placer);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (pos.getY() < WarForgeConfig.CITADEL_MIN_Y) {
            if (!world.isClientSide() && context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.translatable("warforge.common.citadel_min_y", WarForgeConfig.CITADEL_MIN_Y));
            }
            return null;
        }

        if (!world.isClientSide()) {
            if (WarForgeMod.FACTIONS.sieges.isChunkContested(new DimChunkPos(world.dimension(), pos)))
                return null;

            UUID existingClaim = WarForgeMod.FACTIONS.claims.getClaim(new DimChunkPos(world.dimension(), pos));
            if (!existingClaim.equals(FactionIds.NONE))
                return null;
        }

        if (!world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP))
            return null;

        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            ClaimBlockEntity citadel = (ClaimBlockEntity) world.getBlockEntity(pos);
            if (!citadel.getFaction().equals(FactionIds.NONE))
                citadel.increaseRotation();
            else if (!world.isClientSide()) {
                onRemoved(state, world, pos, state, false);
                world.destroyBlock(pos, true);
            }
            return world.isClientSide() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
        }
        if (!world.isClientSide()) {
            Faction playerFaction = WarForgeMod.FACTIONS.registry.getFactionOfPlayer(player.getUUID());
            CitadelBlockEntity citadel = (CitadelBlockEntity) world.getBlockEntity(pos);

            if (playerFaction == null && player.getUUID().equals(citadel.placer)) {
                BlockUiActions.openClaimBlock(player, citadel.getBlockPos());
            }
            else if (playerFaction == null || !playerFaction.getUuid().equals(citadel.factionUUID)) {
                Faction citadelFaction = WarForgeMod.FACTIONS.registry.getFaction(citadel.factionUUID);
                if (citadelFaction != null) {
                    BlockUiActions.openClaimBlock(player, citadel.getBlockPos());
                } else {
                    DimBlockPos citadelPos = new DimBlockPos(world.dimension(), pos);
                    Faction chunkFaction = WarForgeMod.FACTIONS.registry.getFaction(WarForgeMod.FACTIONS.claims.getClaim(citadelPos.toChunkPos()));
                    if (chunkFaction != null) {
                        world.destroyBlock(pos, false);
                        player.sendSystemMessage(Component.translatable("warforge.common.citadel_overlap"));
                    } else {
                        player.sendSystemMessage(Component.translatable("warforge.common.citadel_unowned"));
                    }
                }
            }
            else {
                BlockUiActions.openClaimBlock(player, citadel.getBlockPos());
            }
        }
        return world.isClientSide() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }
}
