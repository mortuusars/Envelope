package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.mail.entity.MailEntities;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.MailServiceEntity;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department.PaybackDepartment;
import io.github.mortuusars.envelope.world.mail.receiver.EntityMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.PigeonholeMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.PlayerMailReceiver;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MailService {
    protected final ServerLevel level;

    protected final PigeonholeManager pigeonholeManager;
    protected final MailEntities mailEntities;
    protected final MailServiceEntity mailServiceEntity;
    protected final PaybackDepartment paybackDepartment;
    protected final DeliveryManager deliveryManager;

    protected @Nullable Players players;
    protected @Nullable BackgroundDelivery backgroundDelivery;

    private MailService(ServerLevel level) {
        Preconditions.checkArgument(level.dimension() == Level.OVERWORLD, "MailService can exist only on overworld level.");
        this.level = level;
        this.pigeonholeManager = new PigeonholeManager(level);
        this.mailEntities = new MailEntities();
        this.mailServiceEntity = new MailServiceEntity(this);
        this.paybackDepartment = new PaybackDepartment(this);
        this.deliveryManager = new DeliveryManager(this);

        this.mailEntities.register(mailServiceEntity);
    }

    /**
     * It's intended to be created only once for a level. Use {@link MailService#of} to get the instance.
     */
    @ApiStatus.Internal
    public static MailService create(ServerLevel level) {
        return new MailService(level);
    }

    public static MailService of(ServerLevel level) {
        return level.getEnvelopeMailService();
    }

    // --

    public ServerLevel getLevel() {
        return level;
    }

    public PigeonholeManager getPigeonholeManager() {
        return pigeonholeManager;
    }

    public MailEntities getMailEntities() {
        return mailEntities;
    }

    public MailServiceEntity getMailService() {
        return mailServiceEntity;
    }

    public PaybackDepartment getPaybackDepartment() {
        return paybackDepartment;
    }

    public @NotNull Players getPlayers() {
        return players == null
              ? players = Players.get(level, "envelope_players")
              : players;
    }

    public @NotNull BackgroundDelivery getBackgroundDelivery() {
        return backgroundDelivery == null
              ? backgroundDelivery = BackgroundDelivery.get(level, "envelope_background_delivery")
              : backgroundDelivery;
    }

    public DeliveryManager getDeliveryManager() {
        return deliveryManager;
    }

    // -- Address

    public AllAddresses getKnownAddresses() {
        return new AllAddresses(
              getPigeonholeManager().getAllAddresses(),
              getPlayers().getDefaultAddresses().keySet(),
              getMailEntities().getAllAddresses()
        );
    }

    public AllAddresses getKnownAddressesOfType(@Nullable Address.Type type) {
        if (type == null) {
            return getKnownAddresses();
        }
        return switch (type) {
            case BLOCK -> AllAddresses.pigeonholes(getPigeonholeManager().getAllAddresses());
            case PLAYER -> AllAddresses.players(getPlayers().getDefaultAddresses().keySet());
            case ENTITY -> AllAddresses.entities(getMailEntities().getAllAddresses());
        };
    }

    public Optional<Address.Block> getPlayerDefaultAddress(Address.Player playerAddress) {
        return Optional.ofNullable(getPlayers().getDefaultAddresses().get(playerAddress));
    }

    /**
     * @return "final" address. Mostly for getting default block address of a player.
     */
    public Address resolve(Address address) {
        if (address instanceof Address.Player playerAddress) {
            return getPlayerDefaultAddress(playerAddress).map(Address.class::cast).orElse(address);
        }
        return address;
    }

    public Optional<BlockPos> getPositionOf(Address address) {
        return address.map(
              block -> getPigeonholeManager().getPositionOf(block),
              player -> getPlayers().getDefaultAddressOf(player).flatMap(this::getPositionOf),
              entity -> Optional.empty());
    }

    public Optional<Integer> getDistanceBetween(Address first, Address second) {
        //TODO: this is quite verbose for simply getting distance involving entity address
        if (first instanceof Address.Entity firstEntity && second instanceof Address.Entity secondEntity) {
            Optional<Integer> firstDistance = getMailEntities().byAddress(firstEntity).map(MailEntity::getDistance);
            Optional<Integer> secondDistance = getMailEntities().byAddress(secondEntity).map(MailEntity::getDistance);

            if (firstDistance.isPresent() && secondDistance.isPresent()) {
                return Optional.of(Math.max(firstDistance.get(), secondDistance.get()));
            }
            return firstDistance.or(() -> secondDistance);
        }

        if (first instanceof Address.Entity entityAddress) {
            return getMailEntities().byAddress(entityAddress).map(MailEntity::getDistance);
        }

        if (second instanceof Address.Entity entityAddress) {
            return getMailEntities().byAddress(entityAddress).map(MailEntity::getDistance);
        }

        Optional<BlockPos> firstPos = getPositionOf(first);
        Optional<BlockPos> secondPos = getPositionOf(second);

        if (firstPos.isEmpty() || secondPos.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((int) Math.sqrt(firstPos.get().distSqr(secondPos.get())));
    }

    public int getDistanceBetweenOrDefault(Address first, Address second) {
        return getDistanceBetween(first, second).orElse(Config.Server.DELIVERY_DEFAULT_DISTANCE.get());
    }

    // --

    public Mail deliverMail(ServerLevel level, Address address, Mail mail) {
        if (mail.isEmpty()) return mail;
        return address.map(PigeonholeMailReceiver::new, PlayerMailReceiver::new, EntityMailReceiver::new).receiveMail(level, mail);
    }

    // --

    public void tick() {
        getBackgroundDelivery().tick(level);
        getPaybackDepartment().tick();

        if (level.getGameTime() % 20 == 0) Bugger.MAIL_SERVICE.collectAndSendData(this);
    }

    // --

    public long getGameTime() {
        return getLevel().getGameTime();
    }
}
