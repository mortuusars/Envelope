package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;
import java.util.stream.Stream;

public class AllAddresses {
    public static final StreamCodec<RegistryFriendlyByteBuf, AllAddresses> STREAM_CODEC = StreamCodec.composite(
          BlockAddress.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::blocks,
          PlayerAddress.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::players,
          ServiceAddress.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::services,
          AllAddresses::new
    );

    public static final AllAddresses EMPTY = new AllAddresses(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    private final Set<BlockAddress> blocks;
    private final Set<PlayerAddress> players;
    private final Set<ServiceAddress> services;

    public AllAddresses(Set<BlockAddress> blocks, Set<PlayerAddress> players, Set<ServiceAddress> services) {
        this.blocks = blocks;
        this.players = players;
        this.services = services;
    }

    public static AllAddresses blocks(Set<BlockAddress> addresses) {
        return new AllAddresses(addresses, Collections.emptySet(), Collections.emptySet());
    }

    public static AllAddresses players(Set<PlayerAddress> addresses) {
        return new AllAddresses(Collections.emptySet(), addresses, Collections.emptySet());
    }

    public static AllAddresses services(Set<ServiceAddress> addresses) {
        return new AllAddresses(Collections.emptySet(), Collections.emptySet(), addresses);
    }

    // --

    public Stream<Address> stream() {
        return Stream.of(blocks, players, services).flatMap(Set::stream);
    }

    public Set<BlockAddress> blocks() {
        return blocks;
    }

    public Set<PlayerAddress> players() {
        return players;
    }

    public Set<ServiceAddress> services() {
        return services;
    }

    // --

    public Optional<Address> byName(String name) {
        return stream()
              .filter(address -> address.matches(name))
              .findFirst();
    }

    public boolean isKnown(String name) {
        return byName(name).isPresent();
    }

    public boolean isKnown(Address address) {
        return stream().anyMatch(a -> a.equals(address));
    }

    public boolean isKnownOfType(Address address, Address.Type type) {
        if (type == Address.Type.CUSTOM || type == Address.Type.UNKNOWN) {
            return false;
        }
        return stream()
              .filter(a -> a.getType() == type)
              .anyMatch(a -> a.equals(address));
    }

    // --

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AllAddresses) obj;
        return Objects.equals(this.blocks, that.blocks) &&
              Objects.equals(this.players, that.players) &&
              Objects.equals(this.services, that.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks, players, services);
    }

    @Override
    public String toString() {
        return "AllAddresses[" +
              "blocks=" + blocks + ", " +
              "players=" + players + ", " +
              "entities=" + services + ']';
    }
}