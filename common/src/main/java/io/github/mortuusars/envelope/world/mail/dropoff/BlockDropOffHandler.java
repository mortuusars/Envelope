package io.github.mortuusars.envelope.world.mail.dropoff;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.block.mailbox.Inbox;
import io.github.mortuusars.envelope.world.block.mailbox.InboxStorage;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Optional;

public class BlockDropOffHandler implements MailDropOffHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public MailDropOffResult handle(MailDropOffContext context) {
        if (!(context.getTarget() instanceof BlockAddress address)) {
            return MailDropOffResult.PASS;
        }

        ItemStack mail = context.getMail();

        if (mail.isEmpty()) {
            return MailDropOffResult.PASS;
        }

        return getInboxByAddress(context.getService(), address)
              .map(inbox -> {
                  if (inbox.isInboxFull()) {
                      if (context.getDelivery().getPhase().isReturning()) {
                          LOGGER.debug("Cannot deliver mail to mailbox '{}': inbox is full.", address);
                          return MailDropOffResult.PASS;
                      } else {
                          LOGGER.debug("Cannot deliver mail to mailbox '{}': inbox is full. Returning to sender.", address);
                          return MailDropOffResult.returned(mail, DeliveryRecord.Message.RECIPIENT_INBOX_IS_FULL);
                      }
                  }

                  ItemStack deliveredMail = Mail.asDelivered(mail.copyWithCount(1));
                  Mail.writeToLog(deliveredMail, DeliveryRecord.arrivedTo(address));
                  Mail.setId(deliveredMail, Id.create(context.getService().getLevel()));

                  if (inbox.addMail(deliveredMail)) {
                      inbox.onMailInserted(deliveredMail);
                      return MailDropOffResult.CONSUME;
                  } else {
                      if (context.getDelivery().getPhase().isReturning()) {
                          LOGGER.debug("Cannot deliver mail to mailbox '{}': mail cannot be inserted.", address);
                          return MailDropOffResult.PASS;
                      } else {
                          LOGGER.debug("Cannot deliver mail to mailbox '{}': mail cannot be inserted. Returning to sender.", address);
                          return MailDropOffResult.returned(mail, DeliveryRecord.Message.UNABLE_TO_REACH);
                      }
                  }
              })
              .orElseGet(() -> {
                  if (context.getDelivery().getPhase().isReturning()) {
                      LOGGER.debug("Cannot deliver mail to mailbox '{}': address not found.", address);
                      return MailDropOffResult.PASS;
                  } else {
                      LOGGER.debug("Cannot deliver mail to mailbox '{}': address not found. Returning to sender.", address);
                      return MailDropOffResult.returned(mail, DeliveryRecord.Message.RECIPIENT_NOT_FOUND);
                  }
              });
    }

    public static Optional<Inbox> getInboxByAddress(MailService service, BlockAddress address) {
        return service.getMailboxes().getBlockEntityOf(address)
              .map(blockEntity -> ((Inbox) blockEntity))
              .or(() -> InboxStorage.get(service.getLevel()).getForDelivery(address));
    }
}
