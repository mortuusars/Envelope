package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.clientbound.ClientboundOpenLetterBlockViewScreenPacket;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LetterBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty TATTERED = BooleanProperty.create("tattered");
    public static final BooleanProperty HAS_CONTENT = BooleanProperty.create("has_content");

    public static final VoxelShape SHAPE_NORTH = Block.box(3, 2, 15, 13, 14, 16);
    public static final VoxelShape SHAPE_EAST = Block.box(0, 2, 3, 1, 14, 13);
    public static final VoxelShape SHAPE_SOUTH = Block.box(3, 2, 0, 13, 14, 1);
    public static final VoxelShape SHAPE_WEST = Block.box(15, 2, 3, 16, 14, 13);

    public LetterBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
              .setValue(FACING, Direction.NORTH)
              .setValue(TATTERED, false)
              .setValue(HAS_CONTENT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TATTERED, HAS_CONTENT);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (state.getValue(TATTERED)) stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
        return stack;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter pLevel,
                                        @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> Shapes.block();
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        return facing.getAxis().isHorizontal()
              ? getStateDefinition().any()
              .setValue(FACING, facing)
              .setValue(TATTERED, context.getItemInHand().get(Envelope.DataComponents.LETTER_TATTERED) != null)
              .setValue(HAS_CONTENT, !context.getItemInHand()
                    .getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY).isEmpty())
              : null;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return Block.canSupportCenter(level, pos.relative(facing.getOpposite()), facing);
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = rotation.rotate(state.getValue(FACING));
        return facing.getAxis().isHorizontal()
              ? state.setValue(FACING, facing)
              : state;
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LetterBlockEntity(Envelope.BlockEntityTypes.LETTER.get(), pos, state);
    }

    // --

    @Override
    protected @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos)
              ? Blocks.AIR.defaultBlockState()
              : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof LetterBlockEntity blockEntity) {
            blockEntity.setLetter(stack.copyWithCount(1));
            level.blockUpdated(pos, state.getBlock()); // Force block to update
        }
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LetterBlockEntity blockEntity) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), blockEntity.getLetter(null));
            blockEntity.setLetter(ItemStack.EMPTY);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // --

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof LetterBlockEntity blockEntity
              && player instanceof ServerPlayer serverPlayer) {
            new ClientboundOpenLetterBlockViewScreenPacket(blockEntity.getLetter(player), pos).sendToClient(serverPlayer);
        }
        level.playSound(player, player, Envelope.SoundEvents.PAPER_CRACKLE.get(), SoundSource.PLAYERS, 1, 1);
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    public void ignite(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                       @Nullable Direction direction, @Nullable LivingEntity igniter) {
        if (level instanceof ServerLevel serverLevel) {
            VoxelShape shape = state.getShape(level, pos);
            AABB bounds = shape.bounds();
            Vec3 p = Vec3.atLowerCornerOf(pos).add(bounds.getCenter());
            serverLevel.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 12,
                  bounds.getXsize() / 2, bounds.getYsize() / 2, bounds.getZsize() / 2, 0.01);

            level.removeBlockEntity(pos); // Prevent item dropping
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1, 1);
        }
    }
}
