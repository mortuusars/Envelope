package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Mail;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.Nullable;

public class MailCoordinator {
    protected final TravelingMail travelingMail = new TravelingMail(this::deliver);
    protected final DeliveredMail deliveredMail = new DeliveredMail();

    public void send(Mail mail) {
        travelingMail.add(mail);
    }

    // --

    public void tick(MinecraftServer server) {
        travelingMail.tick(server);
    }

    // --

    protected void deliver(MinecraftServer server, Mail mail) {
        String recipient = mail.recipient();

        boolean delivered;

        if (recipient.startsWith("@")) {
            delivered = tryDeliverToPlayer(server, mail);
        } else {
            delivered = tryDeliverToNPC(server, mail);
            if (!delivered) {
                delivered = tryDeliverToPlayer(server, mail);
            }
        }

        if (!delivered) {
            returnToSender(server, mail);
            Envelope.LOGGER.error("Returning '{}' back to the sender.", mail);
        } else {
            Envelope.LOGGER.info("Successfully delivered '{}'", mail);
        }
    }

    protected boolean tryDeliverToNPC(MinecraftServer server, Mail mail) {
        if (mail.status() == Mail.Status.REJECTED || mail.status() == Mail.Status.RETURNED) {
            // Void the mail to not send it back and forth.
            return true;
        }

        //TODO: implement NPC consumers
        return false;
    }

    protected boolean tryDeliverToPlayer(MinecraftServer server, Mail mail) {
        String recipient = mail.recipient().replace("@", "");

        if (server.getPlayerList().getPlayerByName(recipient) instanceof ServerPlayer player) {
            return deliveredMail.deliver(player.getUUID(), mail);
        }

        @Nullable GameProfileCache profileCache = server.getProfileCache();
        if (profileCache == null) {
            Envelope.LOGGER.error("Cannot deliver mail to player '{}'. ProfileCache is not available.", recipient);
            return false;
        }

        return profileCache.get(recipient)
                .map(profile -> deliveredMail.deliver(profile.getId(), mail))
                .orElse(false);
    }

    protected void returnToSender(MinecraftServer server, Mail mail) {
        send(new Mail(mail.sender(),
                mail.sender().name(),
                mail.content(),
                server.overworld().getGameTime(),
                mail.travelDuration(), // Same time to return
                Mail.Status.RETURNED));
    }
}
