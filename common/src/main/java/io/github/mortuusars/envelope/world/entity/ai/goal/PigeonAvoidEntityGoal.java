package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class PigeonAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
    private final Pigeon pigeon;

    public PigeonAvoidEntityGoal(Pigeon pigeon, Class<T> entityClassToAvoid, float maxDistance,
                                 double walkSpeedModifier, double sprintSpeedModifier) {
        super(pigeon, entityClassToAvoid, maxDistance, walkSpeedModifier, sprintSpeedModifier);
        this.pigeon = pigeon;
    }

    public PigeonAvoidEntityGoal(Pigeon pigeon, Class<T> entityClassToAvoid, Predicate<LivingEntity> avoidPredicate,
                                 float maxDistance, double walkSpeedModifier, double sprintSpeedModifier, Predicate<LivingEntity> predicateOnAvoidEntity) {
        super(pigeon, entityClassToAvoid, avoidPredicate, maxDistance, walkSpeedModifier, sprintSpeedModifier, predicateOnAvoidEntity);
        this.pigeon = pigeon;
    }

    public PigeonAvoidEntityGoal(Pigeon pigeon, Class<T> entityClassToAvoid, float maxDistance, double walkSpeedModifier,
                                 double sprintSpeedModifier, Predicate<LivingEntity> predicateOnAvoidEntity) {
        super(pigeon, entityClassToAvoid, maxDistance, walkSpeedModifier, sprintSpeedModifier, predicateOnAvoidEntity);
        this.pigeon = pigeon;
    }

    public boolean isAvoiding() {
        return toAvoid != null;
    }

    @Override
    public boolean canUse() {
        if (!super.canUse() && toAvoid != null) {
            Vec3 pos = getPosAway(this.mob, 6, 3, this.toAvoid.position());
            if (pos == null) return false;
            if (this.toAvoid.distanceToSqr(pos.x, pos.y, pos.z) < this.toAvoid.distanceToSqr(this.mob)) return false;
            if (pos.y() < mob.getY()) return false;

            this.path = this.pathNav.createPath(pos.x, pos.y, pos.z, 0);
            return this.path != null;
        }

        return false;
    }

    @Nullable
    public static Vec3 getPosAway(PathfinderMob mob, int radius, int yRange, Vec3 vectorPosition) {
        Vec3 awayVector = mob.position().subtract(vectorPosition);
        boolean isWithinRestriction = GoalUtils.mobRestricted(mob, radius);
        return RandomPos.generateRandomPos(mob, () -> {
            BlockPos blockPos = AirAndWaterRandomPos.generateRandomPos(mob, radius, yRange, 3, awayVector.x, awayVector.z, 1, isWithinRestriction);
            return blockPos != null && !GoalUtils.isWater(mob, blockPos) ? blockPos : null;
        });
    }
}