package io.github.mortuusars.envelope.world.service;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import org.jetbrains.annotations.Nullable;

public class AddressHelper {
    private final EnvelopeContext context;

    public AddressHelper(EnvelopeContext context) {
        this.context = context;
    }

    public AllAddresses getAll() {
        return new AllAddresses(
              context.getPigeonholeManager().getAllAddresses(),
              context.getKnownPlayers().getAllAddresses(),
              context.getMailEntities().getAllAddresses()
        );
    }

    public AllAddresses getAll(@Nullable Address.Type type) {
        if (type == null) {
            return getAll();
        }
        return switch (type) {
            case PIGEONHOLE -> AllAddresses.pigeonholes(context.getPigeonholeManager().getAllAddresses());
            case PLAYER -> AllAddresses.players(context.getKnownPlayers().getAllAddresses());
            case ENTITY -> AllAddresses.entities(context.getMailEntities().getAllAddresses());
        };
    }
}
