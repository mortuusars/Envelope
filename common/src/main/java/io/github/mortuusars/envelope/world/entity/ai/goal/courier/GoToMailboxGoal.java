package io.github.mortuusars.envelope.world.entity.ai.goal.courier;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

public class GoToMailboxGoal extends AbstractGoToBlockGoal {
    @Nullable
    protected Path lastPath;

    public GoToMailboxGoal(PhysicalCourier courier) {
        super(courier);
    }

    @Override
    public @Nullable BlockPos getBlockPos() {
        return courier.getMailboxHandler().getTargetPos();
    }

    @Override
    public boolean canUse() {
        return !courier.isDelivering()
              && courier.canStartDelivery()
              && super.canUse()
              && courier.level().getBlockEntity(getBlockPos()) instanceof MailboxBlockEntity blockEntity
              && blockEntity.isAvailableForPickup();
    }

    @Override
    public void tick() {
        MailboxHandler handler = courier.getMailboxHandler();

        if (handler.getTargetPos() == null) return;

        travellingTicks++;
        if (travellingTicks > adjustedTickDelay(MAX_TRAVELLING_TICKS)) {
            handler.dropAndBlacklistMailbox();
            return;
        }

        if (courier.getNavigation().isInProgress()) {
            return;
        }

        if (!courier.closerThan(handler.getTargetPos(), 16)) {
            if (handler.getTargetPos() != null
                  && Position.distanceToSqr(courier.level(), handler.getTargetPos(), courier.position()) > 32 * 32) {
                handler.dropMailbox();
            } else {
                courier.pathfindRandomlyTowards(handler.getTargetPos());
            }
        } else {
            boolean canReach = courier.pathfindDirectlyTowards(handler.getTargetPos());
            if (!canReach) {
                handler.dropAndBlacklistMailbox();
            } else if (lastPath != null && lastPath.sameAs(courier.getNavigation().getPath())) {
                ticksStuck++;
                if (ticksStuck > TICKS_BEFORE_DROP) {
                    handler.dropMailbox();
                    ticksStuck = 0;
                }
            } else {
                lastPath = courier.getNavigation().getPath();
            }
        }
    }
}
