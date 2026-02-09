package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;
import java.util.stream.Stream;

public class AllAddresses {
    public static final StreamCodec<RegistryFriendlyByteBuf, AllAddresses> STREAM_CODEC = StreamCodec.composite(
          BlockAddress.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::blocks,
          PlayerAddress.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::players,
          EntityAddress.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::entities,
          AllAddresses::new
    );

    public static final AllAddresses EMPTY = new AllAddresses(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    private final Set<BlockAddress> blocks;
    private final Set<PlayerAddress> players;
    private final Set<EntityAddress> entities;

    public AllAddresses(Set<BlockAddress> blocks, Set<PlayerAddress> players, Set<EntityAddress> entities) {
        this.blocks = blocks;
        this.players = players;
        this.entities = entities;
    }

    public static AllAddresses blocks(Set<BlockAddress> addresses) {
        return new AllAddresses(addresses, Collections.emptySet(), Collections.emptySet());
    }

    public static AllAddresses players(Set<PlayerAddress> addresses) {
        return new AllAddresses(Collections.emptySet(), addresses, Collections.emptySet());
    }

    public static AllAddresses entities(Set<EntityAddress> addresses) {
        return new AllAddresses(Collections.emptySet(), Collections.emptySet(), addresses);
    }

    // --

    public Realized realized(RegistryAccess access) {
        return new Realized(blocks, players, entities, access);
    }

    public Stream<Address> stream() {
        return Stream.of(blocks, players, entities).flatMap(Set::stream);
    }

    public Set<BlockAddress> blocks() {
        return blocks;
    }

    public Set<PlayerAddress> players() {
        return players;
    }

    public Set<EntityAddress> entities() {
        return entities;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AllAddresses) obj;
        return Objects.equals(this.blocks, that.blocks) &&
              Objects.equals(this.players, that.players) &&
              Objects.equals(this.entities, that.entities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks, players, entities);
    }

    @Override
    public String toString() {
        return "AllAddresses[" +
              "blocks=" + blocks + ", " +
              "players=" + players + ", " +
              "entities=" + entities + ']';
    }

    // --

    public static class Realized extends AllAddresses {
        private final RegistryAccess access;

        public Realized(Set<BlockAddress> blocks, Set<PlayerAddress> players, Set<EntityAddress> entities, RegistryAccess access) {
            super(blocks, players, entities);
            this.access = access;
        }

        public Optional<Address> byName(String name) {
            return stream()
                  .filter(address -> address.realize(access).matches(name))
                  .findFirst();
        }

        public boolean isKnown(String name) {
            return byName(name).isPresent();
        }

        public boolean isKnown(Address address) {
            return stream().anyMatch(a -> a.realize(access).equals(address));
        }

        public boolean isKnownOfType(Address address, Address.Type type) {
            if (type == Address.Type.CUSTOM || type == Address.Type.UNKNOWN) {
                return false;
            }
            return stream()
                  .filter(a -> a.getType() == type)
                  .anyMatch(a -> a.realize(access).equals(address));
        }
    }
}