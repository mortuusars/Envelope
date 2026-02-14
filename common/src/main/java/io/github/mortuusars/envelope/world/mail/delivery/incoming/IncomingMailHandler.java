package io.github.mortuusars.envelope.world.mail.delivery.incoming;

import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface IncomingMailHandler {
    /**
     * Processes incoming or returning mail.
     * @return Item that will continue on delivery path.<br>
     * - Empty when consumed.
     * - Same item (with or without extra data) when returned due to various reasons.
     * - Another item when result is produced.
     */
    ItemStack handle(ServerLevel level, Delivery delivery);

    IncomingMailHandler RETURN_NOT_FOUND = (level, delivery) ->
          Mail.returned(delivery.getMail(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND);

    IncomingMailHandler RETURN_CANNOT_BE_DETERMINED = (level, delivery) ->
          Mail.returned(delivery.getMail(), DeliveryRecord.Message.RECIPIENT_CANNOT_BE_DETERMINED);
}