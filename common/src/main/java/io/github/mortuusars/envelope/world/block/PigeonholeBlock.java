package io.github.mortuusars.envelope.world.block;

import com.mojang.serialization.MapCodec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeMenuMailS2CP;
import io.github.mortuusars.envelope.util.validation.Issue;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.*;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenPigeonholeAddressTagScreenS2CP;
import io.github.mortuusars.envelope.world.block.occupiable.Occupiable;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeData;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PigeonholeBlock extends BaseEntityBlock {
    public static final MapCodec<BeehiveBlock> CODEC = simpleCodec(BeehiveBlock::new);

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
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity
              && blockEntity.mapAddressed((serverLevel, address, data) -> data.hasMail()).orElse(false)) {
            return 15;
        }

        return state.getValue(WASTE_LEVEL);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.getBlock().equals(newState.getBlock())) {
            if (level instanceof ServerLevel serverLevel) {
                PigeonholeManager pigeonholeManager = serverLevel.getEnvelopeContext().getPigeonholeManager();
                @Nullable PigeonholeData data = pigeonholeManager.getDataAt(pos);
                if (data != null) {
                    NonNullList<ItemStack> itemsToDrop = data.extractAllMail().stream()
                          .map(Mail::getItemCopy)
                          .collect(Collectors.toCollection(NonNullList::create));

                    Containers.dropContents(level, pos, itemsToDrop);

                    PigeonholeMenu.playersWithMenu(serverLevel, data.getAddress()).forEach(player ->
                          Packets.sendToClient(new PigeonholeMenuMailS2CP(Collections.emptyList()), player));

                    pigeonholeManager.remove(data.getAddress());
                }
            }

            if (level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                blockEntity.onBlockRemoved();
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (blockEntity instanceof PigeonholeBlockEntity pigeonholeBlockEntity) {
            if (!EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {
                pigeonholeBlockEntity.releaseAllOccupants(level, pos, state, Occupiable.ReleaseReason.EMERGENCY);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Entity entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (entity instanceof PrimedTnt
              || entity instanceof Creeper
              || entity instanceof WitherSkull
              || entity instanceof WitherBoss
              || entity instanceof MinecartTNT) {
            if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PigeonholeBlockEntity be) {
                be.releaseAllOccupants(be.getLevelOrThrow(), be.getBlockPos(), state, Occupiable.ReleaseReason.EMERGENCY);
            }
        }

        return super.getDrops(state, params);
    }

    @Override
    protected @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (level.getBlockState(neighborPos).getBlock() instanceof FireBlock
              && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity be) {
            be.releaseAllOccupants(be.getLevelOrThrow(), be.getBlockPos(), state, Occupiable.ReleaseReason.EMERGENCY);
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // --

    public void addWaste(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(WASTE_LEVEL)) return;

        int waste = state.getValue(WASTE_LEVEL);
        if (waste < MAX_WASTE_LEVEL) {
            waste += 1;
            level.setBlockAndUpdate(pos, state.setValue(WASTE_LEVEL, waste));
        }
    }

    // -- Interaction

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Envelope.Items.PIGEON_SPAWN_EGG.get())) {
            if (level instanceof ServerLevel serverLevel
                  && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity
                  && blockEntity.hasSpaceForAnotherOccupant()) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }

                @Nullable Pigeon pigeon = Envelope.EntityTypes.PIGEON.get().spawn(serverLevel, pos, MobSpawnType.SPAWN_EGG);
                if (pigeon != null) {
                    blockEntity.addOccupant(pos, state, pigeon);
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(Envelope.Tags.Items.WASTE_SCOOPABLE) && state.getValue(WASTE_LEVEL) >= MAX_WASTE_LEVEL) {
            if (!level.isClientSide()) {
                //TODO: Waste loot table
                popResourceFromFace(level, pos, state.getValue(FACING), new ItemStack(Items.BONE_MEAL));
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                level.setBlockAndUpdate(pos, state.setValue(WASTE_LEVEL, 0));
            }

            level.playSound(player, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);

            return ItemInteractionResult.SUCCESS;
        }

        if (!isValidDimension(level)) {
            Envelope.LOGGER.error("Pigeonhole cannot work in {}", level.dimension().location());
            player.displayClientMessage(Component.literal("Environment is not suitable for deliveries").withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.FAIL;
        }

        if (stack.getItem() instanceof AddressTagItem) {
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                AllAddresses knownAddresses = serverPlayer.serverLevel().getEnvelopeContext().addresses().getAll();
                Packets.sendToClient(new OpenPigeonholeAddressTagScreenS2CP(hand, knownAddresses, pos, blockEntity.getAddress()), serverPlayer);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(Envelope.Tags.Items.MAILABLE)
              && stack.get(Envelope.DataComponents.RECIPIENT) instanceof Address.Block block
              && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity
              && blockEntity.getAddress().map(a -> a.equals(block)).orElse(false)) {
            if (level instanceof ServerLevel serverLevel) {
                if (!stack.has(Envelope.DataComponents.SENDER)) {
                    stack.set(Envelope.DataComponents.SENDER, new Address.Player(player));
                }

                Mail result = blockEntity.getAddress().orElseThrow().receiveMail(serverLevel, new Mail(stack));

                player.setItemInHand(hand, result.getItemCopy());
            }
            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                        Player player, BlockHitResult hitResult) {
        if (state.getValue(HAS_ADDRESS)) {
            if (player instanceof ServerPlayer serverPlayer
                  && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity
                  && !blockEntity.openMenu(serverPlayer)) {
                return InteractionResult.FAIL;
            }

            return InteractionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    public void applyAddress(Player player, BlockState state, BlockPos pos, int slot, String addressId) {
        Level level = player.level();

        if (!isValidDimension(level)) {
            Envelope.LOGGER.error("Cannot apply an address in {}", level.dimension().location());
            return;
        }

        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
            Optional<Address.Block> currentAddress = blockEntity.getAddress();

            if (currentAddress.isPresent() && !currentAddress.get().matches(addressId)) {
                List<Issue> issues = AddressValidation.forPigeonhole(
                            () -> serverLevel.getEnvelopeContext().addresses().getAll(),
                            () -> player)
                      .validate(addressId);

                if (!issues.isEmpty()) {
                    player.displayClientMessage(issues.getFirst().getMessage(), true);
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1, 1);
                    return;
                }
            }

            Address.Block address = new Address.Block(addressId);
            blockEntity.setAddress(address);
            blockEntity.setOwner(player.getUUID());
            level.setBlock(pos, state.setValue(PigeonholeBlock.HAS_ADDRESS, true), PigeonholeBlock.UPDATE_ALL);

            boolean hasChanged = currentAddress.isEmpty() || !currentAddress.get().matches(addressId);
            if (hasChanged) {
                serverLevel.getEnvelopeContext().getPigeonholeManager().getOrRegister(address, pos);

                if (!player.isCreative()) {
                    player.getInventory().getItem(slot).shrink(1);
                    player.giveExperienceLevels(-Config.Server.PIGEONHOLE_ADDRESS_EXPERIENCE_LEVELS_COST.get());
                    level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1, 1);
                }

                player.swing(slot == Inventory.SLOT_OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
                level.playSound(null, pos, SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.BLOCKS, 1, 1);
            }
        }
    }

    public boolean isValidDimension(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PigeonholeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide
              ? null
              : createTickerHelper(blockEntityType, Envelope.BlockEntityTypes.PIGEONHOLE.get(), PigeonholeBlockEntity::serverTick);
    }
}