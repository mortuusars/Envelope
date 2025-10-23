package io.github.mortuusars.envelope.world.service;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DefaultAddresses extends SavedData {
    public static final Codec<DefaultAddresses> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Address.Pigeonhole.STRING_CODEC)
          .xmap(DefaultAddresses::new, DefaultAddresses::get);

    protected final Map<UUID, Address.Pigeonhole> map;

    public DefaultAddresses(Map<UUID, Address.Pigeonhole> map) {
        this.map = new HashMap<>(map); // Make sure it's modifiable.
    }

    public DefaultAddresses() {
        this.map = new HashMap<>();
    }

    public Map<UUID, Address.Pigeonhole> get() {
        return map;
    }

    // --

    public void set(UUID uuid, Address.Pigeonhole address) {
        map.put(uuid, address);
        setDirty();
    }

    public void set(Player player, Address.Pigeonhole address) {
        set(player.getUUID(), address);
    }

    public void remove(Address.Pigeonhole address) {
        if (map.entrySet().removeIf(entry -> entry.getValue().equals(address))) {
            setDirty();
        }
    }

    public Optional<Address.Pigeonhole> of(UUID uuid) {
        return Optional.ofNullable(map.get(uuid));
    }

    public Optional<Address.Pigeonhole> of(Player player) {
        return of(player.getUUID());
    }

    // -- Save / Load

    public static DefaultAddresses get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(DefaultAddresses.factory(), "envelope_default_addresses");
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, NbtOps.INSTANCE, tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot save DefaultAddresses: {}", e.message()))
              .result()
              .filter(t -> t instanceof CompoundTag)
              .map(t -> ((CompoundTag) t))
              .orElse(tag);
    }

    private static DefaultAddresses load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot load DefaultAddresses: {}", e.message()))
              .result()
              .map(Pair::getFirst)
              .orElseGet(DefaultAddresses::new);
    }

    private static Factory<DefaultAddresses> factory() {
        return new Factory<>(DefaultAddresses::new, DefaultAddresses::load, null);
    }
}
