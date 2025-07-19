package io.github.mortuusars.envelope.api.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.MailItem;
import io.github.mortuusars.envelope.world.mail.MailCoordinator;
import io.github.mortuusars.envelope.world.mail.Mailboxes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Mail {
    public static Mailboxes getMailboxes() {
        return MailCoordinator.INSTANCE.getMailboxes();
    }

    // --

    public static boolean send(ItemStack mail, @Nullable Player senderPlayer) {
        if (mail.getItem() instanceof MailItem mailItem) {
            mailItem.updateRecipientBeforeNewSendIfNeeded(mail);
        }

        mail.remove(Envelope.DataComponents.MAIL_TRAVELING_LOG);
        return MailCoordinator.INSTANCE.send(mail, senderPlayer);
    }

    public static boolean send(ItemStack mail) {
        return send(mail, null);
    }
}
