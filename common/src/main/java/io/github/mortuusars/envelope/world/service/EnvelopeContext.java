package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public void tick() {
        getBackgroundDelivery().tick(level);

        if (Bugger.isEnabled() && this.level.getGameTime() % 20 == 0) {
            int activeCouriers = (int) getBackgroundDelivery().getCouriers().stream().filter(Courier::isDelivering).count();
            int finishedCouriers = getBackgroundDelivery().getCouriers().size() - activeCouriers;
            Bugger.ENVELOPE.sendValues(tag -> {
                tag.putInt("pigeonholes", getPigeonholeManager().getAllAddresses().size());
                tag.putInt("delivering_pigeons", this.level.getEntities(EntityTypeTest.forClass(Pigeon.class), Pigeon::isDelivering).size());
                tag.putInt("background_delivering_pigeons", activeCouriers);
                tag.putInt("background_finished_pigeons", finishedCouriers);
            });
        }
    }
}
