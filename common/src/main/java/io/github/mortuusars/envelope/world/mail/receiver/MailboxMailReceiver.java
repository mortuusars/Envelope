package io.github.mortuusars.envelope.world.mail.receiver;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.block.mailbox.Inbox;
import io.github.mortuusars.envelope.world.block.mailbox.InboxStorage;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;

public class MailboxMailReceiver implements MailReceiver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Address.Block address;

    public MailboxMailReceiver(Address.Block address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        if (mail.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return getInboxByAddress(level, address)
              .map(inbox -> {
                  if (inbox.isInboxFull()) {
                      LOGGER.info("Cannot deliver mail to mailbox '{}': inbox is full. Returning to sender.", address);
                      return returned(mail, DeliveryRecord.Message.RECIPIENT_INBOX_IS_FULL);
                  }

                  ItemStack deliveredMail = Mail.asDelivered(mail.copyWithCount(1));
                  Mail.writeToLog(deliveredMail, DeliveryRecord.arrivedTo(address, level.getGameTime()));
                  Mail.setId(deliveredMail, Id.create(level));

                  if (inbox.addMail(deliveredMail)) {
                      inbox.onMailInserted(deliveredMail);
                      return ItemStack.EMPTY;
                  } else {
                      LOGGER.info("Cannot deliver mail to mailbox '{}': mail cannot be inserted. Returning to sender.", address);
                      return returned(mail, DeliveryRecord.Message.UNABLE_TO_REACH);
                  }
              })
              .orElseGet(() -> {
                  LOGGER.info("Cannot deliver mail to mailbox '{}': address not found. Returning to sender.", address);
                  return returned(mail, DeliveryRecord.Message.RECIPIENT_NOT_FOUND);
              });
    }

    public static Optional<Inbox> getInboxByAddress(ServerLevel level, Address.Block address) {
        return MailService.of(level).mailboxes().getBlockEntityOf(address)
              .map(blockEntity -> ((Inbox) blockEntity))
              .or(() -> InboxStorage.get(level).getForDelivery(address));
    }
}