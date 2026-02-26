package io.github.mortuusars.envelope.world.mail;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.util.result.Error;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.mail.delivery.Courier;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryDraft;
import io.github.mortuusars.envelope.world.mail.delivery.DeliveryRoute;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.*;
import io.github.mortuusars.envelope.world.mail.handler.BlockMailHandler;
import io.github.mortuusars.envelope.world.mail.handler.*;
import org.slf4j.Logger;

import java.util.function.Function;

public class DeliveryManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Error ERROR_NO_MAIL = new Error(
          "Mail is empty.",
          "error.envelope.delivery.no_mail");
    private static final Error ERROR_SAME_ADDRESSES = new Error(
          "Recipient address cannot be the same as sender address.",
          "error.envelope.delivery.same_addresses");
    private static final Error ERROR_RECIPIENT_UNKNOWN = new Error(
          "Cannot deliver to unknown address.",
          "error.envelope.delivery.unknown_address");

    private final MailService mailService;

    public DeliveryManager(MailService mailService) {
        this.mailService = mailService;
    }

    public MailService getMailService() {
        return mailService;
    }

    // --

    public Result<StartedDelivery> start(Pigeon pigeon, DeliveryDraft draft) {
        return tryStart(draft, pigeon::startDelivery);
    }

    public Result<StartedDelivery> startService(DeliveryDraft draft) {
        return tryStart(draft, delivery -> Pigeon.spawnServiceCourier(getMailService().getLevel(), delivery));
    }

    protected Result<StartedDelivery> tryStart(DeliveryDraft draft, Function<Delivery, Courier> courier) {
        if (draft.getMail().isEmpty()) {
            LOGGER.error("Cannot start delivery: {}. Delivery: {}", ERROR_NO_MAIL.getMessage(), draft);
            return Result.error(ERROR_NO_MAIL);
        }

        Delivery delivery = new Delivery(
              draft.getOrCreateId(getMailService().getLevel()),
              draft.getOwner(),
              draft.getSender(),
              draft.getRecipient(),
              draft.getMail(),
              DeliveryRoute.build(getMailService().getLevel(), draft.getSender(), draft.getRecipient()),
              draft.getPhase(),
              0,
              false
        );

        LOGGER.debug("Starting delivery: {}", delivery);
        return Result.success(new StartedDelivery(courier.apply(delivery), delivery));
    }

    public record StartedDelivery(Courier courier, Delivery delivery) {
    }

    // --

    public boolean canDeliverTo(Address address) {
        return switch (address.resolve(getMailService())) {
            case BlockAddress block -> true;
            case EntityAddress entity -> true;
            default -> false;
        };
    }

    public MailHandler getMailHandler(Address address) {
        return switch (address) {
            case BlockAddress block -> new BlockMailHandler(block);
            case PlayerAddress player -> new PlayerMailHandler(player);
            case EntityAddress entity -> new BaseEntityMailHandler(entity);
            case CustomAddress ignored -> MailHandler.RETURN_NOT_FOUND;
            case UnknownAddress ignored -> MailHandler.RETURN_RECIPIENT_CANNOT_BE_DETERMINED;
            default -> throw new IllegalStateException("Unexpected type of address: " + address.getType());
        };
    }
}
