package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.Formatting;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.delivery.CourierOrigin;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundDelivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
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

    protected @Nullable KnownPlayers knownPlayers;
    protected @Nullable DefaultAddresses defaultAddresses;
    protected @Nullable BackgroundDelivery backgroundDelivery;

    public EnvelopeContext(ServerLevel level) {
        Preconditions.checkArgument(level.dimension() == Level.OVERWORLD, "EnvelopeContext can exist only on overworld level.");
        this.level = level;
        this.pigeonholeManager = new PigeonholeManager(level);
    }

    public ServerLevel getLevel() {
        return level;
    }

    public PigeonholeManager getPigeonholeManager() {
        return pigeonholeManager;
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

    // --

    public AllAddresses getKnownAddresses() {
        return new AllAddresses(
              getPigeonholeManager().getAllAddresses(),
              getKnownPlayers().getAllAddresses(),
              Envelope.MAIL_ENTITIES.getAllAddresses()
        );
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

        if (Bugger.isEnabled() && level.getGameTime() % 20 == 0) {
            Bugger.ENVELOPE.sendValues(tag -> {
                List<? extends Pigeon> pigeons = level.getEntities(EntityTypeTest.forClass(Pigeon.class), Pigeon::isDelivering);
                List<BackgroundCourier> backgroundCouriers = getBackgroundDelivery().getCouriers();

                tag.putInt("pigeonholes", getPigeonholeManager().getAllAddresses().size());
                tag.putInt("delivering_pigeons", pigeons.size());
                tag.putInt("background_delivering_pigeons", backgroundCouriers.size());
                tag.putInt("background_finished_pigeons", getBackgroundDelivery().getFinishedCouriers().size());

                ListTag deliveries = Stream.concat(pigeons.stream().map(p -> p.getDelivery().orElseThrow()),
                            backgroundCouriers.stream().map(BackgroundCourier::delivery))
                      .map(delivery ->
                            StringTag.valueOf("§b" + delivery.getSender().getString() + "§r "
                                  + EnvelopeSymbols.SMALL_FILLED_ARROW_RIGHT
                                  + " §a" + delivery.getRecipient().getString() + "§r - "
                            + delivery.getCurrentPhase().toPrettyString()))
                      .collect(Collectors.toCollection(ListTag::new));

                tag.put("deliveries", deliveries);
            });
        }
    }
}
