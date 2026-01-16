package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public abstract class AbstractGoToBlockGoal extends Goal {
    public static final int MAX_TRAVELLING_TICKS = 600;
    public static final int TICKS_BEFORE_DROP = 60;
    protected final Pigeon pigeon;
    protected int travellingTicks;
    protected int ticksStuck;

    public AbstractGoToBlockGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        this.travellingTicks = pigeon.level().random.nextInt(10);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public abstract @Nullable BlockPos getBlockPos();

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        @Nullable BlockPos pos = getBlockPos();
        return pos != null
              && !pigeon.hasRestriction()
              && !pigeon.hasReachedTarget(pos)
              && !Position.isFireNearby(pigeon.level(), pos);
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
}
