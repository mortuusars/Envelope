package io.github.mortuusars.envelope.world.entity.ai.goal.courier;

import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.ai.CourierNavigation;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

public class StartDeliveryFromMailboxGoal extends Goal {
    private final PhysicalCourier courier;

    public StartDeliveryFromMailboxGoal(PhysicalCourier courier) {
        this.courier = courier;
    }

    @Override
    public boolean canUse() {
        if (!MailService.operatesIn(courier.level()) || courier.isDelivering() || !courier.canStartDelivery()) {
            return false;
        }

        @Nullable BlockPos pos = courier.getMailboxHandler().getTargetPos();

        return pos != null
              && courier.closerThan(pos, CourierNavigation.getReachDistance())
              && courier.level().getBlockEntity(pos) instanceof MailboxBlockEntity blockEntity
              && blockEntity.isAvailableForPickup();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        courier.getMailboxHandler().getMailboxAtCurrentPos(courier.level())
              .ifPresent(blockEntity -> {
                  blockEntity.tryStartDelivery(courier);
                  courier.getMailboxHandler().setTargetPos(null);
              });
    }
}
