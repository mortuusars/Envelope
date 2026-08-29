package io.github.mortuusars.envelope.world.entity.ai.goal.courier;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public abstract class AbstractGoToBlockGoal extends Goal {
    public static final int MAX_TRAVELLING_TICKS = 600;
    public static final int TICKS_BEFORE_DROP = 60;

    protected final PhysicalCourier courier;

    protected int travellingTicks;
    protected int ticksStuck;

    public AbstractGoToBlockGoal(PhysicalCourier courier) {
        this.courier = courier;
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
              && !courier.asCourierEntity().hasRestriction()
              && !courier.hasReachedTarget(pos)
              && !Position.isFireNearby(courier.level(), pos);
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
        courier.getNavigation().stop();
        courier.getNavigation().resetMaxVisitedNodesMultiplier();
    }
}
