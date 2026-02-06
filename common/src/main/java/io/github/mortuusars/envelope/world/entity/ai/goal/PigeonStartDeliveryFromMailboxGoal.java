package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

public class PigeonStartDeliveryFromMailboxGoal extends Goal {
    private final Pigeon pigeon;

    public PigeonStartDeliveryFromMailboxGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
    }

    @Override
    public boolean canUse() {
        if (!MailService.operatesIn(pigeon.level()) || pigeon.isDelivering() || !pigeon.canStartDelivery()) {
            return false;
        }

        @Nullable BlockPos pos = pigeon.getMailboxHandler().getTargetPos();

        return pos != null
              && pos.closerToCenterThan(pigeon.position(), 2.0)
              && pigeon.level().getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity
              && blockEntity.isAvailableForPickup();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        pigeon.getMailboxHandler().getMailboxAtCurrentPos(pigeon.level())
              .ifPresent(blockEntity -> {
                  blockEntity.tryStartDelivery(pigeon);
                  pigeon.getMailboxHandler().setTargetPos(null);
              });
    }
}
