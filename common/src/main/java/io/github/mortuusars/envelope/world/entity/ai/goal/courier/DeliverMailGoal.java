package io.github.mortuusars.envelope.world.entity.ai.goal.courier;

import io.github.mortuusars.envelope.world.entity.ai.CourierNavigation;
import io.github.mortuusars.envelope.world.entity.ai.PigeonNavigation;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class DeliverMailGoal extends Goal {
    protected final PhysicalCourier courier;

    public DeliverMailGoal(PhysicalCourier courier) {
        this.courier = courier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public PhysicalCourier getCourier() {
        return courier;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return courier.isDelivering();
    }

    @Override
    public void stop() {
        courier.setDelivery(null);
        courier.getNavigation().stop();
        courier.getNavigation().resetMaxVisitedNodesMultiplier();
    }

    @Override
    public void tick() {
        if (!(courier.level() instanceof ServerLevel level)) {
            return;
        }

        courier.getCurrentDelivery().ifPresent(delivery -> {
            courier.tickDelivery(level, delivery);

            delivery.getRoute().getSegment(delivery.getPhase()).endPos()
                  .ifPresentOrElse(localPos -> {
                      BlockPos targetLocal = delivery.getPhase().isDescending()
                            ? PigeonNavigation.getMailboxApproachTarget(level, localPos)
                            : localPos;

                      if ((delivery.getPhase().isAscending() || delivery.getPhase().isDescending())
                            && courier.hasReachedTarget(targetLocal)) {
                          // Complete the phase instantly
                          delivery.setPhaseProgress(courier.getPhaseDuration(level, delivery, delivery.getPhase()));
                          return;
                      }

                      if (!courier.hasReachedTarget(targetLocal)) {
                          BlockPos navigationPos = CourierNavigation.getNavigationPos(courier, targetLocal);
                          if (!courier.getNavigation().isInProgress()
                                || !navigationPos.equals(courier.getNavigation().getTargetPos())) {
                              courier.pathfindDirectlyTowards(targetLocal);
                          }
                      }
                  }, () -> {
                      PathfinderMob entity = courier.asCourierEntity();
                      @Nullable Vec3 randomPos = AirAndWaterRandomPos.getPos(entity, 8, 4, -2,
                            entity.getX(), entity.getZ(), (float) (Math.PI / 2));
                      if (randomPos != null && level.getRandom().nextFloat() < 0.1f) {
                          courier.pathfindDirectlyTowards(BlockPos.containing(randomPos));
                      }
                  });
        });
    }
}