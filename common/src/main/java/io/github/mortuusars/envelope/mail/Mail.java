package io.github.mortuusars.envelope.mail;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.world.item.MailItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.NotImplementedException;

public class Mail {
    public static void addSendData(ServerLevel level, ItemStack mail) {
        if (mail.getItem() instanceof MailItem mailItem) {
            mailItem.updateRecipientBeforeNewSendIfNeeded(mail);
        }

        validateMail(mail);

        mail.remove(Envelope.DataComponents.MAIL_TRAVELING_LOG);

        Address sender = mail.get(Envelope.DataComponents.MAIL_SENDER);
        Address recipient = mail.get(Envelope.DataComponents.MAIL_RECIPIENT);

        if (recipient instanceof Address.Player) {
            throw new NotImplementedException("Sending to players is not implemented yet.");
        }

        long currentGameTime = level.getGameTime();

        MailTravelingLog.addRecords(mail,
                TravelingRecord.sentFrom(sender).atTime(currentGameTime),
                TravelingRecord.travelingTo(recipient));
    }

    public static void addReturnData(ServerLevel level, ItemStack mail) {
        validateMail(mail);

        Address sender = mail.get(Envelope.DataComponents.MAIL_SENDER);
        Address recipient = mail.get(Envelope.DataComponents.MAIL_RECIPIENT);

        long currentGameTime = level.getGameTime();

        MailTravelingLog.addRecords(mail,
                TravelingRecord.returned(recipient).atTime(currentGameTime),
                TravelingRecord.travelingTo(sender));
    }

    private static void validateMail(ItemStack mail) {
        Preconditions.checkArgument(mail.has(Envelope.DataComponents.MAIL_SENDER) && mail.has(Envelope.DataComponents.MAIL_RECIPIENT),
                "Mail must have 'envelope:mail_sender' and 'envelope:mail_recipient' defined. " + mail);
    }
}
