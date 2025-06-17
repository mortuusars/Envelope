package io.github.mortuusars.envelope.api.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.world.mail.MailCoordinator;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public class Mailbox {
    public static void send(Mail mail) {
        @Nullable MinecraftServer server = PlatformHelper.getCurrentServer();
        if (server != null) {
            MailCoordinator.get(server).send(mail);
        } else {
            Envelope.LOGGER.error("Cannot send mail: server is not available.");
        }
    }
}
