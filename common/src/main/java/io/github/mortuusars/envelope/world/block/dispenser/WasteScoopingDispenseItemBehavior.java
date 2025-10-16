package io.github.mortuusars.envelope.world.block.dispenser;

import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class WasteScoopingDispenseItemBehavior extends OptionalDispenseItemBehavior {
    public static final WasteScoopingDispenseItemBehavior INSTANCE = new WasteScoopingDispenseItemBehavior();

    @Override
    protected @NotNull ItemStack execute(BlockSource blockSource, ItemStack item) {
        ServerLevel level = blockSource.level();
        BlockPos blockPos = blockSource.pos().relative(blockSource.state().getValue(DispenserBlock.FACING));
        setSuccess(tryScoopWaste(level, blockPos));
        if (isSuccess()) {
            item.hurtAndBreak(1, level, null, i -> {
            });
        }

        return item;
    }

    protected boolean tryScoopWaste(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PigeonholeBlock && state.getValue(PigeonholeBlock.WASTE_LEVEL) >= PigeonholeBlock.MAX_WASTE_LEVEL) {
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            //TODO: waste loot table
            DispenserBlock.popResourceFromFace(level, pos, state.getValue(PigeonholeBlock.FACING), new ItemStack(Items.BONE_MEAL));
            level.setBlock(pos, state.setValue(PigeonholeBlock.WASTE_LEVEL, 0), Block.UPDATE_ALL);
            return true;
        }

        return false;
    }
}
