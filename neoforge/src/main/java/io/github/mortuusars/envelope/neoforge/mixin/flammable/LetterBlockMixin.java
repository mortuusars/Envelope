package io.github.mortuusars.envelope.neoforge.mixin.flammable;

import io.github.mortuusars.envelope.world.block.LetterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LetterBlock.class)
public abstract class LetterBlockMixin implements IBlockExtension {
    @Shadow
    public abstract void ignite(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                          @Nullable Direction direction, @Nullable LivingEntity igniter);

    @Override
    public int getFlammability(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
        return 250;
    }

    @Override
    public int getFireSpreadSpeed(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction direction) {
        return 20;
    }

    @Override
    public void onCaughtFire(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                             @Nullable Direction direction, @Nullable LivingEntity igniter) {
        ignite(state, level, pos, direction, igniter);
    }
}
