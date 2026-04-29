package io.github.mortuusars.envelope.world.entity.ai.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;

import java.util.function.Predicate;

public class FollowSpecificMobGoal extends FollowMobGoal {
    protected final Predicate<Mob> predicate;

    public FollowSpecificMobGoal(Mob mob, Predicate<Mob> predicate, double speedModifier, float stopDistance, float areaSize) {
        super(mob, speedModifier, stopDistance, areaSize);
        this.predicate = predicate;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && followingMob != null && predicate.test(followingMob);
    }
}
