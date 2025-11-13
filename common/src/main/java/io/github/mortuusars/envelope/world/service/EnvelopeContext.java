package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.delivery.CourierOrigin;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.entity.MailEntities;
import io.github.mortuusars.envelope.world.mail.entity.VillagerMailEntity;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnvelopeContext {
    protected final ServerLevel level;
    protected final PigeonholeManager pigeonholeManager;
    protected final MailEntities mailEntities;

    protected @Nullable KnownPlayers knownPlayers;
    protected @Nullable DefaultAddresses defaultAddresses;
    protected @Nullable BackgroundDelivery backgroundDelivery;
    protected AddressHelper addressHelper;

    public EnvelopeContext(ServerLevel level) {
        Preconditions.checkArgument(level.dimension() == Level.OVERWORLD, "EnvelopeContext can exist only on overworld level.");
        this.level = level;
        this.pigeonholeManager = new PigeonholeManager(level);
        this.mailEntities = new MailEntities();
        this.addressHelper = new AddressHelper(this);

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

    public @NotNull KnownPlayers getKnownPlayers() {
        return knownPlayers == null
              ? knownPlayers = KnownPlayers.get(level, "envelope_known_players")
              : knownPlayers;
    }

    public @NotNull DefaultAddresses getDefaultAddresses() {
        return defaultAddresses == null
              ? defaultAddresses = DefaultAddresses.get(level, "envelope_default_addresses")
              : defaultAddresses;
    }

    public @NotNull BackgroundDelivery getBackgroundDelivery() {
        return backgroundDelivery == null
              ? backgroundDelivery = BackgroundDelivery.get(level, "envelope_background_delivery")
              : backgroundDelivery;
    }

    public AddressHelper addresses() {
        return addressHelper;
    }

    // --

    public BackgroundCourier startDelivery(ItemStack mail) {
        BackgroundCourier courier = new BackgroundCourier(
              new SpawnableEntityData(Envelope.EntityTypes.PIGEON.get()),
              CourierOrigin.service(),
              Delivery.create(level, mail)
        );
        getBackgroundDelivery().addCourier(courier);
        return courier;
    }

    // --

    public void tick() {
        getBackgroundDelivery().tick(level);

        if (level.getGameTime() % 20 == 0) {
            Bugger.ENVELOPE.sendValues(this::collectDebugInfo);
        }
    }

    private void collectDebugInfo(CompoundTag tag) {
        List<? extends Pigeon> pigeons = level.getEntities(EntityTypeTest.forClass(Pigeon.class), Pigeon::isDelivering);
        List<BackgroundCourier> backgroundCouriers = getBackgroundDelivery().getCouriers();

        tag.putInt("pigeonholes", getPigeonholeManager().getAllAddresses().size());
        tag.putInt("delivering_pigeons", pigeons.size());
        tag.putInt("background_delivering_pigeons", backgroundCouriers.size());
        tag.putInt("background_finished_pigeons", getBackgroundDelivery().getFinishedCouriers().size());

        ListTag deliveries = Stream.concat(pigeons.stream().map(p -> p.getDelivery().orElseThrow()),
                    backgroundCouriers.stream().map(BackgroundCourier::delivery))
              .map(delivery -> StringTag.valueOf(
                    ChatFormatting.AQUA + delivery.getSender().getString() + ChatFormatting.RESET +
                          " " + EnvelopeSymbols.SMALL_FILLED_ARROW_RIGHT + " " +
                          ChatFormatting.GREEN + delivery.getRecipient().getString() + ChatFormatting.RESET +

                          ChatFormatting.GRAY +
                          (!delivery.getMail().isEmpty() ? " ✉" : "") +
                          addresses().getDistanceTo(delivery.getSender(), delivery.getRecipient()).map(d -> " ↔" + d).orElse("") +
                          " ⌚" + delivery.getTravelDuration() / 20 + "s" +
                          ChatFormatting.RESET +

                          " // " + delivery.getCurrentPhase().toPrettyString() +

                          ChatFormatting.GRAY +
                          " ⌛" + (delivery.getProgress().getDuration() - delivery.getProgress().getTicks()) / 20 +
                          ChatFormatting.RESET))
              .collect(Collectors.toCollection(ListTag::new));

        tag.put("deliveries", deliveries);
    }
}
