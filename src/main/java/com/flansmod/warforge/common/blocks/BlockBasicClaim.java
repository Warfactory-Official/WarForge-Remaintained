package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.ClaimPlacementRules;
import com.flansmod.warforge.common.PlayerSyncService;
import com.flansmod.warforge.common.faction.FactionIds;

import com.flansmod.warforge.platform.WFUiBlock;

import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketFactionInfo;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import static com.flansmod.warforge.common.Content.dummyTranslusent;
import static com.flansmod.warforge.common.Content.statue;
import static com.flansmod.warforge.common.WarForgeMod.FACTIONS;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;
import static com.flansmod.warforge.common.blocks.BlockDummy.DummyModel.KING;
import static com.flansmod.warforge.common.blocks.BlockDummy.DummyModel.TRANSLUCENT;

public class BlockBasicClaim extends MultiBlockColumn implements EntityBlock, IMultiBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockBasicClaim(BlockBehaviour.Properties properties) {
        super(properties
                .strength(-1.0F, 30000000.0F)
                .noLootTable()
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (this == Content.basicClaimBlock)
            return new BasicClaimBlockEntity(pos, state);
        else
            return new ReinforcedClaimBlockEntity(pos, state);
    }

    public static DimChunkPos hasAdjacent(DimChunkPos sourcePos, Faction placingFaction) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            DimChunkPos adj = new DimChunkPos(sourcePos.dim, sourcePos.x + facing.getStepX(), sourcePos.z + facing.getStepZ());
            if (Objects.equals(FACTIONS.claims.getClaims().get(adj), placingFaction.getUuid())) { return adj; }
        }

        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        if (world instanceof Level level && !level.isClientSide()) {
            if (FACTIONS.sieges.isChunkContested(new DimChunkPos(level.dimension(), pos)))
                return false;

            UUID existingClaim = FACTIONS.claims.getClaim(new DimChunkPos(level.dimension(), pos));
            if (!existingClaim.equals(FactionIds.NONE))
                return false;
        }

        if (!world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP))
            return false;

        return super.canSurvive(state, world, pos);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        Direction facing = placer.getDirection().getOpposite();
        world.setBlock(pos, state.setValue(FACING, facing), 2);
        if (!world.isClientSide()) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof BasicClaimBlockEntity claim) {
                claim.onPlacedBy(placer);
                // setUpMultiblock deferred to ClaimBlockEntity.onServerSetFaction
                FACTIONS.claimService.onNonCitadelClaimPlaced(claim, placer);
                Faction owner = FACTIONS.registry.getFactionOfPlayer(placer.getUUID());
                if (owner != null && claim.getFaction().equals(owner.getUuid()) && placer instanceof Player player) {
                    ClaimPlacementRules.chargeExtraCost(player, owner);
                }
                world.removeBlock(pos, false);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            BasicClaimBlockEntity claim = (BasicClaimBlockEntity) world.getBlockEntity(pos);
            claim.increaseRotation();
            return world.isClientSide() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
        }
        if (!world.isClientSide()) {
            Faction playerFaction = FACTIONS.registry.getFactionOfPlayer(player.getUUID());
            BasicClaimBlockEntity claimTE = (BasicClaimBlockEntity) world.getBlockEntity(pos);

            if (playerFaction == null || !playerFaction.getUuid().equals(claimTE.factionUUID)) {
                Faction citadelFaction = FACTIONS.registry.getFaction(claimTE.factionUUID);
                if (citadelFaction != null) {
                    PacketFactionInfo packet = new PacketFactionInfo();
                    packet.info = citadelFaction.createInfo(((ServerPlayer) player).getUUID());
                    WarForgeMod.NETWORK.sendTo(packet, (ServerPlayer) player);
                } else {
                    player.sendSystemMessage(Component.translatable("warforge.common.claim_no_faction"));
                }
            }
            else if (player instanceof ServerPlayer serverPlayer) {
                PlayerSyncService.syncClaimToPlayer(player, pos);
                WFUiBlock.openUi(serverPlayer, pos);
            }
        }
        return world.isClientSide() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }

    

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    protected void onRemoved(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity blockentity = worldIn.getBlockEntity(pos);

        if (blockentity instanceof IItemHandler handler) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty())
                    Block.popResource(worldIn, pos, stack);
            }
            worldIn.updateNeighbourForOutputSignal(pos, this);
        }

        super.onRemoved(state, worldIn, pos, newState, isMoving);
    }

    @Override
    public void initMap() {
        multiBlockMap = Collections.unmodifiableMap(new HashMap<>() {{
            put(statue.defaultBlockState().setValue(MODEL, KING), new Vec3i(0, 1, 0));
            put(dummyTranslusent.defaultBlockState().setValue(MODEL, TRANSLUCENT), new Vec3i(0, 2, 0));
        }});
    }
}
