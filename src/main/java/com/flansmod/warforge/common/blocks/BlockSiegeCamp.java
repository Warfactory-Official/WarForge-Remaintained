package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.network.PacketSiegeCampInfo;
import com.flansmod.warforge.common.faction.ClaimType;
import com.flansmod.warforge.common.faction.FactionIds;
import com.flansmod.warforge.common.faction.Role;

import org.apache.commons.lang3.tuple.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRemoveClaim;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.server.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.flansmod.warforge.common.Content;
import static com.flansmod.warforge.common.Content.dummyTranslusent;
import static com.flansmod.warforge.common.Content.statue;
import static com.flansmod.warforge.common.WarForgeMod.FACTIONS;
import static com.flansmod.warforge.common.WarForgeMod.VEIN_HANDLER;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;
import static com.flansmod.warforge.common.blocks.BlockDummy.DummyModel.BERSERKER;
import static com.flansmod.warforge.common.blocks.BlockDummy.DummyModel.TRANSLUCENT;

public class BlockSiegeCamp extends MultiBlockColumn implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockSiegeCamp(BlockBehaviour.Properties properties) {
        super(properties
                .strength(-1.0F, 30000000.0F) //Makes sense. We probably don't wanna let people bomb it
                .noLootTable()
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SiegeCampBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Content.TE_SIEGE_CAMP.get()) return null;
        return (lvl, pos, st, be) -> ((SiegeCampBlockEntity) be).tick();
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
        if (!world.isClientSide()) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te instanceof SiegeCampBlockEntity siegeCamp) {
                FACTIONS.claimService.onNonCitadelClaimPlaced(siegeCamp, placer);
                FACTIONS.conquered.clear(new DimChunkPos(world.dimension(), pos));
                siegeCamp.onPlacedBy(placer);
                super.setPlacedBy(world, pos, state, placer, stack);
            }
        }
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!world.isClientSide()) return InteractionResult.SUCCESS;
            ClaimBlockEntity te = (ClaimBlockEntity) world.getBlockEntity(pos);
            PacketRemoveClaim packet = new PacketRemoveClaim();

            packet.pos = te.getClaimPos();

            WarForgeMod.NETWORK.sendToServer(packet);

            return InteractionResult.CONSUME;
        }

        if (!world.isClientSide()) {
            ClaimBlockEntity te = (ClaimBlockEntity) world.getBlockEntity(pos);
            Faction faction = FACTIONS.registry.getFaction(te.getFaction());
            if (faction == null) {
                player.sendSystemMessage(Component.translatable("warforge.common.camp_no_faction"));
                return InteractionResult.SUCCESS;
            }

            if (!faction.isPlayerRoleInFaction(player.getUUID(), Role.OFFICER)) {
                player.sendSystemMessage(Component.translatable("warforge.common.place.not_officer"));
                return InteractionResult.SUCCESS;
            }

            DimChunkPos chunkPos = new DimChunkPos(world.dimension(), pos);
            if (FACTIONS.sieges.isSiegeInProgress(chunkPos)) FACTIONS.siegeSync.sendAllInfoToNearby();
            PacketSiegeCampInfo.sendTo(
                    player,
                    new DimBlockPos(world.dimension(), pos),
                    calculatePossibleAttackDirections(world, pos, player),
                    faction.getSiegeMomentum(),
                    faction.getColour()
            );
        }

        return world.isClientSide() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }

    private List<SiegeCampAttackInfo> calculatePossibleAttackDirections(Level world, BlockPos pos, Player player) {
        List<SiegeCampAttackInfo> list = new ArrayList<>();

        SiegeCampBlockEntity siegeCamp = (SiegeCampBlockEntity) world.getBlockEntity(pos);
        if (siegeCamp == null) return list;

        int RADIUS = 2;

        UUID factionUUID = FACTIONS.registry.getFactionOfPlayer(player.getUUID()).getUuid();
        DimBlockPos siegePos = new DimBlockPos(world.dimension(), pos);
        var validTargets = FACTIONS.sieges.getClaimRadiusAround(factionUUID, siegePos, RADIUS);

        DimChunkPos siegeChunkPos = siegePos.toChunkPos();
        for (DimChunkPos chunk : new ArrayList<>(validTargets.keySet())) {
            int dx = chunk.x - siegeChunkPos.x;
            int dz = chunk.z - siegeChunkPos.z;

            Vec3i offset = new Vec3i(dx, 0, dz);

            Faction claimedBy = FACTIONS.registry.getFaction(FACTIONS.claims.getClaim(chunk));

            SiegeCampAttackInfo info = new SiegeCampAttackInfo();
            info.offset = offset;
            info.canAttack = (Math.abs(offset.getZ()) <= 1 && Math.abs(offset.getX()) <= 1) && validTargets.get(chunk);

            info.factionUuid = claimedBy == null ? FactionIds.NONE : claimedBy.getUuid();
            info.factionName = claimedBy == null ? "" : claimedBy.getName();
            info.factionColour = claimedBy == null ? 0 : claimedBy.getColour();
            info.claimType = claimedBy == null ? ClaimType.NONE : claimedBy.getClaimType(chunk);
            info.conquered = FACTIONS.conquered.isConquered(chunk);
            Pair<Vein, Quality> veinInfo = VEIN_HANDLER.getVein(chunk.dim, chunk.x, chunk.z,
                    WarForgeMod.MC_SERVER.overworld().getSeed());
            if (veinInfo != null) {
                info.warforgeVein = veinInfo.getLeft();
                info.oreQuality =  veinInfo.getRight();
            }

            list.add(info);
        }

        return list;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level worldIn, BlockPos pos, int id, int param) {
        return true;
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void initMap() {
        multiBlockMap = Collections.unmodifiableMap(new HashMap<>() {{
            put(statue.defaultBlockState().setValue(MODEL, BERSERKER), new Vec3i(0, 1, 0));
            put(dummyTranslusent.defaultBlockState().setValue(MODEL, TRANSLUCENT), new Vec3i(0, 2, 0));
        }});
    }
}
