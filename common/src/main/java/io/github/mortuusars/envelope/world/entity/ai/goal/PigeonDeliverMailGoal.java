package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class PigeonDeliverMailGoal extends Goal {
    protected final Pigeon pigeon;

    public PigeonDeliverMailGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public Pigeon pigeon() {
        return pigeon;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
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

        pigeon.getCurrentDelivery().ifPresent(delivery -> {
            pigeon().getDeliveryHandler().tickDelivery(level, delivery);

            delivery.getRoute().getSegment(delivery.getPhase()).endPos()
                  .ifPresent(pos -> {
                      if (delivery.getPhase().isDescending()) {
                          BlockState state = level.getBlockState(pos);
                          if (state.getBlock() instanceof MailboxBlock) {
                              pos.relative(state.getValue(MailboxBlock.FACING));
                          }
                      }

                      if ((delivery.getPhase().isAscending() || delivery.getPhase().isDescending())
                            && pigeon.hasReachedTarget(pos)) {
                          // Complete the phase instantly
                          delivery.setPhaseProgress(pigeon().getDeliveryHandler().getPhaseDuration(level, delivery, delivery.getPhase()));
                          return;
                      }

                      if (!pigeon.getNavigation().isInProgress() || !pos.equals(pigeon.getNavigation().getTargetPos())) {
                          pigeon.pathfindDirectlyTowards(pos);
                      }
                  }); //TODO: orElse -> moveToRandomPos to simulate confusion that it doesn't know where to go
        });
    }
}