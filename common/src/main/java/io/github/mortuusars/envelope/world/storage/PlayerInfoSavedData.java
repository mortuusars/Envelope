package io.github.mortuusars.envelope.world.storage;

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

public class PlayerInfoSavedData extends SavedData {
    public static final Codec<PlayerInfoSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, UUIDUtil.CODEC)
                    .optionalFieldOf("names", new HashMap<>()).forGetter(PlayerInfoSavedData::getNames),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Address.Pigeonhole.STRING_CODEC)
                    .optionalFieldOf("default_addresses", new HashMap<>()).forGetter(PlayerInfoSavedData::getDefaultAddresses)
    ).apply(instance, PlayerInfoSavedData::new));

    private final Map<String, UUID> names;
    private final Map<UUID, Address.Pigeonhole> defaultAddresses;

    public PlayerInfoSavedData(Map<String, UUID> names, Map<UUID, Address.Pigeonhole> defaultAddresses) {
        this.names = new HashMap<>(names); // Make sure it's modifiable.
        this.defaultAddresses = new HashMap<>(defaultAddresses); // Make sure it's modifiable.
    }

    public PlayerInfoSavedData() {
        this(new HashMap<>(), new HashMap<>());
    }

    public Map<String, UUID> getNames() {
        return names;
    }

    public Map<UUID, Address.Pigeonhole> getDefaultAddresses() {
        return defaultAddresses;
    }

    // --

    public static PlayerInfoSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PlayerInfoSavedData.factory(), "envelope_player_info");
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot save PlayerInfoSavedData: {}", e.message()))
                .result()
                .filter(t -> t instanceof CompoundTag)
                .map(t -> ((CompoundTag) t))
                .orElse(tag);
    }

    private static PlayerInfoSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot load PlayerInfoSavedData: {}", e.message()))
                .result().map(Pair::getFirst).orElseGet(PlayerInfoSavedData::new);
    }

    private static Factory<PlayerInfoSavedData> factory() {
        return new Factory<>(PlayerInfoSavedData::new, PlayerInfoSavedData::load, null);
    }
}
