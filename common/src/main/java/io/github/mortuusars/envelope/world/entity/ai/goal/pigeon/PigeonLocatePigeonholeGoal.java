package io.github.mortuusars.envelope.world.entity.ai.goal.pigeon;

import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

public class PigeonLocatePigeonholeGoal extends Goal {
    protected final Pigeon pigeon;

    public PigeonLocatePigeonholeGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return pigeon.getPigeonholeHandler().getLocateCooldown() == 0
              && pigeon.getPigeonholeHandler().getTargetPos() == null
              && pigeon.getPigeonholeHandler().wantsToEnterPigeonhole(pigeon);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        pigeon.getPigeonholeHandler().resetLocateCooldown();
        List<BlockPos> nearbyPigeonholes = pigeon.getPigeonholeHandler()
              .findNearbyPigeonholesWithSpace((ServerLevel)pigeon.level(), pigeon.blockPosition());

        if (!nearbyPigeonholes.isEmpty()) {
            for (BlockPos pos : nearbyPigeonholes) {
                if (!pigeon.getPigeonholeHandler().isTargetBlacklisted(pos)) {
                    pigeon.getPigeonholeHandler().setTargetPos(pos);
                    return;
                }
            }

            pigeon.getPigeonholeHandler().clearBlacklist();
            pigeon.getPigeonholeHandler().setTargetPos(nearbyPigeonholes.getFirst());
        }
    }
}
