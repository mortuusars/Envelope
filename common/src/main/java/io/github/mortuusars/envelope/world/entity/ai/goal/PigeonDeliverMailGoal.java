package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
            pigeon().tickDelivery(level, delivery);

            delivery.getRoute().getSegment(delivery.getPhase()).endPos()
                  .ifPresentOrElse(localPos -> {
                      BlockPos targetLocal = localPos;
                      if (delivery.getPhase().isDescending()) {
                          BlockState state = level.getBlockState(localPos);
                          if (state.getBlock() instanceof MailboxBlock) {
                              targetLocal = localPos.relative(state.getValue(MailboxBlock.FACING));
                          }
                      }

                      if ((delivery.getPhase().isAscending() || delivery.getPhase().isDescending())
                            && pigeon.hasReachedTarget(targetLocal)) {
                          // Complete the phase instantly
                          delivery.setPhaseProgress(pigeon().getPhaseDuration(level, delivery, delivery.getPhase()));
                          return;
                      }

                      BlockPos navigationPos = Position.getNavigationPos(level, targetLocal);
                      if (!pigeon.getNavigation().isInProgress()
                            || !navigationPos.equals(pigeon.getNavigation().getTargetPos())) {
                          pigeon.pathfindDirectlyTowards(targetLocal);
                      }
                  }, () -> {
                      @Nullable Vec3 randomPos = AirAndWaterRandomPos.getPos(pigeon, 8, 4, -2,
                            pigeon.getX(), pigeon.getZ(), (float) (Math.PI / 2));
                      if (randomPos != null && level.getRandom().nextFloat() < 0.1f) {
                          pigeon.pathfindDirectlyTowards(BlockPos.containing(randomPos));
                      }
                  });
        });
    }
}