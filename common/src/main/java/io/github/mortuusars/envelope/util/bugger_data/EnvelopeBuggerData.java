package io.github.mortuusars.envelope.util.bugger_data;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.mortaar.bugger.data.EntityData;
import io.github.mortuusars.mortaar.bugger.data.OptionalEntityData;

public class EnvelopeBuggerData {
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
