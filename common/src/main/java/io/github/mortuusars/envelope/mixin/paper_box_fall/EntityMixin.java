package io.github.mortuusars.envelope.mixin.paper_box_fall;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.mortuusars.envelope.world.block.PaperBoxBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow
    public float fallDistance;

    @WrapMethod(method = "checkFallDamage")
    private void onCheckFallDamage(double y, boolean onGround, BlockState state, BlockPos pos, Operation<Void> original) {
        @Nullable Float distance = null;
        Entity entity = (Entity)(Object)this;

        if (state.getBlock() instanceof PaperBoxBlock block && fallDistance > block.getFallDistanceToBreak(state, pos, entity)) {
            distance = block.reduceFallDistance(state, pos, entity, fallDistance);
        }

        original.call(y, onGround, state, pos);

        if (distance != null) {
            fallDistance = distance;
        }
    }
}
