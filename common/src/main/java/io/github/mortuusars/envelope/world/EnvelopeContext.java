package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.AllAddresses;
import io.github.mortuusars.envelope.world.pigeonhole.PigeonholeManager;
import net.minecraft.server.level.ServerLevel;

public class EnvelopeContext {
    protected final ServerLevel level;
    protected final PigeonholeManager pigeonholeManager;

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

    // --

    public AllAddresses getKnownAddresses() {
        return new AllAddresses(
              getPigeonholeManager().getAllAddresses(),
              level.getEnvelopePlayerInformation().getKnownPlayers().getAllAddresses(),
              Envelope.MAIL_ENTITIES.getAllAddresses()
        );
    }
}
