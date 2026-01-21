package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.MapCodec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenMailboxAddressTagScreenS2CP;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.delivery.CourierOrigin;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressValidation;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MailboxBlock extends BaseEntityBlock {
    public static final MapCodec<MailboxBlock> CODEC = simpleCodec(MailboxBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public MailboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
              .setValue(FACING, Direction.NORTH)
              .setValue(OPEN, false));
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
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

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity
              && !blockEntity.getAllMail().isEmpty()) {
            return 15;
        }
        return 0;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer != null && level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity) {
            blockEntity.setOwner(placer.getUUID());
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.getBlock().equals(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            // Using MailboxBlockEntity#onBlockRemoved does not cover every case,
            // as block entity might not exist while the block is still placed.
            // This happens with CarryOn relocation for example, where block entity is removed first.
            // So we remove any registered mailbox at this position:
            MailService.of(serverLevel).mailboxes().remove(pos);

            if (level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity) {
                blockEntity.onBlockRemoved(level, pos, state, newState);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!MailService.operatesIn(level)) {
            player.displayClientMessage(Component.literal("Mail Service does not operate in this dimension.")
                  .withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof AddressTagItem) {
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity) {
                AllAddresses knownAddresses = serverPlayer.serverLevel().getEnvelopeMailService().getKnownAddresses();
                Packets.sendToClient(new OpenMailboxAddressTagScreenS2CP(hand, knownAddresses, pos, Optional.of(blockEntity.getAddress())), serverPlayer);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (player.isCreative()
              && stack.is(Envelope.Tags.Items.MAILABLE)
              && stack.get(Envelope.DataComponents.MAIL_RECIPIENT) instanceof Address.Block recipientAddress
              && level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity
              && blockEntity.getAddress().equals(recipientAddress)) {
            if (level instanceof ServerLevel serverLevel) {
                ItemStack result = MailService.of(serverLevel).deliverMail(recipientAddress, stack.split(1));
                if (player.getItemInHand(hand).isEmpty()) {
                    player.setItemInHand(hand, result.copy());
                } else if (!player.addItem(result)) {
                    player.drop(result, false);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(Envelope.Items.PIGEON_SPAWN_EGG.get())) {
            if (level instanceof ServerLevel serverLevel) {
                if (level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity
                      && blockEntity.isAvailableForPickup()
                      && Envelope.EntityTypes.PIGEON.get().spawn(serverLevel,
                        pos.relative(state.getValue(FACING)), MobSpawnType.SPAWN_EGG) instanceof Pigeon pigeon
                      && blockEntity.tryStartDelivery(pigeon)) {
                    pigeon.setOrigin(CourierOrigin.service());
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                } else {
                    serverLevel.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1, 1);
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                        Player player, BlockHitResult hitResult) {
        if (!MailService.operatesIn(level)) {
            player.displayClientMessage(Component.literal("Mail Service does not operate in this dimension.")
                  .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        if (level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity) {
            blockEntity.openMenu(player);
        }

        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    // --

    public void changeAddress(Player player, BlockPos pos, InteractionHand hand, String addressId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof AddressTagItem)) {
            Envelope.LOGGER.error("Failed change address: item in hand is not AddressTagItem, but {}", stack);
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity)) {
            Envelope.LOGGER.error("Failed change address: block entity at pos is not a MailboxBlockEntity.");
            return;
        }

        AddressValidation.forMailbox(MailService.of(level).getKnownAddresses(), player)
              .test(addressId)
              .ifPresentOrElse(
                    id -> {
                        applyAddress(player, blockEntity, new Address.Block(id), stack);
                        player.swing(hand);
                    },
                    error -> {
                        player.displayClientMessage(error.getTranslation(), true);
                        player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.75f, 1f);
                    });
    }

    public static void placeBlockWithAddress(Player player, InteractionHand hand, BlockPlaceContext context, String addressId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = context.getClickedPos();
        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.75f, 1f);
            Envelope.LOGGER.error("Failed to place mailbox: item in hand is not a BlockItem, but {}", stack);
            return;
        }

        AddressValidation.forMailbox(MailService.of(level).getKnownAddresses(), player)
              .test(addressId)
              .ifPresentOrElse(
                    id -> {
                        blockItem.place(context);

                        if (!(level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity)) {
                            player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.75f, 1f);
                            Envelope.LOGGER.error("Failed to place mailbox: be at pos [{}] is not MailboxBlockEntity", pos.toShortString());
                            return;
                        }

                        blockEntity.getBlockState().getBlock().setPlacedBy(level, pos, blockEntity.getBlockState(), player, stack);
                        applyAddress(player, blockEntity, new Address.Block(id), stack);
                        player.swing(hand);
                    },
                    error -> {
                        player.displayClientMessage(error.getTranslation(), true);
                        player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.75f, 1f);
                    });
    }

    private static void applyAddress(Player player, MailboxBlockEntity blockEntity, Address.Block address, ItemStack stack) {
        blockEntity.setAddress(address);

        if (Config.Server.MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST.get() > 0) {
            player.level().playSound(player, blockEntity.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS);
        }

        if (!player.isCreative()) {
            stack.shrink(1);
            player.giveExperienceLevels(-Config.Server.MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST.get());
        }
    }

    // --

    @Nullable
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MailboxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide
              ? null
              : createTickerHelper(blockEntityType, Envelope.BlockEntityTypes.MAILBOX.get(),
              (lvl, blockPos, blockState, blockEntity) -> blockEntity.serverTick(((ServerLevel) lvl), blockPos, state));
    }
}