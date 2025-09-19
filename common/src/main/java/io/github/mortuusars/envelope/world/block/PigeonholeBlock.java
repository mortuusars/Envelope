package io.github.mortuusars.envelope.world.block;

import com.mojang.serialization.MapCodec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
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

import java.util.List;

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
        return state.getValue(WASTE_LEVEL);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.getBlock().equals(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity pigeonholeBlockEntity) {
            pigeonholeBlockEntity.removeAddress();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (blockEntity instanceof PigeonholeBlockEntity pigeonholeBlockEntity) {
            if (!EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {
                pigeonholeBlockEntity.releaseAllOccupants(state, PigeonholeBlockEntity.ReleaseReason.EMERGENCY);
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
            BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (blockEntity instanceof PigeonholeBlockEntity pigeonholeBlockEntity) {
                pigeonholeBlockEntity.releaseAllOccupants(state, PigeonholeBlockEntity.ReleaseReason.EMERGENCY);
            }
        }

        return super.getDrops(state, params);
    }

    @Override
    protected @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (level.getBlockState(neighborPos).getBlock() instanceof FireBlock && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity beehiveBlockEntity) {
            beehiveBlockEntity.releaseAllOccupants(state, PigeonholeBlockEntity.ReleaseReason.EMERGENCY);
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
        if (stack.is(Envelope.Tags.Items.WASTE_SCOOPABLE) && state.getValue(WASTE_LEVEL) >= MAX_WASTE_LEVEL) {
            if (!level.isClientSide()) {
                popResourceFromFace(level, pos, state.getValue(FACING), new ItemStack(Items.BONE_MEAL));
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                level.setBlockAndUpdate(pos, state.setValue(WASTE_LEVEL, 0));
            }

            level.playSound(player, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);

            return ItemInteractionResult.SUCCESS;
        }

        if (stack.is(Items.NAME_TAG)) { //TODO: config to require nametag
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                if (!blockEntity.openAddressMenu(serverPlayer, hand)) {
                    return ItemInteractionResult.FAIL;
                }
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (state.getValue(HAS_ADDRESS)) {
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                if (!blockEntity.openMenu(serverPlayer, hand)) {
                    return ItemInteractionResult.FAIL;
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    public void applyAddress(Player player, BlockState state, BlockPos pos, InteractionHand hand, String address) {
        Level level = player.level();

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity mailboxBlockEntity) {
            mailboxBlockEntity.setAddress(new Address.Pigeonhole(address));
            level.setBlock(pos, state.setValue(PigeonholeBlock.HAS_ADDRESS, true), PigeonholeBlock.UPDATE_ALL);
            if (!player.isCreative()) {
                player.getItemInHand(hand).shrink(1);
            }
            player.swing(hand);
        }

        level.playSound(player, pos, SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.BLOCKS, 1, 1);
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