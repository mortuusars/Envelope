package io.github.mortuusars.envelope.world.mail;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class MailCoordinator {
    public static final MailCoordinator INSTANCE = new MailCoordinator();
    private MinecraftServer server;

    private MailCoordinator() {
    }

    // --

    public void init(MinecraftServer server) {
        this.server = server;
        TravelingMail.get(server).onFinishedTraveling = this::finishTraveling;
    }

    public void tick(MinecraftServer server) {
        TravelingMail.get(server).tick(server);
    }

    // --

    public boolean send(ItemStack mail, @Nullable Player player) {
        validateMail(mail);

        Address sender = mail.get(Envelope.DataComponents.MAIL_SENDER);
        Address recipient = mail.get(Envelope.DataComponents.MAIL_RECIPIENT);
        int travelDuration = Math.max(1, mail.getOrDefault(Envelope.DataComponents.MAIL_TRAVEL_DURATION,
                getTravelDuration(mail, sender, recipient)));

        if (recipient instanceof Address.Player) {
            throw new NotImplementedException("Sending to players is not implemented yet.");
        }

        if (!mail.has(Envelope.DataComponents.MAIL_ID)) {
//            mail.set(Envelope.DataComponents.MAIL_ID, UUID.randomUUID());
        }

        long currentGameTime = getCurrentGameTime();

        mail.set(Envelope.DataComponents.MAIL_TRAVEL_DURATION, travelDuration);
        mail.set(Envelope.DataComponents.MAIL_SENT_AT, currentGameTime);

        MailTravelingLog.addRecords(mail,
                TravelingRecord.sentFrom(sender, currentGameTime, Optional.ofNullable(player).map(Player::getName)),
                TravelingRecord.travelingTo(recipient, currentGameTime, travelDuration));

        return TravelingMail.get(server).startTraveling(mail);
    }

    protected void finishTraveling(ItemStack mail) {
//        validateMail(mail);
//
//        if (!mail.has(Envelope.DataComponents.MAIL_ID)) {
//            mail.set(Envelope.DataComponents.MAIL_ID, UUID.randomUUID());
//        }
//
//        Address recipient = Objects.requireNonNull(mail.get(Envelope.DataComponents.MAIL_RECIPIENT));

//        Mailboxes mailboxes = Mailboxes.get(server);

//        if (mailboxes.exists(recipient)) {
//            MailTravelingLog.addRecords(mail, TravelingRecord.arrivedTo(recipient, getCurrentGameTime()));
//            mailboxes.putMail(recipient, mail);
//            onMailReceived(recipient, mail);
//        } else {
//            Envelope.LOGGER.error("Cannot receive mail: address {} is not known. {}", recipient, mail);
//            returnToSender(mail, TravelingRecord.Status.RETURNED, Optional.ofNullable(Address.MAIL_SERVICE.getDisplayName()));
//        }
    }

    protected void returnToSender(ItemStack mail, TravelingRecord.Status status, Optional<Component> operator) {
        if (MailTravelingLog.of(mail).records().stream().anyMatch(s -> s.status() == TravelingRecord.Status.RETURNED)) {
            Envelope.LOGGER.error("Mail {} would not be returned back to sender because it has been returned already. Deleting.", mail);
            return;
        }

        validateMail(mail);

        mail.set(Envelope.DataComponents.MAIL_RECIPIENT, Objects.requireNonNull(mail.get(Envelope.DataComponents.MAIL_SENDER)));
        mail.set(Envelope.DataComponents.MAIL_SENDER, Address.MAIL_SERVICE);
//        MailTravelingLog.addRecords(mail, new TravelingRecord(status, Address.MAIL_SERVICE, getCurrentGameTime(), 0, operator));

        Envelope.LOGGER.info("Returning mail back to sender: {}", mail);

        send(mail, null);
    }

    // --

//    protected void onMailReceived(Address recipient, ItemStack mail) {
//        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
//            if (player.containerMenu instanceof MailboxMenu menu && recipient.equals(menu.getBlockEntity().getAddress())) {
//                Packets.sendToClient(MailboxHasNewMailS2CP.INSTANCE, player);
//            }
//        }
//    }

    // --

    public long getCurrentGameTime() {
        return server.overworld().getGameTime();
    }

    public int getTravelDuration(ItemStack mail, Address from, Address to) {
        //TODO: distance influences duration?
        return Config.Server.TRAVEL_DURATION.get();
    }

    private void validateMail(ItemStack mail) {
        Preconditions.checkArgument(mail.has(Envelope.DataComponents.MAIL_SENDER) && mail.has(Envelope.DataComponents.MAIL_RECIPIENT),
                "Mail must have 'envelope:mail_sender' and 'envelope:mail_recipient' defined. " + mail);
    }
}
