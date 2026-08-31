package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.MapCodec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.network.packet.clientbound.ClientboundOpenMailboxAddressTagScreenPacket;
import io.github.mortuusars.envelope.world.mail.delivery.CourierOrigin;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import io.github.mortuusars.envelope.world.mail.address.BlockAddressValidation;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MailboxBlock extends BaseEntityBlock {
    public static final MapCodec<MailboxBlock> CODEC = simpleCodec(MailboxBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty HAS_MAIL = BooleanProperty.create("has_mail");

    public static final VoxelShape SHAPE_Z = Shapes.or(
          Block.box(3, 2, 3, 13, 11.5, 13),
          Block.box(6, 11.5, 3, 10, 12, 13),
          Block.box(2, 0, 2, 14, 2, 14));
    public static final VoxelShape SHAPE_X = Shapes.or(
          Block.box(3, 2, 3, 13, 11.5, 13),
          Block.box(3, 11.5, 6, 13, 12, 10),
          Block.box(2, 0, 2, 14, 2, 14));
    public static final VoxelShape SHAPE_HANGING_NORTH = Shapes.or(
          Block.box(2, 0, 7, 14, 2, 16),
          Block.box(3, 2, 8, 13, 11.5, 16),
          Block.box(6, 11.5, 8, 10, 12, 16));
    public static final VoxelShape SHAPE_HANGING_EAST = Shapes.or(
          Block.box(0, 0, 2, 9, 2, 14),
          Block.box(0, 2, 3, 8, 11.5, 13),
          Block.box(0, 11.5, 6, 8, 12, 10));
    public static final VoxelShape SHAPE_HANGING_SOUTH = Shapes.or(
          Block.box(2, 0, 0, 14, 2, 9),
          Block.box(3, 2, 0, 13, 11.5, 8),
          Block.box(6, 11.5, 0, 10, 12, 8));
    public static final VoxelShape SHAPE_HANGING_WEST = Shapes.or(
          Block.box(7, 0, 2, 16, 2, 14),
          Block.box(8, 2, 3, 16, 11.5, 13),
          Block.box(8, 11.5, 6, 16, 12, 10));

    public MailboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
              .setValue(FACING, Direction.NORTH)
              .setValue(HANGING, false)
              .setValue(OPEN, false)
              .setValue(HAS_MAIL, false));
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
        if (state.hasProperty(HANGING) && state.getValue(HANGING)) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> SHAPE_HANGING_SOUTH;
                case WEST -> SHAPE_HANGING_WEST;
                case EAST -> SHAPE_HANGING_EAST;
                default -> SHAPE_HANGING_NORTH;
            };
        }

        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_Z : SHAPE_X;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANGING, OPEN, HAS_MAIL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos attachPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        boolean hanging = !context.replacingClickedOnBlock()
              && context.getClickedFace().getAxis().isHorizontal()
              && context.getLevel().getBlockState(attachPos).isFaceSturdy(context.getLevel(), attachPos, context.getClickedFace());

        return this.defaultBlockState()
              .setValue(FACING, hanging ? context.getClickedFace() : context.getHorizontalDirection().getOpposite())
              .setValue(HANGING, hanging);
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
            MailService.of(serverLevel).getMailboxes().remove(pos);

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
                new ClientboundOpenMailboxAddressTagScreenPacket(hand, knownAddresses, pos, blockEntity.getAddress()).sendToClient(serverPlayer);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (player.isCreative()
              && stack.is(Envelope.Tags.Items.MAILABLE)
              && stack.get(Envelope.DataComponents.MAIL_ADDRESS_TAG) instanceof BlockAddress address
              && level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity
              && blockEntity.getAddress().equals(address)) {
            if (level instanceof ServerLevel) {
                ItemStack mail = Mail.of(stack.copyWithCount(1))
                      .writeToLog(DeliveryRecord.sentFrom(new PlayerAddress(player)))
                      .writeToLog(DeliveryRecord.arrivedTo(address))
                      .sender(new PlayerAddress(player))
                      .id(Id.create(level))
                      .get();
                if (blockEntity.addMail(mail)) {
                    player.getItemInHand(hand).shrink(1);
                    blockEntity.onMailInserted(mail);
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
                    if (player.isCreative()) {
                        pigeon.setOrigin(CourierOrigin.service());
                    } else {
                        pigeon.setOrigin(CourierOrigin.regular(pos));
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
        if (!(level.getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (!MailService.operatesIn(level)) {
            player.displayClientMessage(Component.literal("Mail Service does not operate in this dimension.")
                  .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        blockEntity.openMenu(player);
        player.awardStat(Envelope.Stats.INTERACT_WITH_MAILBOX.get());

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

        BlockAddressValidation.forMailbox(MailService.of(level).getKnownAddresses(), player)
              .test(addressId)
              .ifPresentOrElse(
                    id -> {
                        applyAddress(player, blockEntity, new BlockAddress(id), stack);
                        player.swing(hand, true);

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
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

        BlockAddressValidation.forMailbox(MailService.of(level).getKnownAddresses(), player)
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
                        applyAddress(player, blockEntity, new BlockAddress(id), stack);
                        player.swing(hand, true);
                    },
                    error -> {
                        player.displayClientMessage(error.getTranslation(), true);
                        player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.75f, 1f);
                    });
    }

    private static void applyAddress(Player player, MailboxBlockEntity blockEntity, BlockAddress address, ItemStack stack) {
        blockEntity.setAddress(address);

        if (Config.Server.MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST.get() > 0) {
            player.level().playSound(player, blockEntity.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS);
        }

        if (!player.isCreative()) {
            player.giveExperienceLevels(-Config.Server.MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST.get());
        }
    }

    // --

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MailboxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide
              ? null
              : createTickerHelper(blockEntityType, Envelope.BlockEntityTypes.MAILBOX.get(),
              (lvl, blockPos, blockState, blockEntity) -> blockEntity.serverTick(((ServerLevel) lvl), blockPos, state));
    }
}