package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
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
        if (!(pigeon.level() instanceof ServerLevel level) || pigeon.getDelivery() == null) return;

        pigeon.tickDelivery(level);

        if (pigeon.getDelivery() != null && pigeon.getDelivery().getPhase().getEnd().isPresent()) {
            BlockPos pos = pigeon.getDelivery().getPhase().getEnd().get();
            if (pigeon.hasReachedTarget(pos)) {
                pigeon.getDelivery().getPhase().setTicks(pigeon.getDelivery().getPhase().getDuration());
            } else if (!pigeon.getNavigation().isInProgress()) {
                if (!pigeon.pathfindDirectlyTowards(pos)) {
                    pigeon.pathfindRandomlyTowards(pos);
                }
            }
        }
    }
}
