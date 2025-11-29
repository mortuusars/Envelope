package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.VoxelShapeUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PaperBoxBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty BOXES = IntegerProperty.create("boxes", 1, 4);

    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        // Axis X
        SHAPES[0] = Block.box(5, 0, 4, 11, 6, 12);
        SHAPES[1] = Shapes.or(Block.box(1, 0, 4, 7, 6, 13), Block.box(8, 0, 5, 15, 6, 12));
        SHAPES[2] = Shapes.or(Block.box(1, 0, 5, 7, 6, 14), Block.box(8, 0, 8, 15, 6, 15), Block.box(8, 0, 1, 14, 6, 7));
        SHAPES[3] = Shapes.or(SHAPES[2], Block.box(5, 6, 4, 11, 12, 13));
        // Axis Z
        SHAPES[4] = VoxelShapeUtils.rotate(SHAPES[0], Rotation.CLOCKWISE_90);
        SHAPES[5] = VoxelShapeUtils.rotate(SHAPES[1], Rotation.CLOCKWISE_90);
        SHAPES[6] = VoxelShapeUtils.rotate(SHAPES[2], Rotation.CLOCKWISE_90);
        SHAPES[7] = VoxelShapeUtils.rotate(SHAPES[3], Rotation.CLOCKWISE_90);
    }

    public PaperBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(getStateDefinition().any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(BOXES, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, BOXES);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return SHAPES[state.getValue(BOXES) - 1 + (state.getValue(AXIS) == Direction.Axis.Z ? 4 : 0)];
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos());
        if (blockstate.is(this)) {
            return blockstate.cycle(BOXES);
        } else {
            return getStateDefinition().any().setValue(AXIS, context.getHorizontalDirection().getAxis());
        }
    }

    @Override
    public boolean canBeReplaced(@NotNull BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive()
            && context.getItemInHand().getItem() == this.asItem()
            && state.getValue(BOXES) < 4 || super.canBeReplaced(state, context);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isSecondaryUseActive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int boxes = state.getValue(BOXES) - 1;

        if (level instanceof ServerLevel serverLevel) {
            if (boxes <= 0) {
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(BOXES, boxes), Block.UPDATE_ALL);
            }

            if (!player.isCreative()) {
                player.addItem(new ItemStack(this.asItem()));
            }

            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                  pos.getX() + 0.5f, pos.getY() + 0.3f, pos.getZ() + 0.5f, 3, 0.2, 0.2, 0.2, 0);
        }

        level.playSound(player, pos, Envelope.SoundEvents.PAPER_USE.get(), SoundSource.BLOCKS,
            0.9f, level.getRandom().nextFloat() * 0.1f + 0.85f + (boxes * 0.2f));

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> {
                return switch (state.getValue(AXIS)) {
                    case X -> state.setValue(AXIS, Direction.Axis.Z);
                    case Z -> state.setValue(AXIS, Direction.Axis.X);
                    default -> state;
                };
            }
            default -> {
                return state;
            }
        }
    }
}
