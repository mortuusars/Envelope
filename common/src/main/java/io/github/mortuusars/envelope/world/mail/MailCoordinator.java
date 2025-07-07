package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.Mail;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class MailCoordinator extends SavedData {
    protected final TravelingMailStorage travelingMail = new TravelingMailStorage(this::onFinishTraveling);
    protected final MailboxStorage mailboxes = new MailboxStorage();

    // --

    public TravelingMailStorage getTravelingMail() {
        return travelingMail;
    }

    public MailboxStorage getMailboxes() {
        return mailboxes;
    }

    // --

    public void send(Mail mail) {
        getTravelingMail().add(mail);
        setDirty();
    }

    // --

    public void tick(MinecraftServer server) {
        getTravelingMail().tick(server);
        setDirty();
    }

    // --

    protected void onFinishTraveling(MinecraftServer server, Mail mail) {
        if (getMailboxes().exists(mail.recipient().name())) {
            getMailboxes().deliver(mail.recipient().name(), mail);
        }

//        boolean delivered = switch (mail.recipient().type()) {
//            case PLAYER -> tryDeliverToPlayer(server, mail);
//            case NPC -> tryDeliverToNPC(server, mail);
//            case UNKNOWN -> {
//                if (tryDeliverToNPC(server, mail)) { // Try to NPC first
//                    yield true;
//                }
//                yield tryDeliverToPlayer(server, mail); // Then to player if not handled
//            }
//        };
//
//        if (!delivered) {
//            returnToSender(server, mail);
//            Envelope.LOGGER.error("Returning '{}' back to the sender.", mail);
//        } else {
//            Envelope.LOGGER.info("Successfully delivered '{}'", mail);
//        }
//
//        setDirty();
    }

    protected boolean tryDeliverToNPC(MinecraftServer server, Mail mail) {
        if (mail.status() == Mail.Status.REJECTED || mail.status() == Mail.Status.RETURNED) {
            // Void the mail to not send it back and forth.
            return true;
        }

        //TODO: implement NPC consumers
        return false;
    }

//    protected boolean tryDeliverToPlayer(MinecraftServer server, Mail mail) {
//        @Nullable UUID playerUuid = tryGetPlayerUuid(server, mail.recipient());
//
//        if (playerUuid == null) {
//            Envelope.LOGGER.error("Cannot deliver mail: unknown recipient '{}'.", mail.recipient());
//            return false;
//        }
//
//        deliveredMail.deliver(playerUuid, mail);
//        @Nullable ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
//        if (player != null) {
//            if (player.containerMenu instanceof MailboxMenu) {
//                Packets.sendToClient(MailboxHasNewMailS2CP.INSTANCE, player);
//                player.level().playSound(null, player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 0.75f, 1f);
//            }
//        }
//
//        return true;
//    }

//    protected @Nullable UUID tryGetPlayerUuid(MinecraftServer server, Address recipient) {
//        if (tryFindOnlineRecipient(server, recipient) instanceof ServerPlayer player) {
//            return player.getUUID();
//        }
//        return KnownPlayers.get(server).byAddress(recipient);
//    }
//
//    protected @Nullable ServerPlayer tryFindOnlineRecipient(MinecraftServer server, Address recipient) {
//        if (recipient instanceof Address.Player playerAddress) {
//            @Nullable ServerPlayer player = server.getPlayerList().getPlayer(playerAddress.uuid());
//            if (player != null) {
//                return player;
//            }
//        }
//        return server.getPlayerList().getPlayerByName(recipient.id());
//    }

    protected void returnToSender(MinecraftServer server, Mail mail) {
        send(new Mail(Address.MAIL_SERVICE,
                      mail.sender(),
                      mail.content(),
                      server.overworld().getGameTime(),
                      mail.travelTime(), // Same time to return
                      Mail.Status.RETURNED));
    }

    // --

    public static MailCoordinator get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), "envelope_mail");
    }

    // --

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("traveling", travelingMail.save(new CompoundTag(), registries));
        tag.put("delivered", mailboxes.save(new CompoundTag(), registries));
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        travelingMail.load(tag.getCompound("traveling"), registries);
        mailboxes.load(tag.getCompound("delivered"), registries);
    }

    private static Factory<MailCoordinator> factory() {
        return new Factory<>(MailCoordinator::new,
                (tag, provider) -> {
                    MailCoordinator instance = new MailCoordinator();
                    instance.load(tag, provider);
                    return instance;
                }, null);
    }
}
