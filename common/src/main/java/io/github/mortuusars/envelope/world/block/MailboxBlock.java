package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.Mail;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MailboxBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");

    public static final VoxelShape SHAPE_REGULAR = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(3, 2, 3, 13, 10, 13),
            Block.box(1, 10, 1, 15, 14, 15)
    );
    public static final VoxelShape SHAPE_HANGING_NORTH = Shapes.or(
            Block.box(1, 0, 6, 15, 2, 16),
            Block.box(3, 2, 8, 13, 10, 16),
            Block.box(1, 10, 6, 15, 14, 16)
    );
    public static final VoxelShape SHAPE_HANGING_EAST = Shapes.or(
            Block.box(0, 0, 1, 10, 2, 15),
            Block.box(0, 2, 3, 8, 10, 13),
            Block.box(0, 10, 1, 10, 14, 15)
    );
    public static final VoxelShape SHAPE_HANGING_SOUTH = Shapes.or(
            Block.box(1, 0, 0, 15, 2, 10),
            Block.box(3, 2, 0, 13, 10, 8),
            Block.box(1, 10, 0, 15, 14, 10)
    );
    public static final VoxelShape SHAPE_HANGING_WEST = Shapes.or(
            Block.box(6, 0, 1, 16, 2, 15),
            Block.box(8, 2, 3, 16, 10, 13),
            Block.box(6, 10, 1, 16, 14, 15)
    );

    public MailboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANGING);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HANGING)) {
            return switch (state.getValue(FACING)) {
                case NORTH, UP, DOWN -> SHAPE_HANGING_NORTH;
                case EAST -> SHAPE_HANGING_EAST;
                case SOUTH -> SHAPE_HANGING_SOUTH;
                case WEST -> SHAPE_HANGING_WEST;
            };
        }
        return SHAPE_REGULAR;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace().getAxis().isHorizontal()) {
            return defaultBlockState()
                    .setValue(FACING, context.getClickedFace())
                    .setValue(HANGING, true);
        }

        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HANGING, false);
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // --

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity) {
            List<ItemStack> mail = Mail.getMailboxes().getAllMail(blockEntity.getAddress());

            PlatformHelper.openMenu(serverPlayer, blockEntity, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeVarInt(mail.size());
                for (ItemStack item : mail) {
                    ItemStack.STREAM_CODEC.encode(buffer, item);
                }
            });
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MailboxBlockEntity(pos, state);
    }
}
