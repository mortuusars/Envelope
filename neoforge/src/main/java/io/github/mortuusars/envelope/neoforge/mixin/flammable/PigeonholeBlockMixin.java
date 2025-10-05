package io.github.mortuusars.envelope.neoforge.mixin.flammable;

import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PigeonholeBlock.class)
public class PigeonholeBlockMixin implements IBlockExtension {
    @Override
    public int getFlammability(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
        return 5;
    }
}
