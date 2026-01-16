package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

public class PigeonGoToMailboxGoal extends AbstractGoToBlockGoal {
    @Nullable
    protected Path lastPath;

    public PigeonGoToMailboxGoal(Pigeon pigeon) {
        super(pigeon);
    }

    @Override
    public @Nullable BlockPos getBlockPos() {
        return pigeon.getMailboxHandler().getTargetPos();
    }

    @Override
    public boolean canUse() {
        return !pigeon.isDelivering()
              && super.canUse()
              && !pigeon.isTired()
              && pigeon.level().getBlockEntity(getBlockPos()) instanceof MailboxBlockEntity blockEntity
              && blockEntity.isAvailableForPickup();
    }

    @Override
    public void tick() {
        MailboxHandler handler = pigeon.getMailboxHandler();

        if (handler.getTargetPos() == null) return;

        travellingTicks++;
        if (travellingTicks > adjustedTickDelay(MAX_TRAVELLING_TICKS)) {
            handler.dropAndBlacklistMailbox();
            return;
        }

        if (pigeon.getNavigation().isInProgress()) {
            return;
        }

        if (!pigeon.closerThan(handler.getTargetPos(), 16)) {
            if (!pigeon.blockPosition().closerThan(handler.getTargetPos(), 32)) {
                handler.dropMailbox();
            } else {
                pigeon.pathfindRandomlyTowards(handler.getTargetPos());
            }
        } else {
            boolean canReach = pigeon.pathfindDirectlyTowards(handler.getTargetPos());
            if (!canReach) {
                handler.dropAndBlacklistMailbox();
            } else if (lastPath != null && lastPath.sameAs(pigeon.getNavigation().getPath())) {
                ticksStuck++;
                if (ticksStuck > TICKS_BEFORE_DROP) {
                    handler.dropMailbox();
                    ticksStuck = 0;
                }
            } else {
                lastPath = pigeon.getNavigation().getPath();
            }
        }
    }
}
