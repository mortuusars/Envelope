package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.item.LetterItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class VillagerMailEntity extends MailEntity {
    public VillagerMailEntity(Address.Entity address, int distance) {
        super(address, distance);
    }

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        if (!(mail.getItemForReading().getItem() instanceof LetterItem)) {
            return mail.writeToLog(log -> log.append(DeliveryRecord.returnedFrom(getAddress())));
        }

        @Nullable Address sender = mail.get(Envelope.DataComponents.MAIL_SENDER);
        if (sender != null) {
            ItemStack letter = new ItemStack(Envelope.Items.LETTER.get());
            letter.set(Envelope.DataComponents.LETTER_SUBJECT, "uhhhhh");
            letter.set(Envelope.DataComponents.LETTER_MESSAGE, "i got your letter. it was nice. thanks.");
            letter.set(Envelope.DataComponents.MAIL_SENDER, getAddress());
            letter.set(Envelope.DataComponents.MAIL_RECIPIENT, sender);

            return new Mail(letter).writeToLog(log ->
                  log.append(DeliveryRecord.sentFrom(getAddress()).atTime(level.getGameTime())));
        }

        return Mail.EMPTY;
    }
}
