package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class PigeonDeliverMailGoal extends Goal {
    protected final Pigeon pigeon;

    public PigeonDeliverMailGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return pigeon.isDelivering();
    }

    @Override
    public void stop() {
        pigeon.setDelivery(null);
        pigeon.getNavigation().stop();
        pigeon.getNavigation().resetMaxVisitedNodesMultiplier();
    }

    @Override
    public void tick() {
        if (!(pigeon.level() instanceof ServerLevel level)) {
            return;
        }

        pigeon.getDelivery().ifPresent(delivery -> {
            delivery.tick(level, pigeon.getDeliveryHandler());

            delivery.getRoute().getSegment(delivery.getCurrentPhase()).endPos()
                  .ifPresent(pos -> {
                      if ((delivery.getCurrentPhase().isAscending() || delivery.getCurrentPhase().isDescending())
                            && pigeon.hasReachedTarget(pos)) {
                          delivery.getProgress().complete();
                      } else if (!pigeon.getNavigation().isInProgress()) {
                          if (!pigeon.pathfindDirectlyTowards(pos)) {
                              pigeon.pathfindRandomlyTowards(pos);
                          }
                      }
                  });
        });
    }
}
