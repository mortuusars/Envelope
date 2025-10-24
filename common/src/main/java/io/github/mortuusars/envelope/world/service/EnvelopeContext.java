package io.github.mortuusars.envelope.world.service;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeManager;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnvelopeContext {
    protected final ServerLevel level;
    protected final PigeonholeManager pigeonholeManager;

    protected @Nullable KnownPlayers knownPlayers;
    protected @Nullable DefaultAddresses defaultAddresses;
    protected @Nullable BackgroundDelivery backgroundDelivery;

    public EnvelopeContext(ServerLevel level) {
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
        return knownPlayers == null ? knownPlayers = KnownPlayers.get(level) : knownPlayers;
    }

    public @NotNull DefaultAddresses getDefaultAddresses() {
        return defaultAddresses == null ? defaultAddresses = DefaultAddresses.get(level) : defaultAddresses;
    }

    public @NotNull BackgroundDelivery getBackgroundDelivery() {
        return backgroundDelivery == null ? backgroundDelivery = BackgroundDelivery.get(level) : backgroundDelivery;
    }

    // --

    public AllAddresses getKnownAddresses() {
        return new AllAddresses(
              getPigeonholeManager().getAllAddresses(),
              getKnownPlayers().getAllAddresses(),
              Envelope.MAIL_ENTITIES.getAllAddresses()
        );
    }
}
