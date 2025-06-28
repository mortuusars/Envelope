package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.Mail;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.MailboxHasNewMailS2CP;
import io.github.mortuusars.envelope.world.KnownPlayers;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MailCoordinator extends SavedData {
    protected final TravelingMailStorage travelingMail = new TravelingMailStorage(this::onFinishTraveling);
    protected final DeliveredMailStorage deliveredMail = new DeliveredMailStorage();

    // --

    public TravelingMailStorage getTravelingMail() {
        return travelingMail;
    }

    public DeliveredMailStorage getDeliveredMail() {
        return deliveredMail;
    }

    // --

    public void send(Mail mail) {
        travelingMail.add(mail);
        setDirty();
    }

    // --

    public void tick(MinecraftServer server) {
        travelingMail.tick(server);
        setDirty();
    }

    // --

    protected void onFinishTraveling(MinecraftServer server, Mail mail) {
        boolean delivered = switch (mail.recipient().type()) {
            case PLAYER -> tryDeliverToPlayer(server, mail);
            case NPC -> tryDeliverToNPC(server, mail);
            case UNKNOWN -> {
                if (tryDeliverToNPC(server, mail)) { // Try to NPC first
                    yield true;
                }
                yield tryDeliverToPlayer(server, mail); // Then to player if not handled
            }
        };

        if (!delivered) {
            returnToSender(server, mail);
            Envelope.LOGGER.error("Returning '{}' back to the sender.", mail);
        } else {
            Envelope.LOGGER.info("Successfully delivered '{}'", mail);
        }

        setDirty();
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
        @Nullable UUID playerUuid = tryGetPlayerUuid(server, mail.recipient());

        if (playerUuid == null) {
            Envelope.LOGGER.error("Cannot deliver mail: unknown recipient '{}'.", mail.recipient());
            return false;
        }

        deliveredMail.deliver(playerUuid, mail);
        @Nullable ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            if (player.containerMenu instanceof MailboxMenu) {
                Packets.sendToClient(MailboxHasNewMailS2CP.INSTANCE, player);
                player.level().playSound(null, player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 0.75f, 1f);
            }
        }

        return true;
    }

    protected @Nullable UUID tryGetPlayerUuid(MinecraftServer server, Address recipient) {
        if (tryFindOnlineRecipient(server, recipient) instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return KnownPlayers.get(server).byAddress(recipient);
    }

    protected @Nullable ServerPlayer tryFindOnlineRecipient(MinecraftServer server, Address recipient) {
        if (recipient instanceof Address.Player playerAddress) {
            @Nullable ServerPlayer player = server.getPlayerList().getPlayer(playerAddress.uuid());
            if (player != null) {
                return player;
            }
        }
        return server.getPlayerList().getPlayerByName(recipient.id());
    }

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
        tag.put("delivered", deliveredMail.save(new CompoundTag(), registries));
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        travelingMail.load(tag.getCompound("traveling"), registries);
        deliveredMail.load(tag.getCompound("delivered"), registries);
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
