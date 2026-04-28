package io.github.mortuusars.envelope.world.block;

import com.mojang.serialization.MapCodec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.occupiable.Occupiable;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PigeonholeBlock extends BaseEntityBlock {
    public static final MapCodec<PigeonholeBlock> CODEC = simpleCodec(PigeonholeBlock::new);

    public static final int MAX_WASTE_LEVEL = 5;

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty WASTE_LEVEL = IntegerProperty.create("waste_level", 0, MAX_WASTE_LEVEL);

    public PigeonholeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
              .setValue(FACING, Direction.NORTH)
              .setValue(WASTE_LEVEL, 0));
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WASTE_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

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
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
              : createTickerHelper(blockEntityType, Envelope.BlockEntityTypes.PIGEONHOLE.get(),
              (lvl, blockPos, blockState, blockEntity) -> blockEntity.serverTick(((ServerLevel) lvl), blockPos, state));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (blockEntity instanceof PigeonholeBlockEntity be) {
            if (!EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {
                be.releaseAllOccupants(level, pos, state, Occupiable.ReleaseReason.EMERGENCY);
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
        int waste = state.getValue(WASTE_LEVEL);
        if (waste < MAX_WASTE_LEVEL) {
            waste += 1;
            level.setBlockAndUpdate(pos, state.setValue(WASTE_LEVEL, waste));
        }
    }

    public void clearWaste(Level level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(WASTE_LEVEL, 0));
    }

    public boolean canScoopWaste(BlockState state) {
        return state.getValue(WASTE_LEVEL) >= MAX_WASTE_LEVEL;
    }

    public void dropWasteItems(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool, @Nullable Player player) {
        LootParams lootParams = new LootParams.Builder(level)
              .withParameter(LootContextParams.BLOCK_STATE, state)
              .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
              .withParameter(LootContextParams.TOOL, tool)
              .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
              .create(LootContextParamSets.BLOCK);

        List<ItemStack> items = level.getServer().reloadableRegistries()
              .getLootTable(Envelope.LootTables.PIGEONHOLE_WASTE)
              .getRandomItems(lootParams);

        for (ItemStack item : items) {
            popResourceFromFace(level, pos, state.getValue(FACING), item);

            if (item.is(Items.DIAMOND) && player instanceof ServerPlayer serverPlayer) {
                Envelope.CriteriaTriggers.SCOOP_DIAMOND.get().trigger(serverPlayer);
            }
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

        if (stack.is(Envelope.Tags.Items.WASTE_SCOOPABLE) && canScoopWaste(state)) {
            if (level instanceof ServerLevel serverLevel) {
                dropWasteItems(serverLevel, pos, state, player.getItemInHand(hand), player);
                clearWaste(level, pos, state);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            }

            level.playSound(player, pos, Envelope.SoundEvents.PIGEONHOLE_SCOOP.get(), SoundSource.BLOCKS,
                  1.0F, level.random.nextFloat() * 0.2f + 0.95f);

            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}