package io.github.mortuusars.envelope.world.entity.ai.goal.courier;

import io.github.mortuusars.envelope.world.entity.ai.CourierNavigation;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

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
        if (!(courier.asCourierEntity().level() instanceof ServerLevel level)) {
            return;
        }

        courier.getCurrentDelivery().ifPresent(delivery -> {
            courier.tickDelivery(level, delivery);

            delivery.getRoute().getSegment(delivery.getPhase()).endPos()
                  .ifPresentOrElse(localPos -> {
                      BlockPos targetLocal = delivery.getPhase().isDescending()
                            ? CourierNavigation.getMailboxApproachTarget(level, localPos)
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
                      @Nullable Vec3 randomPos = MobAdapter.getPos(courier.asCourierEntity(), 8, 4, -2,
                            courier.position().x(), courier.position().z(), (float) (Math.PI / 2));
                      if (randomPos != null && level.getRandom().nextFloat() < 0.1f) {
                          courier.pathfindDirectlyTowards(BlockPos.containing(randomPos));
                      }
                  });
        });
    }

    public static class MobAdapter {
        @Nullable
        public static Vec3 getPos(Mob mob, int maxDistance, int yRange, int y, double x, double z, double amplifier) {
            boolean bl = GoalUtilsAdapter.mobRestricted(mob, maxDistance);
            return generateRandomPos(mob, () -> generateRandomPos(mob, maxDistance, yRange, y, x, z, amplifier, bl));
        }

        @Nullable
        public static BlockPos generateRandomPos(Mob mob, int maxDistance, int yRange, int y, double x, double z, double amplifier, boolean shortCircuit) {
            BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(mob.getRandom(), maxDistance, yRange, y, x, z, amplifier);
            if (blockPos == null) {
                return null;
            } else {
                BlockPos blockPos2 = generateRandomPosTowardDirection(mob, maxDistance, mob.getRandom(), blockPos);
                if (!GoalUtilsAdapter.isOutsideLimits(blockPos2, mob) && !GoalUtilsAdapter.isRestricted(shortCircuit, mob, blockPos2)) {
                    blockPos2 = RandomPos.moveUpOutOfSolid(blockPos2, mob.level().getMaxBuildHeight(), (p) -> GoalUtilsAdapter.isSolid(mob, p));
                    return GoalUtilsAdapter.hasMalus(mob, blockPos2) ? null : blockPos2;
                } else {
                    return null;
                }
            }
        }

        public static BlockPos generateRandomPosTowardDirection(Mob mob, int range, RandomSource random, BlockPos pos) {
            int i = pos.getX();
            int j = pos.getZ();
            if (mob.hasRestriction() && range > 1) {
                BlockPos blockPos = mob.getRestrictCenter();
                if (mob.getX() > (double)blockPos.getX()) {
                    i -= random.nextInt(range / 2);
                } else {
                    i += random.nextInt(range / 2);
                }

                if (mob.getZ() > (double)blockPos.getZ()) {
                    j -= random.nextInt(range / 2);
                } else {
                    j += random.nextInt(range / 2);
                }
            }

            return BlockPos.containing((double)i + mob.getX(), (double)pos.getY() + mob.getY(), (double)j + mob.getZ());
        }

        @Nullable
        public static Vec3 generateRandomPos(Mob mob, Supplier<BlockPos> posSupplier) {
            Objects.requireNonNull(mob);
            return generateRandomPos(posSupplier, pos -> 0.0);
        }

        @Nullable
        public static Vec3 generateRandomPos(Supplier<BlockPos> posSupplier, ToDoubleFunction<BlockPos> toDoubleFunction) {
            double d = Double.NEGATIVE_INFINITY;
            BlockPos blockPos = null;

            for(int i = 0; i < 10; ++i) {
                BlockPos blockPos2 = posSupplier.get();
                if (blockPos2 != null) {
                    double e = toDoubleFunction.applyAsDouble(blockPos2);
                    if (e > d) {
                        d = e;
                        blockPos = blockPos2;
                    }
                }
            }

            return blockPos != null ? Vec3.atBottomCenterOf(blockPos) : null;
        }

        @Nullable
        public static Vec3 getPosTowards(Mob mob, int radius, int yRange, int y, Vec3 vectorPosition, double amplifier) {
            Vec3 vec3 = vectorPosition.subtract(mob.getX(), mob.getY(), mob.getZ());
            boolean bl = GoalUtilsAdapter.mobRestricted(mob, radius);
            return generateRandomPos(mob, () -> {
                BlockPos blockPos = generateRandomPos(mob, radius, yRange, y, vec3.x, vec3.z, amplifier, bl);
                return blockPos != null && !GoalUtilsAdapter.isWater(mob, blockPos) ? blockPos : null;
            });
        }
    }

    public static class GoalUtilsAdapter {
        public static boolean hasGroundPathNavigation(Mob mob) {
            return mob.getNavigation() instanceof GroundPathNavigation;
        }

        public static boolean mobRestricted(Mob mob, int radius) {
            return mob.hasRestriction() && mob.getRestrictCenter().closerToCenterThan(mob.position(), (double)(mob.getRestrictRadius() + (float)radius) + (double)1.0F);
        }

        public static boolean isOutsideLimits(BlockPos pos, Mob mob) {
            return pos.getY() < mob.level().getMinBuildHeight() || pos.getY() > mob.level().getMaxBuildHeight();
        }

        public static boolean isRestricted(boolean shortCircuit, Mob mob, BlockPos pos) {
            return shortCircuit && !mob.isWithinRestriction(pos);
        }

        public static boolean isNotStable(PathNavigation navigation, BlockPos pos) {
            return !navigation.isStableDestination(pos);
        }

        public static boolean isWater(Mob mob, BlockPos pos) {
            return mob.level().getFluidState(pos).is(FluidTags.WATER);
        }

        public static boolean hasMalus(Mob mob, BlockPos pos) {
            return mob.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(mob, pos)) != 0.0F;
        }

        public static boolean isSolid(Mob mob, BlockPos pos) {
            return mob.level().getBlockState(pos).isSolid();
        }
    }
}