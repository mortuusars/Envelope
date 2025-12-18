package io.github.mortuusars.envelope.world.delivery;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;

public interface DeliveryHandler {
    void endDelivery(ServerLevel level, Delivery delivery);

    default DeliveryPhase advancePhase(ServerLevel level, Delivery delivery, DeliveryPhase currentPhase) {
        if (currentPhase == DeliveryPhase.LOCATING_RECIPIENT) {
            if (!MailService.of(level).canDeliverMailTo(delivery.getRecipient())) {
                delivery.updateMail(mail -> mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                      .message(DeliveryRecord.Message.RECIPIENT_NOT_FOUND)));
                return DeliveryPhase.APPROACHING_SENDER;
            }
        }

        return currentPhase.next(canSkipTraveling(level, delivery));
    }

    default boolean canSkipTraveling(ServerLevel level, Delivery delivery) {
        if (delivery.getMail().has(Envelope.DataComponents.PAYBACK)) {
            return false;
        }

        return delivery.getRoute().getDistance()
              .map(distance -> distance < DeliveryRoute.DEFAULT_ASCEND_DISTANCE * 2)
              .orElse(false);
    }

    default int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> (int)Ticks.fromSeconds(5);
            case LOCATING_RECIPIENT, TRAVELING_TO_RECIPIENT -> delivery.getTravelDuration().ticks() / 2;
            case TRAVELING_TO_SENDER -> delivery.getTravelDuration().ticks();
            case HANDLING_DELIVERY, HANDLING_RETURN -> (int)Ticks.fromSeconds(0.20f);
            case STARTED, FINISHED -> 1;
        };
    }

    default void phaseStarted(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        if (phase == DeliveryPhase.STARTED) {
            delivery.updateMail(mail -> mail.writeToLog(DeliveryRecord.sentFrom(delivery.getSender())
                  .at(level.getGameTime())));
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
                delivery.updateMail(mail -> MailService.of(level).receiveMail(level, delivery.getRecipient(), mail));
            }
            case HANDLING_RETURN -> {
                delivery.updateMail(mail -> MailService.of(level).receiveMail(level, delivery.getSender(), mail));
            }
        }
    }
}