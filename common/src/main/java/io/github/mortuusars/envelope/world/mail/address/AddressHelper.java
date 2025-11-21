package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.service.EnvelopeContext;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AddressHelper {
    private final EnvelopeContext context;

    public AddressHelper(EnvelopeContext context) {
        this.context = context;
    }

    public AllAddresses getAll() {
        return new AllAddresses(
              context.getPigeonholeManager().getAllAddresses(),
              context.getPlayers().getDefaultAddresses().keySet(),
              context.getMailEntities().getAllAddresses()
        );
    }

    public AllAddresses getAll(@Nullable Address.Type type) {
        if (type == null) {
            return getAll();
        }
        return switch (type) {
            case PIGEONHOLE -> AllAddresses.pigeonholes(context.getPigeonholeManager().getAllAddresses());
            case PLAYER -> AllAddresses.players(context.getPlayers().getDefaultAddresses().keySet());
            case ENTITY -> AllAddresses.entities(context.getMailEntities().getAllAddresses());
        };
    }

    public Optional<Address.Pigeonhole> getPlayerDefaultAddress(Address.Player playerAddress) {
        return Optional.ofNullable(context.getPlayers().getDefaultAddresses().get(playerAddress));
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

    /**
     * @return {@code true} if address can receive mail.
     */
    public boolean canDeliverMailTo(Address address) {
        address = resolve(address);
        return getAll().isKnown(address);
    }

    public Optional<Integer> getDistanceTo(Address from, Address to) {
        if (to instanceof Address.Entity entity) {
            return context.getMailEntities().byAddress(entity).map(MailEntity::getDistance);
        }

        Optional<BlockPos> fromPos = Position.ofAddress(context.getLevel(), from);
        Optional<BlockPos> toPos = Position.ofAddress(context.getLevel(), to);
        return fromPos.isEmpty() || toPos.isEmpty()
              ? Optional.empty()
              : Optional.of((int) Math.sqrt(fromPos.get().distSqr(toPos.get())));
    }
}
