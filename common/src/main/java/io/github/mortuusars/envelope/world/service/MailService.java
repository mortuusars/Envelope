package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.mail.entity.MailEntities;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.MailServiceEntity;
import io.github.mortuusars.envelope.world.mail.receiver.EntityMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.PigeonholeMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.PlayerMailReceiver;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MailService {
    protected final ServerLevel level;

    protected final PigeonholeManager pigeonholeManager;
    protected final MailEntities mailEntities;
    protected final MailServiceEntity mailServiceEntity;
    protected final DeliveryManager deliveryManager;

    protected @Nullable Players players;
    protected @Nullable BackgroundDelivery backgroundDelivery;

    private MailService(ServerLevel level) {
        Preconditions.checkArgument(level.dimension() == Level.OVERWORLD, "EnvelopeContext can exist only on overworld level.");
        this.level = level;
        this.pigeonholeManager = new PigeonholeManager(level);
        this.mailEntities = new MailEntities();
        this.mailServiceEntity = new MailServiceEntity(this);
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
     * @return "final" address. Mostly for getting default pigeonhole address of a player.
     */
    public Address resolve(Address address) {
        if (address instanceof Address.Player playerAddress) {
            return getPlayerDefaultAddress(playerAddress).map(Address.class::cast).orElse(address);
        }
        return address;
    }

    public boolean canDeliverMailTo(Address address) {
        if (address.equals(Address.UNKNOWN)) {
            return false;
        }
        address = resolve(address);
        return getKnownAddresses().isKnown(address);
    }

    public Optional<BlockPos> getPositionOf(Address address) {
        return Position.ofAddress(level, address);
    }

    public Optional<Integer> getDistanceBetween(Address first, Address second) {
        if (second instanceof Address.Entity entity) {
            return MailService.of(level).getMailEntities().byAddress(entity).map(MailEntity::getDistance);
        }

        Optional<BlockPos> firstPos = Position.ofAddress(level, first);
        Optional<BlockPos> secondPos = Position.ofAddress(level, second);

        if (firstPos.isEmpty() || secondPos.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((int) Math.sqrt(firstPos.get().distSqr(secondPos.get())));
    }

    public int getDistanceBetweenOrDefault(Address first, Address second) {
        return getDistanceBetween(first, second).orElse(Config.Server.DELIVERY_DEFAULT_DISTANCE.get());
    }

    // --

    public Mail receiveMail(ServerLevel level, Address address, Mail mail) {
        if (mail.isEmpty()) return Mail.EMPTY;
        return address.map(PigeonholeMailReceiver::new, PlayerMailReceiver::new, EntityMailReceiver::new).receiveMail(level, mail);
    }

    // --

    public void tick() {
        getBackgroundDelivery().tick(level);
        getMailService().tick();

        if (level.getGameTime() % 20 == 0) {
            Bugger.ENVELOPE.sendValues(this::collectDebugInfo);
        }
    }

    // --

    private void collectDebugInfo(CompoundTag tag) {
        List<? extends Pigeon> pigeons = level.getEntities(EntityTypeTest.forClass(Pigeon.class), Pigeon::isDelivering);
        List<BackgroundCourier> backgroundCouriers = getBackgroundDelivery().getCouriers();

        tag.putInt("pigeonholes", getPigeonholeManager().getAllAddresses().size());
        tag.putInt("delivering_pigeons", pigeons.size());
        tag.putInt("background_delivering_pigeons", backgroundCouriers.size());
        tag.putInt("background_finished_pigeons", getBackgroundDelivery().getFinishedCouriers().size());

        tag.putInt("mail_awaiting_payback", getMailService().getPaybackDepartment().getMailAwaitingPaybackCount());

        ListTag deliveries = Stream.concat(
                    pigeons.stream().map(p -> p.getDelivery().orElseThrow()),
                    backgroundCouriers.stream().map(BackgroundCourier::delivery))
              .sorted(Comparator.comparingInt(Delivery::hashCode))
              .map(delivery -> StringTag.valueOf(formDeliveryString(delivery)))
              .collect(Collectors.toCollection(ListTag::new));

        tag.put("deliveries", deliveries);
    }

    private @NotNull String formDeliveryString(Delivery delivery) {
        return ChatFormatting.AQUA + delivery.getSender().format().withIcon().toString() + ChatFormatting.RESET +
              " " + EnvelopeSymbols.SMALL_FILLED_ARROW_RIGHT + " " +
              ChatFormatting.GREEN + delivery.getRecipient().format().withIcon().toString() + ChatFormatting.RESET +

              ChatFormatting.GRAY +
              (!delivery.getMail().isEmpty() ? " " + delivery.getMail().getItemForReading().getHoverName().getString() : "") +
              getDistanceBetween(delivery.getSender(), delivery.getRecipient()).map(d -> " | ↔" + d).orElse("") +
              " | ⌚" + delivery.getTravelDuration().seconds() + "s" +
              ChatFormatting.RESET +

              " // " + delivery.getCurrentPhase().toPrettyString() +

              ChatFormatting.GRAY +
              " ⌛" + (delivery.getProgress().getDuration() - delivery.getProgress().getTicks()) / 20 +
              ChatFormatting.RESET;
    }

    // --

    public long getGameTime() {
        return getLevel().getGameTime();
    }
}
