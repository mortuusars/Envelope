package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.api.mail.Mail;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;

public class TravelingMail {
    protected final DeliveryHandler deliver;
    protected final Set<Mail> mail = new HashSet<>();

    public TravelingMail(DeliveryHandler deliver) {
        this.deliver = deliver;
    }

    public void add(Mail mail) {
        this.mail.add(mail);
    }

    public void tick(MinecraftServer server) {
        mail.removeIf(mail -> {
            if (mail.sentAt() + mail.travelDuration() <= server.overworld().getGameTime()) {
                deliver.deliver(server, mail);
                return true;
            }
            return false;
        });
    }

    public interface DeliveryHandler {
        void deliver(MinecraftServer server, Mail mail);
    }
}
