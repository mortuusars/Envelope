package io.github.mortuusars.envelope.world.delivery;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.item.component.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryRecord;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface DeliveryHandler {
    default void tickDelivery(ServerLevel level, Delivery delivery) {
        if (delivery.isEnded()) {
            return;
        }

        if (delivery.getPhaseProgress() == 0) {
            phaseStarted(level, delivery);
        }

        delivery.incrementCurrentPhaseProgress();
        phaseTicked(level, delivery);

        if (delivery.getPhaseProgress() >= getPhaseDuration(level, delivery, delivery.getPhase())) {
            phaseCompleted(level, delivery);

            if (delivery.getPhase() == DeliveryPhase.FINISHED) {
                Preconditions.checkState(!delivery.isEnded(), "Delivery is already ended.");
                delivery.end();
                endDelivery(level, delivery);
                if (Bugger.isEnabled()) {
                    Envelope.LOGGER.info("Delivery '{} > {}' is finished.", delivery.getSender(), delivery.getRecipient());
                }
            } else {
                advancePhase(level, delivery);
            }
        }
    }

    /**
     * Handles next phase selection.<br>
     * Jumping to phases non-linearly should be done here. (to return early, etc.)
     */
    default void advancePhase(ServerLevel level, Delivery delivery) {
        if (delivery.getPhase() == DeliveryPhase.DISPATCHING) {
            MailService.of(level).getDeliveryManager().dispatch(delivery);
        } else {
            DeliveryPhase nextPhase = delivery.getPhase().next();
            delivery.setPhaseAndResetProgress(nextPhase);
        }
    }

    default int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            case STARTED, FINISHED -> 1;
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> 5 * Ticks.SECOND;
            case TRAVELING_TO_MAIL_HUB, TRAVELING_TO_RECIPIENT -> delivery.getRoute().travelDuration().ticks() / 2;
            case RETURNING_TO_SENDER -> delivery.getRoute().travelDuration().ticks();
            case DISPATCHING -> 20;
            case HANDLING_DELIVERY, HANDLING_RETURN -> 5;
        };
    }

    default void phaseStarted(ServerLevel level, Delivery delivery) {
        if (!delivery.getPhase().isTraveling() || delivery.getRoute() == DeliveryRoute.EMPTY) {
            delivery.updateRoute(level);
        }

        if (delivery.getPhase() == DeliveryPhase.STARTED) {
            Mail.writeToLog(delivery.getMail(), DeliveryRecord.sentFrom(delivery.getSender()).at(level.getGameTime()));
        }
    }

    default void phaseTicked(ServerLevel level, Delivery delivery) {

    }

    default void phaseCompleted(ServerLevel level, Delivery delivery) {
        if (delivery.getMail().isEmpty()) {
            return;
        }

        switch (delivery.getPhase()) {
            case HANDLING_DELIVERY -> {
                ItemStack result = MailService.of(level).deliverMail(delivery.getRecipient(), delivery.getMail());
                delivery.setMail(result);
            }
            case HANDLING_RETURN -> {
                ItemStack result = MailService.of(level).deliverMail(delivery.getSender(), delivery.getMail());
                delivery.setMail(result);
            }
        }
    }

    default void endDelivery(ServerLevel level, Delivery delivery) {
    }
}