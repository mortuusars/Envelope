package io.github.mortuusars.envelope.world.service;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.util.result.Error;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.address.Address;
import org.slf4j.Logger;

import java.util.function.Function;

public class DeliveryManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Error ERROR_SAME_ADDRESSES = new Error(
          "Recipient address cannot be the same as sender address.",
          "error.envelope.delivery.same_addresses");
    private static final Error ERROR_RECIPIENT_UNKNOWN = new Error(
          "Cannot deliver to unknown address.",
          "error.envelope.delivery.unknown_address");
    private static final Error ERROR_NO_MAIL = new Error(
          "Mail is empty.",
          "error.envelope.delivery.no_mail");

    private final MailService mailService;

    public DeliveryManager(MailService mailService) {
        this.mailService = mailService;
    }

    public MailService getMailService() {
        return mailService;
    }

    // --

    public Result<StartedDelivery> start(Pigeon pigeon, Delivery delivery) {
        return tryStart(delivery, pigeon::startDelivery);
    }

    public Result<StartedDelivery> start(Pigeon pigeon, Delivery.Builder deliveryBuilder) {
        return start(pigeon, deliveryBuilder.create());
    }

    public Result<StartedDelivery> startService(Delivery delivery) {
        return tryStart(delivery, validDelivery -> Pigeon.spawnServiceCourier(getMailService().getLevel(), validDelivery));
    }

    public Result<StartedDelivery> startService(Delivery.Builder deliveryBuilder) {
        return startService(deliveryBuilder.create());
    }

    protected Result<StartedDelivery> tryStart(Delivery delivery, Function<Delivery, Courier> courier) {
//        if (delivery.getSender().matches(Address.UNKNOWN) && delivery.getRecipient().matches(Address.UNKNOWN)) {
//            LOGGER.error("Cannot start delivery: {}. Delivery: {}", ERROR_RECIPIENT_UNKNOWN.getMessage(), delivery);
//            return Result.error(ERROR_RECIPIENT_UNKNOWN);
//        }
//        if (delivery.getSender().matches(delivery.getRecipient())) {
//            LOGGER.error("Cannot start delivery: {}. Delivery: {}", ERROR_SAME_ADDRESSES.getMessage(), delivery);
//            return Result.error(ERROR_SAME_ADDRESSES);
//        }
        if (delivery.getPhase() == DeliveryPhase.STARTED && delivery.getMail().isEmpty()) {
            LOGGER.error("Cannot start delivery: {}. Delivery: {}", ERROR_NO_MAIL.getMessage(), delivery);
            return Result.error(ERROR_NO_MAIL);
        }

        delivery.updateMetadata(data -> data.withTimestampIfMissing(getMailService().getGameTime()));

        LOGGER.debug("Starting delivery {}", delivery);

        return Result.success(new StartedDelivery(courier.apply(delivery), delivery));
    }

    public record StartedDelivery(Courier courier, Delivery delivery) {
    }

    // --

    public boolean canDeliverTo(Address address) {
        if (address.matches(Address.UNKNOWN)) {
            return false;
        }

        Address resolvedAddress = getMailService().resolve(address);

        if (resolvedAddress instanceof Address.Player) {
            // Player doesn't have default address
            return false;
        }

        return getMailService().getKnownAddresses().isKnown(resolvedAddress);
    }

    public void dispatch(Delivery delivery) {
        if (!canDeliverTo(delivery.getRecipient())) {
            delivery.getMail().writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
            delivery.setPhaseAndResetProgress(DeliveryPhase.RETURNING_TO_SENDER);
            return;
        }

        if (getMailService().getPaybackDepartment().tryHandle(delivery)) {
            return;
        }

        delivery.setPhaseAndResetProgress(delivery.getPhase().next()); // Continue normally
    }
}
