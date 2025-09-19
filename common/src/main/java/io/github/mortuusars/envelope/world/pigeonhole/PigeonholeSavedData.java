package io.github.mortuusars.envelope.world.pigeonhole;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PigeonholeSavedData extends SavedData {
    public static final Codec<PigeonholeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Address.Pigeonhole.STRING_CODEC, PigeonholeData.CODEC)
                    .optionalFieldOf("pigeonholes", new HashMap<>()).forGetter(PigeonholeSavedData::getPigeonholes),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Address.Pigeonhole.STRING_CODEC)
                    .optionalFieldOf("player_addresses", new HashMap<>()).forGetter(PigeonholeSavedData::getPlayerAddresses)
    ).apply(instance, PigeonholeSavedData::new));

    private final Map<Address.Pigeonhole, PigeonholeData> pigeonholes;
    private final Map<UUID, Address.Pigeonhole> playerAddresses;

    protected PigeonholeSavedData(Map<Address.Pigeonhole, PigeonholeData> pigeonholes, Map<UUID, Address.Pigeonhole> playerAddresses) {
        this.pigeonholes = new HashMap<>(pigeonholes);
        this.playerAddresses = new HashMap<>(playerAddresses);
    }

    protected PigeonholeSavedData() {
        this(new HashMap<>(), new HashMap<>());
    }

    public Map<Address.Pigeonhole, PigeonholeData> getPigeonholes() {
        return pigeonholes;
    }

    public Map<UUID, Address.Pigeonhole> getPlayerAddresses() {
        return playerAddresses;
    }

    // --

    public static PigeonholeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), "envelope_pigeonholes");
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot save PigeonholeSavedData: {}", e.message()))
                .result()
                .filter(t -> t instanceof CompoundTag)
                .map(t -> ((CompoundTag) t))
                .orElse(tag);
    }

    private static PigeonholeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot load PigeonholeSavedData: {}", e.message()))
                .result().map(Pair::getFirst).orElseGet(PigeonholeSavedData::new);
    }

    private static Factory<PigeonholeSavedData> factory() {
        return new Factory<>(PigeonholeSavedData::new, PigeonholeSavedData::load, null);
    }
}
