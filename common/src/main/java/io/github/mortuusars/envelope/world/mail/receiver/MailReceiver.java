package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.mail.Mail;
import net.minecraft.server.level.ServerLevel;

public interface MailReceiver {
    Mail receiveMail(ServerLevel level, Mail mail);
}
