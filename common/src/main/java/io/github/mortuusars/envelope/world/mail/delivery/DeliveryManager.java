package io.github.mortuusars.envelope.world.mail.delivery;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.*;
import org.slf4j.Logger;

import java.util.function.Function;

public class DeliveryManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MailService mailService;

    public DeliveryManager(MailService mailService) {
        this.mailService = mailService;
    }

    public MailService getMailService() {
        return mailService;
    }

    // --

    public void start(Pigeon pigeon, DeliveryDraft draft) {
        start(draft, pigeon::startDelivery);
    }

    public void startService(DeliveryDraft draft) {
        start(draft, delivery -> Pigeon.spawnServiceCourier(getMailService().getLevel(), delivery));
    }

    public void start(DeliveryDraft draft, Function<Delivery, Courier> courier) {
        Delivery delivery = createDelivery(draft);
        courier.apply(delivery);
        LOGGER.debug("Started delivery: {}", delivery);
    }

    protected Delivery createDelivery(DeliveryDraft draft) {
        return new Delivery(draft.getOrCreateId(getMailService().getLevel()),
              draft.getOwner(),
              draft.getSender(),
              draft.getRecipient(),
              draft.getMail(),
              DeliveryRoute.build(getMailService().getLevel(), draft.getSender(), draft.getRecipient()),
              draft.getPhase(),
              0,
              false);
    }

    // --

    public boolean canDeliverTo(Address address) {
        address = address.resolve(getMailService());
        return address instanceof BlockAddress || address instanceof ServiceAddress;
    }
}
