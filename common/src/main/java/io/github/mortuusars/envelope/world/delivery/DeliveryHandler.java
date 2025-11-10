package io.github.mortuusars.envelope.world.delivery;

import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.mail.Mail;
import net.minecraft.server.level.ServerLevel;

public interface DeliveryHandler {
    void endDelivery(ServerLevel level, Delivery delivery);

    default DeliveryPhase advancePhase(ServerLevel level, Delivery delivery, DeliveryPhase currentPhase) {
        if (currentPhase == DeliveryPhase.LOCATING_RECIPIENT
              && !level.getEnvelopeContext().getKnownAddresses().isKnown(delivery.getRecipient())) {
            delivery.setMail(Mail.returnedRecipientNotFound(delivery.getMail()));
            return DeliveryPhase.APPROACHING_SENDER;
        }

        return currentPhase.hasNext() ? currentPhase.next(delivery.getRoute().canSkipTraveling()) : currentPhase;
    }

    default int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> Ticks.fromSeconds(5);
            case LOCATING_RECIPIENT, TRAVELING_TO_RECIPIENT -> delivery.getTravelDuration() / 2;
            case TRAVELING_TO_SENDER -> delivery.getTravelDuration();
            case HANDLING_DELIVERY, HANDLING_RETURN -> Ticks.fromSeconds(0.20f);
            case STARTED, FINISHED -> 1;
        };
    }

    default void phaseStarted(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        if (phase == DeliveryPhase.STARTED) {
            delivery.setMail(Mail.sent(delivery.getMail(), level));
        }
    }

    default void phaseTicked(ServerLevel level, Delivery delivery, DeliveryPhase phase) {

    }

    default void phaseCompleted(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        if (delivery.getMail().isEmpty()) {
            return;
        }

        switch (phase) {
            case HANDLING_DELIVERY -> {
                delivery.setMail(delivery.getRecipient().receiveMail(level, delivery.getMail()));
            }
            case HANDLING_RETURN -> {
                delivery.setMail(delivery.getSender().receiveMail(level, delivery.getMail()));
            }
        }
    }
}