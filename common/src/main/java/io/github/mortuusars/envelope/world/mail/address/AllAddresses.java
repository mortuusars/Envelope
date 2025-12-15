package io.github.mortuusars.envelope.world.mail.address;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;
import java.util.stream.Stream;

public record AllAddresses(Set<Address.Block> blocks, Set<Address.Player> players, Set<Address.Entity> entities) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AllAddresses> STREAM_CODEC = StreamCodec.composite(
            Address.Block.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::blocks,
            Address.Player.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::players,
            Address.Entity.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::entities,
            AllAddresses::new
    );

    public static final AllAddresses EMPTY = new AllAddresses(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

    public static AllAddresses pigeonholes(Set<Address.Block> addresses) {
        return new AllAddresses(addresses, Collections.emptySet(), Collections.emptySet());
    }

    public static AllAddresses players(Set<Address.Player> addresses) {
        return new AllAddresses(Collections.emptySet(), addresses, Collections.emptySet());
    }

    public static AllAddresses entities(Set<Address.Entity> addresses) {
        return new AllAddresses(Collections.emptySet(), Collections.emptySet(), addresses);
    }

    public Stream<Address> stream() {
        return Stream.of(blocks, players, entities).flatMap(Set::stream);
    }

    public Optional<Address> byName(String name) {
        return stream().filter(a -> a.matches(name)).findFirst();
    }

    public boolean isKnown(String name) {
        return byName(name).isPresent();
    }

    public boolean isKnown(Address address) {
        return stream().anyMatch(a -> a.matches(address));
    }

    public boolean isKnownOfType(Address address, Address.Type type) {
        return switch (type) {
            case BLOCK -> blocks.stream().anyMatch(a -> a.matches(address));
            case PLAYER -> players.stream().anyMatch(a -> a.matches(address));
            case ENTITY -> entities.stream().anyMatch(a -> a.matches(address));
        };
    }
}