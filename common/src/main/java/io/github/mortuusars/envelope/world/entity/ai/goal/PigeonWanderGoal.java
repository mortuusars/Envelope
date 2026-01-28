package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PigeonWanderGoal extends Goal {
    private static final int WANDER_THRESHOLD = 22;

    private final Pigeon pigeon;

    public PigeonWanderGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return pigeon.getNavigation().isDone() && pigeon.getRandom().nextInt(25) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return pigeon.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        Vec3 pos = this.findPos();
        if (pos != null) {
            pigeon.getNavigation().moveTo(pigeon.getNavigation().createPath(BlockPos.containing(pos), 1), 1.0);
        }
    }

    @Nullable
    private Vec3 findPos() {
        Vec3 pos;
        @Nullable BlockPos pigeonholePos = pigeon.getPigeonholeHandler().getTargetPos();
        if (pigeon.getPigeonholeHandler().isPigeonholeValid(pigeon.level(), pigeon.blockPosition())
              && pigeonholePos != null && !pigeon.closerThan(pigeonholePos, WANDER_THRESHOLD)) {
            Vec3 pigeonholeCenter = Vec3.atCenterOf(pigeonholePos);
            pos = pigeonholeCenter.subtract(pigeon.position()).normalize();
        } else {
            pos = pigeon.getViewVector(0.0F);
        }

        int radius = 8;
        Vec3 hoverPos = HoverRandomPos.getPos(pigeon, radius, 12, pos.x, pos.z, (float) (Math.PI / 2), 3, 1);
        return hoverPos != null ? hoverPos : AirAndWaterRandomPos.getPos(pigeon, radius, 4, -2, pos.x, pos.z, (float) (Math.PI / 2));
    }
}
