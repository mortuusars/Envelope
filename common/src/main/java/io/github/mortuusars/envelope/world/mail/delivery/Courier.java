package io.github.mortuusars.envelope.world.mail.delivery;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.dropoff.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;

public interface Courier {
    Logger LOGGER = LogUtils.getLogger();

    MailDropOffHandler DROP_OFF_HANDLER = MailDropOffHandler.chain(
          new BaseDropOffHandler(),
          new BlockDropOffHandler(),
          new PlayerDropOffHandler(),
          new ServiceDropOffHandler(),
          context -> MailDropOffResult.returned(context.getMail())
    );

    Optional<Delivery> getCurrentDelivery();

    CourierOrigin getOrigin();

    default boolean isDelivering() {
        return getCurrentDelivery().isPresent();
    }

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
                    LOGGER.info("{} '{} > {}' is finished.", type, delivery.getSender(), delivery.getRecipient());
                }
            } else {
                if (!handlePhaseTransition(level, delivery)) {
                    delivery.beginPhase(delivery.getPhase().next());
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
            case STARTED, FINISHED -> 5;
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> 100;
            case TRAVELING_FROM_SENDER_TO_HUB, TRAVELING_FROM_HUB_TO_SENDER -> delivery.getRoute().getSenderToHubDuration().ticks();
            case TRAVELING_FROM_HUB_TO_RECIPIENT, TRAVELING_FROM_RECIPIENT_TO_HUB ->
                  delivery.getRoute().getRecipientToHubDuration().ticks();
            case DISPATCHING_DELIVERY, DISPATCHING_RETURN -> 20;
            case HANDLING_DELIVERY, HANDLING_RETURN -> 10;
        };
    }

    default void phaseStarted(ServerLevel level, Delivery delivery) {
    }

    default void phaseTicked(ServerLevel level, Delivery delivery) {
    }

    default void phaseCompleted(ServerLevel level, Delivery delivery) {
        if (delivery.getMail().isEmpty()) {
            return;
        }

        switch (delivery.getPhase()) {
            case STARTED -> Mail.writeToLog(delivery.getMail(), DeliveryRecord.sentFrom(delivery.getSender()));
            case HANDLING_DELIVERY -> handleMailDropOff(level, delivery, delivery.getRecipient());
            case HANDLING_RETURN -> handleMailDropOff(level, delivery, delivery.getSender());
        }
    }

    default void endDelivery(ServerLevel level, Delivery delivery) {
    }

    // --

    default boolean dispatchDelivery(ServerLevel level, Delivery delivery) {
        MailService service = MailService.of(level);

        if (Mail.getSender(delivery.getMail()).isEmpty()) {
            Mail.setSender(delivery.getMail(), delivery.getSender());
        }

        if (!service.getDeliveryManager().canDeliverTo(delivery.getRecipient())) {
            Mail.returned(delivery.getMail(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND);
            delivery.beginPhase(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER);
            return true;
        }

        if (service.getPaybackDepartment().tryHandleDelivery(delivery)) {
            return true;
        }

        delivery.updateRoute(level);
        return false;
    }

    default boolean dispatchReturn(ServerLevel level, Delivery delivery) {
        MailService service = MailService.of(level);

        if (!Mail.isReturned(delivery.getMail()) && Mail.getSender(delivery.getMail()).isEmpty()) {
            Mail.setSender(delivery.getMail(), delivery.getRecipient());
        }

        if (service.getPaybackDepartment().tryHandleReturn(delivery)) {
            return true;
        }

        if (getOrigin().isService() && shouldTerminateServiceDeliveryEarly(level, delivery)) {
            LOGGER.debug("Terminating service delivery [{}] early.", delivery.getId());
            delivery.beginPhase(DeliveryPhase.FINISHED);
            return true;
        }

        if (service.getDeliveryManager().canDeliverTo(delivery.getSender())) {
            delivery.updateRoute(level);
        }

        return false;
    }

    default void handleMailDropOff(ServerLevel level, Delivery delivery, Address recipient) {
        if (Mail.getSender(delivery.getMail()).isEmpty()) {
            Mail.setSender(delivery.getMail(), delivery.getSender());
        }

        MailDropOffContext context = new MailDropOffContext(MailService.of(level), recipient, delivery);
        MailDropOffResult result = DROP_OFF_HANDLER.handle(context);

        if (result.isHandled() && !(result instanceof MailDropOffResult.Returned)) {
            if (recipient.equals(delivery.getRecipient())) {
                delivery.getOwner()
                      .map(level::getPlayerByUUID)
                      .ifPresent(player -> {
                          if (player instanceof ServerPlayer serverPlayer) {
                              Envelope.CriteriaTriggers.MAIL_DELIVERED.get().trigger(serverPlayer, delivery);
                              player.awardStat(Envelope.Stats.MAIL_DELIVERIES.get());
                          }
                      });
            }

            ItemStack mail = result.getMail();
            if (result.isReply()) {
                if (delivery.getPhase().isReturning()) {
                    LOGGER.error("Received reply in the returning phase. This must be an error. Delivery: {}", delivery);
                } else {
                    Mail.writeToLog(mail, DeliveryRecord.sentFrom(delivery.getRecipient()));
                }
            }
            delivery.setMail(mail);
        }
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
