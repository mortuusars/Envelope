package io.github.mortuusars.envelope.util.bugger;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.data.EntityData;
import io.github.mortuusars.envelope.util.bugger.data.OptionalEntityData;
import io.github.mortuusars.envelope.util.bugger.data.NbtData;
import io.github.mortuusars.envelope.util.bugger_data.MailServiceBuggerData;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Utility to make in-game debugging easier.
 */
public class Bugger {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Supplier<Boolean> enabler = () -> false;

    public static boolean isEnabled() {
        return enabler.get();
    }

    // --

    public static final MailServiceBuggerData MAIL_SERVICE = new MailServiceBuggerData();

    public static final EntityData<PigeonholeHandler> PIGEON_PIGEONHOLE_HANDLER =
          new EntityData<>(Envelope.resource("pigeon_pigeonhole_handler"), PigeonholeHandler.CODEC)
                .handle(((entity, handler) -> {
                    if (entity instanceof Pigeon pigeon) {
                        pigeon.setPigeonholeHandler(handler);
                    }
                }));

    public static final EntityData<MailboxHandler> PIGEON_MAILBOX_HANDLER =
          new EntityData<>(Envelope.resource("pigeon_mailbox_handler"), MailboxHandler.CODEC)
                .handle(((entity, handler) -> {
                    if (entity instanceof Pigeon pigeon) {
                        pigeon.setMailboxHandler(handler);
                    }
                }));

    public static final OptionalEntityData<Delivery> PIGEON_DELIVERY =
          new OptionalEntityData<>(Envelope.resource("pigeon_delivery"), Delivery.CODEC)
                .handle((entity, delivery) -> {
                    if (entity instanceof Pigeon pigeon) {
                        pigeon.setDelivery(delivery.orElse(null));
                    }
                });
}