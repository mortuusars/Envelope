package io.github.mortuusars.envelope.world.block.dispenser;

import io.github.mortuusars.envelope.world.block.PackageBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class PackageDispenseItemBehavior implements DispenseItemBehavior {
    @Override
    public @NotNull ItemStack dispense(BlockSource blockSource, ItemStack stack) {
        Direction facing = blockSource.state().getValue(DispenserBlock.FACING);
        BlockPos facingPos = blockSource.blockEntity().getBlockPos().relative(facing);
        tryPlace(blockSource.level(), facing, facingPos, stack);
        return stack;
    }

    protected void tryPlace(Level level, Direction facing, BlockPos pos, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!level.getBlockState(pos).canBeReplaced()) return;
        if (!Block.canSupportCenter(level, pos.below(), Direction.UP)) return;

        BlockState state = blockItem.getBlock().getStateDefinition().any().setValue(PackageBlock.FACING, facing.getOpposite());

        if (!level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) return;

        BlockState placedState = level.getBlockState(pos);
        if (placedState.is(state.getBlock())) {
            placedState = updateBlockStateFromTag(pos, level, stack, placedState);
            BlockItem.updateCustomBlockEntityTag(level, null, pos, stack);
            updateBlockEntityComponents(level, pos, stack);
        }

        SoundType soundType = placedState.getSoundType();
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(placedState));
        stack.consume(1, null);
    }

    protected BlockState updateBlockStateFromTag(BlockPos pos, Level level, ItemStack stack, BlockState state) {
        BlockItemStateProperties blockItemStateProperties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        if (blockItemStateProperties.isEmpty()) {
            return state;
        } else {
            BlockState blockState = blockItemStateProperties.apply(state);
            if (blockState != state) {
                level.setBlock(pos, blockState, 2);
            }

            return blockState;
        }
    }

    protected void updateBlockEntityComponents(Level level, BlockPos poa, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(poa);
        if (blockEntity != null) {
            blockEntity.applyComponentsFromItemStack(stack);
            blockEntity.setChanged();
        }
    }
}
