package io.github.mortuusars.envelope.world.block.dispenser;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class WasteScoopingDispenseItemBehavior extends OptionalDispenseItemBehavior {
    public static final WasteScoopingDispenseItemBehavior INSTANCE = new WasteScoopingDispenseItemBehavior();

    @Override
    protected @NotNull ItemStack execute(BlockSource blockSource, ItemStack item) {
        ServerLevel level = blockSource.level();
        BlockPos blockPos = blockSource.pos().relative(blockSource.state().getValue(DispenserBlock.FACING));
        setSuccess(tryScoopWaste(level, blockPos, item));
        return item;
    }

    protected boolean tryScoopWaste(ServerLevel level, BlockPos pos, ItemStack item) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PigeonholeBlock block && block.canScoopWaste(state)) {
            block.dropWasteItems(level, pos, state, item, null);
            block.clearWaste(level, pos, state);
            item.hurtAndBreak(1, level, null, i -> {});
            level.playSound(null, pos, Envelope.SoundEvents.PIGEONHOLE_SCOOP.get(), SoundSource.BLOCKS,
                  1.0F, level.random.nextFloat() * 0.1f + 0.95f);
            return true;
        }
        return false;
    }
}
