package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

public class PigeonGoToPigeonholeGoal extends AbstractGoToBlockGoal {
    @Nullable
    protected Path lastPath;

    public PigeonGoToPigeonholeGoal(Pigeon pigeon) {
        super(pigeon);
    }

    @Override
    public @Nullable BlockPos getBlockPos() {
        return pigeon.getPigeonholeHandler().getTargetPos();
    }

    @Override
    public boolean canUse() {
        return super.canUse()
              && pigeon.getPigeonholeHandler().wantsToEnterPigeonhole(pigeon)
              && pigeon.level().getBlockState(pigeon.getPigeonholeHandler().getTargetPos()).is(Envelope.Tags.Blocks.PIGEONHOLES)
              && PigeonholeHandler.isPigeonholeSafe(pigeon.level(), pigeon.getPigeonholeHandler().getTargetPos());
    }

    @Override
    public void tick() {
        PigeonholeHandler handler = pigeon.getPigeonholeHandler();

        if (handler.getTargetPos() == null) return;

        travellingTicks++;
        if (travellingTicks > adjustedTickDelay(MAX_TRAVELLING_TICKS)) {
            handler.dropAndBlacklistPigeonhole();
            return;
        }

        if (pigeon.getNavigation().isInProgress()) {
            return;
        }

        if (!pigeon.closerThan(handler.getTargetPos(), 16)) {
            if (handler.getTargetPos() != null
                  && Position.distanceToSqr(pigeon.level(), handler.getTargetPos(), pigeon.position()) > 32 * 32) {
                handler.dropPigeonhole();
            } else {
                pigeon.pathfindRandomlyTowards(handler.getTargetPos());
            }
        } else {
            boolean canReach = pigeon.pathfindDirectlyTowards(handler.getTargetPos());
            if (!canReach) {
                handler.dropAndBlacklistPigeonhole();
            } else if (lastPath != null && lastPath.sameAs(pigeon.getNavigation().getPath())) {
                ticksStuck++;
                if (ticksStuck > TICKS_BEFORE_DROP) {
                    handler.dropPigeonhole();
                    ticksStuck = 0;
                }
            } else {
                lastPath = pigeon.getNavigation().getPath();
            }
        }
    }
}
