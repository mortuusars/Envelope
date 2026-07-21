package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PigeonWanderGoal extends WaterAvoidingRandomFlyingGoal {
    public static final int WANDER_RADIUS = 24;

    protected final Pigeon pigeon;

    public PigeonWanderGoal(Pigeon pigeon, double speedModifier) {
        super(pigeon, speedModifier);
        this.pigeon = pigeon;
        interval = 100;
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        if (pigeon.isInWaterOrBubble()) {
            @Nullable Vec3 pos = LandRandomPos.getPos(pigeon, 15, 15);
            if (pos != null) {
                return pos;
            }
        }

        @Nullable BlockPos homePos = pigeon.getPigeonholeHandler().getHomePos();
        if (homePos != null) {
            homePos = BlockPos.containing(Position.getGlobalCenter(pigeon.level(), homePos));
        }

        @Nullable Vec3 newPos = choosePos(homePos);
        if (newPos == null || homePos == null) {
            return newPos;
        }

        double newDistanceToHomeSqr = homePos.distToCenterSqr(newPos);

        if (newDistanceToHomeSqr < WANDER_RADIUS * WANDER_RADIUS
              || newDistanceToHomeSqr <= homePos.distToCenterSqr(pigeon.position())) { // If it brings the pigeon closer to home
            return newPos;
        }

        return null;
    }

    private @Nullable Vec3 choosePos(@Nullable BlockPos homePos) {
        if (pigeon.isSitting() && (pigeon.isTired() || pigeon.getRandom().nextFloat() < 0.75f)) {
            // Returning null keeps pigeon sitting pose.
            // When tired, sitting is controlled by PigeonSitGoal.
            // But if perched, we keep the pose with 75% chance.
            return null;
        }

        if (pigeon.getRandom().nextFloat() < 0.2) {
            // Parrots use very low probability (0.001 or so) for their "perching".
            // But even with 0.2 it does not happen that often in my tests.
            @Nullable Vec3 pos = getPerchPos();
            if (pos != null) {
                return pos;
            }
        }

        if (pigeon.getRandom().nextFloat() < 0.2f
              && pigeon.level().getBlockState(pigeon.blockPosition().below()).is(Envelope.Tags.Blocks.PIGEONS_PERCHABLE_ON)) {
            pigeon.setSitting(true);
            return null;
        }

        Vec3 direction = homePos != null && homePos.distToCenterSqr(pigeon.position()) > WANDER_RADIUS * WANDER_RADIUS
              ? Vec3.atCenterOf(homePos).subtract(pigeon.position()).normalize() // Bias towards home
              : pigeon.getViewVector(0.0F); // Bias towards facing

        int range = 8;
        int yRange = 8;

        Vec3 hoverPos = HoverRandomPos.getPos(pigeon, range, yRange,
              direction.x, direction.z, (float) (Math.PI / 2), 3, 1);
        if (hoverPos != null) {
            return hoverPos;
        }

        return AirAndWaterRandomPos.getPos(pigeon, range, yRange, -1,
              direction.x, direction.z, (float) (Math.PI / 2));
    }

    protected @Nullable Vec3 getPerchPos() {
        BlockPos currentPos = pigeon.blockPosition();
        BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();

        Iterable<BlockPos> positions = BlockPos.betweenClosed(
              Mth.floor(pigeon.getX() - 3.0), Mth.floor(pigeon.getY() - 6.0), Mth.floor(pigeon.getZ() - 3.0),
              Mth.floor(pigeon.getX() + 3.0), Mth.floor(pigeon.getY() + 6.0), Mth.floor(pigeon.getZ() + 3.0)
        );

        for (BlockPos pos : positions) {
            if (currentPos.equals(pos)) {
                continue;
            }

            BlockState stateBelow = pigeon.level().getBlockState(belowPos.setWithOffset(pos, Direction.DOWN));
            if (stateBelow.is(Envelope.Tags.Blocks.PIGEONS_PERCHABLE_ON)
                  && isPathfindable(pos)
                  && isPathfindable(abovePos.setWithOffset(pos, Direction.UP))) {
                return Vec3.atBottomCenterOf(pos);
            }
        }

        return null;
    }

    protected boolean isPathfindable(BlockPos pos) {
        return pigeon.level().isEmptyBlock(pos) || pigeon.level().getBlockState(pos).isPathfindable(PathComputationType.AIR);
    }
}