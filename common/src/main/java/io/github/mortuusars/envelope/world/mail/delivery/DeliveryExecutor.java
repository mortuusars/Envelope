package io.github.mortuusars.envelope.world.mail.delivery;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public interface DeliveryExecutor {
    Logger LOGGER = LogUtils.getLogger();

    CourierOrigin getOrigin();

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
                    String type = getOrigin().isService() ? "Service delivery" : "Delivery";
                    Envelope.LOGGER.info("{} '{} > {}' is finished.", type, delivery.getSender(), delivery.getRecipient());
                }
            } else {
                if (!handlePhaseTransition(level, delivery)) {
                    delivery.setPhaseAndResetProgress(delivery.getPhase().next());
                }
            }
        }
    }

    /**
     * Allows manually handling selection of next phase. <br>
     * Jumping to phases non-linearly should be done here. (to return early, etc.)
     *
     * @return {@code true} to skip selection of next phase.<br>
     * {@code false} to select next phase in order.
     */
    default boolean handlePhaseTransition(ServerLevel level, Delivery delivery) {
        if (delivery.getPhase() == DeliveryPhase.DISPATCHING_DELIVERY) {
            return dispatchDelivery(level, delivery);
        }

        if (delivery.getPhase() == DeliveryPhase.DISPATCHING_RETURN) {
            return dispatchReturn(level, delivery);
        }

        return false;
    }

    default int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            case STARTED, FINISHED -> 8;
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> 5 * Ticks.SECOND;
            case TRAVELING_FROM_SENDER_TO_HUB, TRAVELING_FROM_HUB_TO_SENDER -> delivery.getRoute().getSenderToHubDuration().ticks();
            case TRAVELING_FROM_HUB_TO_RECIPIENT, TRAVELING_FROM_RECIPIENT_TO_HUB ->
                  delivery.getRoute().getRecipientToHubDuration().ticks();
            case DISPATCHING_DELIVERY, DISPATCHING_RETURN -> 20;
            case HANDLING_DELIVERY, HANDLING_RETURN -> 10;
        };
    }

    default void phaseStarted(ServerLevel level, Delivery delivery) {
        if (delivery.getPhase() == DeliveryPhase.STARTED) {
            Mail.writeToLog(delivery.getMail(), DeliveryRecord.sentFrom(delivery.getSender()));
        }
    }

    default void phaseTicked(ServerLevel level, Delivery delivery) {

    }

    default void phaseCompleted(ServerLevel level, Delivery delivery) {
        if (delivery.getMail().isEmpty()) {
            return;
        }

        final MailService service = MailService.of(level);

        switch (delivery.getPhase()) {
            case HANDLING_DELIVERY -> {
                MailDropOffContext context = new MailDropOffContext(service, delivery.getRecipient(), delivery);
                MailDropOffResult result = service.getDeliveryManager().handleMailDropOff(context);
                if (result.isHandled()) {
                    ItemStack mail = result.getMail();
                    if (result.isReply()) {
                        Mail.writeToLog(mail, DeliveryRecord.sentFrom(delivery.getRecipient()));
                    }
                    delivery.setMail(mail);
                }
            }
            case HANDLING_RETURN -> {
                MailDropOffContext context = new MailDropOffContext(service, delivery.getSender(), delivery);
                MailDropOffResult result = service.getDeliveryManager().handleMailDropOff(context);
                if (result.isHandled()) {
                    delivery.setMail(result.getMail());
                }
            }
        }
    }

    default void endDelivery(ServerLevel level, Delivery delivery) {
    }

    // --

    default boolean dispatchDelivery(ServerLevel level, Delivery delivery) {
        MailService mailService = MailService.of(level);

        Mail.setSender(delivery.getMail(), delivery.getSender());

        if (!mailService.getDeliveryManager().canDeliverTo(delivery.getRecipient())) {
            Mail.returned(delivery.getMail(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND);
            delivery.setPhaseAndResetProgress(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER);
            return true;
        }

        if (mailService.getPaybackDepartment().tryHandleDelivery(delivery)) {
            return true;
        }

        delivery.updateRoute(level);
        return false;
    }

    default boolean dispatchReturn(ServerLevel level, Delivery delivery) {
        if (MailService.of(level).getPaybackDepartment().tryHandleReturn(delivery)) {
            return true;
        }

        if (!Mail.isReturned(delivery.getMail()) && Mail.getSender(delivery.getMail()).isEmpty()) {
            Mail.setSender(delivery.getMail(), delivery.getRecipient());
        }

        if (getOrigin().isService() && shouldTerminateServiceDeliveryEarly(level, delivery)) {
            LOGGER.debug("Terminating service delivery [{}] early.", delivery.getId());
            delivery.setPhaseAndResetProgress(DeliveryPhase.FINISHED);
            return true;
        }

        delivery.updateRoute(level);
        return false;
    }

    /**
     * Whether a service delivery should be ended early (when dispatching return),
     * to save on processing and avoid spawning the courier when it doesn't deliver anything.
     * <br>
     * This can cause side effects, if some logic depends on courier completing specific phase, even if empty.
     */
    default boolean shouldTerminateServiceDeliveryEarly(ServerLevel level, Delivery delivery) {
        return delivery.getMail().isEmpty();
    }
}