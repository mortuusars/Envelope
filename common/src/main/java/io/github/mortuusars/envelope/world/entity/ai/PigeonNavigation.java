package io.github.mortuusars.envelope.world.entity.ai;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryPhase;
import net.minecraft.core.BlockPos;
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
public final class PigeonNavigation {
    private PigeonNavigation() {
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
    public static boolean hasReachedTarget(Pigeon pigeon, BlockPos localPos, double distance) {
        if (isWithinReach(pigeon, localPos, distance)) {
            return true;
        }

        Level level = pigeon.level();
        BlockPos navigationPos = Position.getNavigationPos(level, localPos);
        PathNavigation navigation = pigeon.getNavigation();
        @Nullable Path path = navigation.getPath();

        if (path != null && path.canReach() && path.isDone() && path.getTarget().equals(navigationPos)) {
            return true;
        }

        BlockPos navigationTarget = navigation.getTargetPos();
        return !navigation.isInProgress()
              && navigationTarget != null
              && navigationTarget.equals(navigationPos)
              && navigationTarget.closerThan(pigeon.blockPosition(), distance);
    }

    public static boolean isWithinReach(Pigeon pigeon, BlockPos localPos, double distance) {
        return Position.isWithinReach(pigeon.level(), localPos, pigeon.position(), pigeon.blockPosition(), distance);
    }

    public static BlockPos getNavigationPos(Pigeon pigeon, BlockPos localPos) {
        return Position.getNavigationPos(pigeon.level(), localPos);
    }
}
