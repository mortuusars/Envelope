package io.github.mortuusars.envelope.world.mail.receiver;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.block.mailbox.Inbox;
import io.github.mortuusars.envelope.world.block.mailbox.Inboxes;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.MailService;
import io.github.mortuusars.envelope.world.block.mailbox.Mailboxes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class BlockMailReceiver implements MailReceiver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Address.Block address;

    public BlockMailReceiver(Address.Block address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        Mailboxes mailboxes = MailService.of(level).mailboxes();

        if (mail.isEmpty()) {
            return mail;
        }

        return mailboxes.getBlockEntityOf(address)
              .map(be -> ((Inbox) be))
              .or(() -> Inboxes.get(level).forDelivery(address))
              .map(inbox -> {
                  if (inbox.isFull()) {
                      LOGGER.info("Cannot deliver mail to mailbox '{}': inbox is full. Returning to sender.", address);
                      return returned(mail, DeliveryRecord.Message.RECIPIENT_INBOX_IS_FULL);
                  }

                  ItemStack deliveredMail = Mail.asDelivered(mail.copyWithCount(1));
                  Mail.writeToLog(deliveredMail, DeliveryRecord.arrivedTo(address).at(level.getGameTime()));
                  Mail.setId(deliveredMail, Id.create(level));

                  if (inbox.addMail(deliveredMail)) {
                      if (inbox instanceof MailboxBlockEntity be) {
                          level.playSound(null, be.getBlockPos(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 1, 1);
                      }
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
}
