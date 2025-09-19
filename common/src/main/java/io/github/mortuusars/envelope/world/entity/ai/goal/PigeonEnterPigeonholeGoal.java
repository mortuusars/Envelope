package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

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

        BlockPos pos = pigeon.getPigeonholeHandler().getPigeonholePos();

        if (pos != null
                && pigeon.getPigeonholeHandler().wantsToEnterPigeonhole()
                && pos.closerToCenterThan(pigeon.position(), 2.0)
                && pigeon.level().getBlockEntity(pos) instanceof PigeonholeBlockEntity be) {
            if (!be.isFull()) {
                return true;
            }

            pigeon.getPigeonholeHandler().setPigeonholePos(null);
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        pigeon.getPigeonholeHandler().getPigeonhole().ifPresent(pigeonhole -> pigeonhole.addOccupant(pigeon));
    }
}
