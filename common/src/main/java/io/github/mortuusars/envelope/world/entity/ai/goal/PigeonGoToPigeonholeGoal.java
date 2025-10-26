package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PigeonGoToPigeonholeGoal extends Goal {
    public static final int MAX_TRAVELLING_TICKS = 600;
    protected static final int TICKS_BEFORE_PIGEONHOLE_DROP = 60;
    protected final Pigeon pigeon;
    @Nullable
    protected Path lastPath;
    protected int travellingTicks;
    protected int ticksStuck;

    public PigeonGoToPigeonholeGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        this.travellingTicks = pigeon.level().random.nextInt(10);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return pigeon.getPigeonholeHandler().getCurrentPos() != null
                && !pigeon.hasRestriction()
                && pigeon.getPigeonholeHandler().wantsToEnterPigeonhole(pigeon.level())
                && !pigeon.hasReachedTarget(pigeon.getPigeonholeHandler().getCurrentPos())
                && pigeon.level().getBlockState(pigeon.getPigeonholeHandler().getCurrentPos()).is(Envelope.Tags.Blocks.PIGEONHOLES);
    }

    @Override
    public void start() {
        travellingTicks = 0;
        ticksStuck = 0;
        super.start();
    }

    @Override
    public void stop() {
        this.travellingTicks = 0;
        this.ticksStuck = 0;
        pigeon.getNavigation().stop();
        pigeon.getNavigation().resetMaxVisitedNodesMultiplier();
    }

    @Override
    public void tick() {
        PigeonholeHandler handler = pigeon.getPigeonholeHandler();

        if (handler.getCurrentPos() == null) return;

        travellingTicks++;
        if (travellingTicks > adjustedTickDelay(MAX_TRAVELLING_TICKS)) {
            handler.dropAndBlacklistPigeonhole();
        } else if (!pigeon.getNavigation().isInProgress()) {
            if (!pigeon.closerThan(handler.getCurrentPos(), 16)) {
                if (!pigeon.blockPosition().closerThan(handler.getCurrentPos(), 32)) {
                    handler.dropPigeonhole();
                } else {
                    pigeon.pathfindRandomlyTowards(handler.getCurrentPos());
                }
            } else {
                boolean bl = pathfindDirectlyTowards(handler.getCurrentPos());
                if (!bl) {
                    handler.dropAndBlacklistPigeonhole();
                } else if (lastPath != null && lastPath.sameAs(pigeon.getNavigation().getPath())) {
                    ticksStuck++;
                    if (ticksStuck > TICKS_BEFORE_PIGEONHOLE_DROP) {
                        handler.dropPigeonhole();
                        ticksStuck = 0;
                    }
                } else {
                    lastPath = pigeon.getNavigation().getPath();
                }
            }
        }
    }

    private boolean pathfindDirectlyTowards(BlockPos pos) {
        pigeon.getNavigation().setMaxVisitedNodesMultiplier(10.0F);
        pigeon.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 2, 1.0);
        return pigeon.getNavigation().getPath() != null && pigeon.getNavigation().getPath().canReach();
    }
}
