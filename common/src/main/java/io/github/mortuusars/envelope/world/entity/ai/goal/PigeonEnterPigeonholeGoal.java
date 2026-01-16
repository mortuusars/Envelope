package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

public class PigeonEnterPigeonholeGoal extends Goal {
    private final Pigeon pigeon;

    public PigeonEnterPigeonholeGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
    }

    @Override
    public boolean canUse() {
        if (pigeon.isDelivering()) {
            return false;
        }

        @Nullable BlockPos pos = pigeon.getPigeonholeHandler().getTargetPos();

        if (pos != null
              && pigeon.getPigeonholeHandler().wantsToEnterPigeonhole(pigeon)
              && pos.closerToCenterThan(pigeon.position(), 2.0)
              && !Position.isFireNearby(pigeon.level(), pos)
              && pigeon.level().getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
            if (blockEntity.hasSpaceForAnotherOccupant()) {
                return true;
            }

            pigeon.getPigeonholeHandler().setTargetPos(null);
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        pigeon.getPigeonholeHandler().getPigeonholeAtCurrentPos(pigeon.level())
              .ifPresent(pigeonhole -> pigeonhole.addOccupant(pigeonhole.getBlockPos(), pigeonhole.getBlockState(), pigeon));
    }
}
