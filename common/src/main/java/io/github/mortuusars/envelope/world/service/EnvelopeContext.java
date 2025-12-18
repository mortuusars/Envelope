package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressHelper;
import io.github.mortuusars.envelope.world.mail.entity.MailEntities;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.MailService;
import io.github.mortuusars.envelope.world.mail.entity.VillagerMailEntity;
import io.github.mortuusars.envelope.world.mail.receiver.EntityMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.PigeonholeMailReceiver;
import io.github.mortuusars.envelope.world.mail.receiver.PlayerMailReceiver;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnvelopeContext {
    protected final ServerLevel level;
    protected final PigeonholeManager pigeonholeManager;
    protected final MailEntities mailEntities;
    protected final AddressHelper addressHelper;
    protected final MailService mailService;
    protected final DeliveryManager deliveryManager;

    protected @Nullable Players players;
    protected @Nullable BackgroundDelivery backgroundDelivery;

    public EnvelopeContext(ServerLevel level) {
        Preconditions.checkArgument(level.dimension() == Level.OVERWORLD, "EnvelopeContext can exist only on overworld level.");
        this.level = level;
        this.pigeonholeManager = new PigeonholeManager(level);
        this.mailEntities = new MailEntities();
        this.addressHelper = new AddressHelper(this);
        this.mailService = new MailService(this);
        this.deliveryManager = new DeliveryManager(this);

        this.mailEntities.register(mailService);
        this.mailEntities.register(new VillagerMailEntity(new Address.Entity("Villager"), 1500));
    }

    public static EnvelopeContext of(ServerLevel level) {
        return level.getEnvelopeContext();
    }

    public static EnvelopeContext of(ServerPlayer player) {
        return player.serverLevel().getEnvelopeContext();
    }

    public ServerLevel getLevel() {
        return level;
    }

    public PigeonholeManager getPigeonholeManager() {
        return pigeonholeManager;
    }

    public MailEntities getMailEntities() {
        return mailEntities;
    }

    public MailService getMailService() {
        return mailService;
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

    public AddressHelper addresses() {
        return addressHelper;
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
              addresses().getDistanceTo(delivery.getSender(), delivery.getRecipient()).map(d -> " | ↔" + d).orElse("") +
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
