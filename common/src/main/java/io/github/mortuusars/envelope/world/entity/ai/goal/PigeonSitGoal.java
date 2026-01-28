package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.world.entity.ai.goal.Goal;

public class PigeonSitGoal extends Goal {
    private final Pigeon pigeon;
    private int time;
    private long cooldownUntil;

    public PigeonSitGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return !pigeon.isDelivering()
              && pigeon.isTired() && pigeon.getTiredTicks() > 100
              && !pigeon.isPanicking()
              && pigeon.onGround()
              && pigeon.level().getGameTime() >= cooldownUntil
              && pigeon.getRandom().nextInt(20) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return pigeon.isTired() && pigeon.isSitting() && time < 600;
    }

    @Override
    public void start() {
        pigeon.setSitting(true);
        time = 0;
    }

    @Override
    public void stop() {
        pigeon.setSitting(false);
        cooldownUntil = pigeon.level().getGameTime() + 100;
    }

    @Override
    public void tick() {
        if (pigeon.isSitting()) {
            time++;
        }
    }
}
