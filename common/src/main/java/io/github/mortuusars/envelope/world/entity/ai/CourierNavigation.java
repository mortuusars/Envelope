package io.github.mortuusars.envelope.world.entity.ai;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryPhase;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Pathfinding and reach checks for pigeons interacting with local block targets.
 * <p>
 * Local {@link BlockPos} values may live in a Sable sub-level plot grid; callers should pass those
 * stored positions and let {@link Position} project them when comparing or navigating.
 */
public final class CourierNavigation {
    private CourierNavigation() {
    }

    public static double getReachDistance() {
        return 1;
    }

    /**
     * Block in front of a mailbox face — where pigeons stand when approaching for delivery.
     */
    public static BlockPos getMailboxApproachTarget(Level level, BlockPos mailboxPos) {
        BlockState state = level.getBlockState(mailboxPos);
        if (state.getBlock() instanceof MailboxBlock) {
            return mailboxPos.relative(state.getValue(MailboxBlock.FACING));
        }
        return mailboxPos;
    }

    public static BlockPos getSegmentApproachTarget(Level level, BlockPos segmentEndPos, DeliveryPhase phase) {
        return phase.isDescending()
              ? getMailboxApproachTarget(level, segmentEndPos)
              : segmentEndPos;
    }

    /**
     * Whether a pigeon has reached a local target block.
     * <ol>
     *   <li>Within {@code distance} in projected world space, or via legacy block-grid distance.</li>
     *   <li>Current path finished at the projected navigation target.</li>
     *   <li>Navigation idle at the projected target and within block-grid distance of it.</li>
     * </ol>
     */
    public static boolean hasReachedTarget(PhysicalCourier courier, BlockPos localPos, double distance) {
        if (isWithinReach(courier, localPos, distance)) {
            return true;
        }

        Level level = courier.asCourierEntity().level();
        BlockPos navigationPos = Position.getNavigationPos(level, localPos);
        PathNavigation navigation = courier.getNavigation();
        @Nullable Path path = navigation.getPath();

        if (path != null && path.canReach() && path.isDone() && path.getTarget().equals(navigationPos)) {
            return true;
        }

        BlockPos navigationTarget = navigation.getTargetPos();
        return !navigation.isInProgress()
              && navigationTarget != null
              && navigationTarget.equals(navigationPos)
              && navigationTarget.closerThan(courier.asCourierEntity().blockPosition(), distance);
    }

    public static boolean isWithinReach(PhysicalCourier courier, BlockPos localPos, double distance) {
        LivingEntity entity = courier.asCourierEntity();
        return Position.isWithinReach(entity.level(), localPos, entity.position(), entity.blockPosition(), distance);
    }

    public static BlockPos getNavigationPos(PhysicalCourier courier, BlockPos localPos) {
        return Position.getNavigationPos(courier.asCourierEntity().level(), localPos);
    }
}
