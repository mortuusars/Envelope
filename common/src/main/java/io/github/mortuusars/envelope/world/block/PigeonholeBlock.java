package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.api.mail.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PigeonholeBlock extends Block implements EntityBlock {
    public static final int MAX_WASTE_LEVEL = 5;

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty WASTE_LEVEL = IntegerProperty.create("waste_level", 0, MAX_WASTE_LEVEL);
    public static final BooleanProperty HAS_ADDRESS = BooleanProperty.create("has_address");
    public static final BooleanProperty HAS_MAIL = BooleanProperty.create("has_mail");

    public PigeonholeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WASTE_LEVEL, 0)
                .setValue(HAS_ADDRESS, false)
                .setValue(HAS_MAIL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WASTE_LEVEL, HAS_ADDRESS, HAS_MAIL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // --

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.NAME_TAG) && !state.getValue(HAS_ADDRESS)) { //TODO: config to require nametag
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                PlatformHelper.openMenu(serverPlayer, blockEntity.createAddressMenuProvider(hand, ""),
                        buffer -> {
                            buffer.writeEnum(hand);
                            buffer.writeBlockPos(pos);
                            buffer.writeUtf("");
                        });
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.isEmpty() && state.getValue(HAS_ADDRESS)) {
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                PlatformHelper.openMenu(serverPlayer, blockEntity.createMenuProvider(), buffer -> {
                    List<ItemStack> mail = blockEntity.getAllMail();
                    buffer.writeBlockPos(pos);
                    buffer.writeVarInt(mail.size());
                    for (ItemStack item : mail) {
                        ItemStack.STREAM_CODEC.encode(buffer, item);
                    }
                });
            }

            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    public void applyAddress(Player player, BlockState state, BlockPos pos, InteractionHand hand, String address) {
        Level level = player.level();

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity mailboxBlockEntity) {
            mailboxBlockEntity.setAddress(new Address.Mailbox(address));
            level.setBlock(pos, state.setValue(PigeonholeBlock.HAS_ADDRESS, true), PigeonholeBlock.UPDATE_ALL);
            player.getItemInHand(hand).shrink(1);
            player.swing(hand);
        }

        level.playSound(player, pos, SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.BLOCKS, 1, 1);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PigeonholeBlockEntity(pos, state);
    }
}