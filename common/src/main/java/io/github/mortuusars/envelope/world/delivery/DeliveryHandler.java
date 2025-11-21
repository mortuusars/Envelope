package io.github.mortuusars.envelope.world.delivery;

import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;

public interface DeliveryHandler {
    void endDelivery(ServerLevel level, Delivery delivery);

    default DeliveryPhase advancePhase(ServerLevel level, Delivery delivery, DeliveryPhase currentPhase) {
        if (currentPhase == DeliveryPhase.LOCATING_RECIPIENT
              && !level.getEnvelopeContext().addresses().canDeliverMailTo(delivery.getRecipient())) {
            delivery.updateMail(mail -> mail.writeToLog(log -> log.append(DeliveryRecord.returned_recipientNotFound())));
            return DeliveryPhase.APPROACHING_SENDER;
        }

        return currentPhase.next(canSkipTraveling(level, delivery));
    }

    default boolean canSkipTraveling(ServerLevel level, Delivery delivery) {
        return delivery.getRoute().getDistance()
              .map(distance -> distance < DeliveryRoute.DEFAULT_ASCEND_DISTANCE * 2)
              .orElse(false);
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
            delivery.updateMail(mail -> mail.writeToLog(log ->
                  log.append(DeliveryRecord.sentFrom(mail.getSenderOrElse(Address.UNKNOWN)).atTime(level.getGameTime()))));
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
                delivery.updateMail(mail -> delivery.getRecipient().receiveMail(level, mail));
            }
            case HANDLING_RETURN -> {
                delivery.updateMail(mail -> delivery.getSender().receiveMail(level, mail));
            }
        }
    }
}