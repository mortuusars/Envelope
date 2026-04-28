package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class PigeonAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
    private final Pigeon pigeon;
    private final TargetingConditions targetingConditions;

    public PigeonAvoidEntityGoal(Pigeon pigeon, Class<T> entityClassToAvoid, float maxDistance, double walkSpeedModifier,
                                 double sprintSpeedModifier, Predicate<LivingEntity> predicateOnAvoidEntity) {
        super(pigeon, entityClassToAvoid, maxDistance, walkSpeedModifier, sprintSpeedModifier, predicateOnAvoidEntity);
        this.pigeon = pigeon;
        this.targetingConditions = TargetingConditions.forCombat().range(maxDistance).selector(predicateOnAvoidEntity.and(avoidPredicate));
    }

    public boolean isAvoiding() {
        return toAvoid != null;
    }

    @Override
    public boolean canUse() {
        // Add some delay, or else pigeon is too good at avoiding
        if (pigeon.getRandom().nextInt(6) != 0) {
            return false;
        }

        toAvoid = pigeon.level().getNearestEntity(
              pigeon.level().getEntitiesOfClass(avoidClass, pigeon.getBoundingBox().inflate(maxDist, maxDist, maxDist), entity -> true),
              targetingConditions, pigeon, pigeon.getX(), pigeon.getY(), pigeon.getZ()
        );

        if (toAvoid == null) {
            return false;
        }

        Vec3 pos = DefaultRandomPos.getPosAway(pigeon, 14, 8, toAvoid.position());

        if (pos == null) {
            return false;
        } else if (toAvoid.distanceToSqr(pos.x, pos.y, pos.z) < toAvoid.distanceToSqr(pigeon)) {
            return false;
        } else {
            path = pathNav.createPath(pos.x, pos.y, pos.z, 0);
            return path != null;
        }
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