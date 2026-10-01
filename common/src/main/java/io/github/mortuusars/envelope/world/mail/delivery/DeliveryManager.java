package io.github.mortuusars.envelope.world.mail.delivery;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.entity.CourierBat;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.*;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public void start(PhysicalCourier courier, DeliveryDraft draft) {
        Delivery delivery = createDelivery(draft, courier);
        courier.startDelivery(delivery);
        LOGGER.debug("Started delivery: {}", delivery);
    }

    public void startService(DeliveryDraft draft) {
        ServerLevel level = getMailService().getLevel();

        PhysicalCourier courier = level.isNight()
              ? CourierBat.createService(level)
              : Pigeon.createService(level);
        start(courier, draft);
        courier.transitionToBackground(level);
    }

    protected Delivery createDelivery(DeliveryDraft draft, Courier courier) {
        return new Delivery(draft.getOrCreateId(getMailService().getLevel()),
              draft.getOwner(),
              draft.getSender(),
              draft.getRecipient(),
              draft.getMail(),
              DeliveryRoute.build(getMailService().getLevel(), draft.getSender(), draft.getRecipient(), courier),
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
