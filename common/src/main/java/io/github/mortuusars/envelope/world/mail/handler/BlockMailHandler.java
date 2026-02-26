package io.github.mortuusars.envelope.world.mail.handler;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.block.mailbox.Inbox;
import io.github.mortuusars.envelope.world.block.mailbox.InboxStorage;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;

public class BlockMailHandler implements MailHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final BlockAddress address;

    public BlockMailHandler(BlockAddress address) {
        this.address = address;
    }

    @Override
    public MailHandlingResult handle(MailService service, Delivery delivery) {
        ItemStack mail = delivery.getMail();

        if (mail.isEmpty()) {
            return MailHandlingResult.CONSUME;
        }

        return getInboxByAddress(service, address)
              .map(inbox -> {
                  if (inbox.isInboxFull()) {
                      LOGGER.info("Cannot deliver mail to mailbox '{}': inbox is full. Returning to sender.", address);
                      return  MailHandlingResult.returned(mail, DeliveryRecord.Message.RECIPIENT_INBOX_IS_FULL);
                  }

                  ItemStack deliveredMail = Mail.asDelivered(mail.copyWithCount(1));
                  Mail.writeToLog(deliveredMail, DeliveryRecord.arrivedTo(address));
                  Mail.setId(deliveredMail, Id.create(service.getLevel()));

                  if (inbox.addMail(deliveredMail)) {
                      inbox.onMailInserted(deliveredMail);
                      return MailHandlingResult.CONSUME;
                  } else {
                      LOGGER.info("Cannot deliver mail to mailbox '{}': mail cannot be inserted. Returning to sender.", address);
                      return MailHandlingResult.returned(mail, DeliveryRecord.Message.UNABLE_TO_REACH);
                  }
              })
              .orElseGet(() -> {
                  LOGGER.info("Cannot deliver mail to mailbox '{}': address not found. Returning to sender.", address);
                  return MailHandlingResult.returned(mail, DeliveryRecord.Message.RECIPIENT_NOT_FOUND);
              });
    }

    public static Optional<Inbox> getInboxByAddress(MailService service, BlockAddress address) {
        return service.getMailboxes().getBlockEntityOf(address)
              .map(blockEntity -> ((Inbox) blockEntity))
              .or(() -> InboxStorage.get(service.getLevel()).getForDelivery(address));
    }
}