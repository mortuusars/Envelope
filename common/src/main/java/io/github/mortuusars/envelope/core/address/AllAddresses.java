package io.github.mortuusars.envelope.core.address;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.stream.Stream;

public record AllAddresses(Set<Address.Pigeonhole> pigeonholes, Set<Address.Player> players, Set<Address.Npc> npcs) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AllAddresses> STREAM_CODEC = StreamCodec.composite(
            Address.Pigeonhole.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::pigeonholes,
            Address.Player.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::players,
            Address.Npc.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), AllAddresses::npcs,
            AllAddresses::new
    );

    public static AllAddresses of(ServerLevel level) {
        return new AllAddresses(
                level.getEnvelopePigeonholeManager().getAllAddresses(),
                level.getEnvelopePlayerInformation().getKnownPlayers().getAllAddresses(),
                Envelope.MAIL_ENTITIES.getAllAddresses());
    }

    public Stream<Address> stream() {
        return Stream.of(pigeonholes, players, npcs).flatMap(Set::stream);
    }

    public Optional<Address> byName(String name) {
        return stream().filter(a -> a.matches(name)).findFirst();
    }

    public boolean isKnown(String name) {
        return byName(name).isPresent();
    }

    public boolean isKnown(Address address) {
        return stream().anyMatch(a -> a.matches(address.id()));
    }
}