package io.github.mortuusars.envelope.world.service;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class KnownPlayers extends SavedData {
    public static final Codec<KnownPlayers> CODEC = Codec.unboundedMap(Codec.STRING, UUIDUtil.CODEC)
          .xmap(KnownPlayers::new, KnownPlayers::get);

    protected final Map<String, UUID> map;
    protected @Nullable Set<Address.Player> addresses = null;

    public KnownPlayers(Map<String, UUID> map) {
        this.map = new HashMap<>(map); // Make sure it's modifiable.
    }

    public KnownPlayers() {
        this.map = new HashMap<>();
    }

    public Map<String, UUID> get() {
        return map;
    }

    // --

    public void add(Player player) {
        map.put(player.getScoreboardName(), player.getUUID());
        setDirty();
        resetAddresses();
    }

    public void remove(String name) {
        if (map.remove(name) != null) {
            setDirty();
            resetAddresses();
        }
    }

    public void clear() {
        map.clear();
        setDirty();
        resetAddresses();
    }

    public Optional<UUID> getUuid(String name) {
        return Optional.ofNullable(map.get(name));
    }

    public Optional<UUID> getUuid(Address.Player address) {
        return Optional.ofNullable(map.get(address.id()));
    }

    public Optional<String> getName(UUID uuid) {
        for (var entry : map.entrySet()) {
            if (Objects.equals(entry.getValue(), uuid)) {
                return Optional.ofNullable(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public @NotNull Set<Address.Player> getAllAddresses() {
        if (addresses == null) {
            addresses = map.keySet().stream()
                  .map(Address.Player::new)
                  .collect(Collectors.toSet());
        }
        return addresses;
    }

    private void resetAddresses() {
        addresses = null;
    }

    // -- Save / Load

    public static KnownPlayers get(ServerLevel level, String name) {
        return level.getDataStorage().computeIfAbsent(factory(), name);
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot save KnownPlayers: {}", e.message()))
                .result()
                .filter(t -> t instanceof CompoundTag)
                .map(t -> ((CompoundTag) t))
                .orElse(tag);
    }

    private static KnownPlayers load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot load KnownPlayers: {}", e.message()))
                .result().map(Pair::getFirst).orElseGet(KnownPlayers::new);
    }

    private static Factory<KnownPlayers> factory() {
        return new Factory<>(KnownPlayers::new, KnownPlayers::load, null);
    }
}
