package io.github.mortuusars.envelope.world.delivery;

import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class PigeonDeliveryHandler implements DeliveryHandler {
    private final Pigeon pigeon;

    public PigeonDeliveryHandler(Pigeon pigeon) {
        this.pigeon = pigeon;
    }

    public Pigeon pigeon() {
        return pigeon;
    }

    @Override
    public int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> Ticks.fromSeconds(15);
            default -> DeliveryHandler.super.getPhaseDuration(level, delivery, phase);
        };
    }

    @Override
    public void phaseStarted(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        DeliveryHandler.super.phaseStarted(level, delivery, phase);
        if (phase.isTraveling()) {
            pigeon().transitionToBackground(level);
        }
        pigeon().onDeliveryChanged();
    }

    @Override
    public DeliveryPhase advancePhase(ServerLevel level, Delivery delivery, DeliveryPhase currentPhase) {
        if (currentPhase == DeliveryPhase.DEPARTING_SENDER && !hasReachedSegmentEndPos(delivery)) {
            if (!delivery.getMail().isEmpty()) {
                delivery.setMail(Mail.returned(delivery.getMail(), Address.MAIL_SERVICE,
                      Component.translatable("gui.envelope.mail.log.returned.unable_to_reach")));
            }
            return DeliveryPhase.APPROACHING_SENDER;
        }

        return DeliveryHandler.super.advancePhase(level, delivery, currentPhase);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (!delivery.getMail().isEmpty()) {
            pigeon().spawnAtLocation(delivery.getMail().copy());
            delivery.setMail(ItemStack.EMPTY);
            Pigeon.LOGGER.info("{} has dropped undelivered mail on the ground because it cannot be delivered to sender Pigeonhole.",
                  pigeon().getName().getString());
        }

        if (pigeon().isService()) {
            pigeon().onVanished(level);
            pigeon().discard();
        } else {
            pigeon().setDelivery(null);
            pigeon().getPigeonholeHandler().setCurrentPos(pigeon().getPigeonholeHandler().getLastReleasePos());
            // Prevent Pigeon entering Pigeonhole immediately:
            pigeon().getPigeonholeHandler().setWantCooldown(40);
        }
    }

    protected boolean hasReachedSegmentEndPos(Delivery delivery) {
        return delivery.getRoute().getSegment(delivery.getCurrentPhase()).endPos()
              .map(pos -> pigeon().hasReachedTarget(pos))
              .orElse(true);
    }
}
