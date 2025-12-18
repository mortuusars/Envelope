package io.github.mortuusars.envelope.world.service;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.util.result.Error;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.address.Address;
import org.slf4j.Logger;

import java.util.function.Function;

public class DeliveryManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Result<StartedDelivery> ERROR_SAME_ADDRESSES = Result.error(new Error(
          "Recipient address cannot be the same as sender address.",
          "error.envelope.delivery.same_addresses"));
    private static final Result<StartedDelivery> ERROR_RECIPIENT_UNKNOWN = Result.error(new Error(
          "Cannot deliver to unknown address.",
          "error.envelope.delivery.unknown_address"));
    private static final Result<StartedDelivery> ERROR_NO_MAIL = Result.error(new Error(
          "Mail is empty.",
          "error.envelope.delivery.no_mail"));

    private final MailService context;

    public DeliveryManager(MailService context) {
        this.context = context;
    }

    public Result<StartedDelivery> start(Delivery delivery, Pigeon pigeon) {
        return start(delivery, pigeon::startDelivery)
              .ifError(e -> LOGGER.error(e.getMessage()));
    }

    public Result<StartedDelivery> start(Delivery.Builder deliveryBuilder, Pigeon pigeon) {
        return start(deliveryBuilder.create(context.getLevel()), pigeon);
    }

    public Result<StartedDelivery> startService(Delivery delivery) {
        return start(delivery, validDelivery -> Pigeon.spawnServiceCourier(context.getLevel(), validDelivery))
              .ifError(e -> LOGGER.error(e.getMessage()));
    }

    public Result<StartedDelivery> startService(Delivery.Builder deliveryBuilder) {
        return startService(deliveryBuilder.create(context.getLevel()));
    }

    protected Result<StartedDelivery> start(Delivery delivery, Function<Delivery, Courier> courier) {
        if (delivery.getRecipient().matches(Address.UNKNOWN)) return ERROR_RECIPIENT_UNKNOWN;
        if (delivery.getSender().matches(delivery.getRecipient())) return ERROR_SAME_ADDRESSES;
        if (delivery.getCurrentPhase() == DeliveryPhase.STARTED && delivery.getMail().isEmpty()) return ERROR_NO_MAIL;
        return Result.success(new StartedDelivery(courier.apply(delivery), delivery));
    }

    public record StartedDelivery(Courier courier, Delivery delivery) {}
}
