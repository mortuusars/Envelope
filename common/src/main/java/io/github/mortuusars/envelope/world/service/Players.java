package io.github.mortuusars.envelope.world.service;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Players extends SavedData {
    public static final Codec<Players> CODEC = Codec.unboundedMap(Codec.STRING, PlayerData.CODEC)
          .xmap(Players::new, Players::getData);

    protected final Map<String, PlayerData> data;
//    protected @Nullable Map<UUID, PlayerData> dataById;
//    protected @Nullable Map<Address.Player, PlayerData> dataByAddress;
    protected @Nullable Set<Address.Player> addresses;
    protected @Nullable Map<Address.Player, Address.Block> defaultAddresses;

    public Players(Map<String, PlayerData> data) {
        this.data = new HashMap<>(data); // Make sure it's modifiable
    }

    public Players() {
        this(Collections.emptyMap());
    }

    protected Map<String, PlayerData> getData() {
        return data;
    }

    // --

    public void add(Player player) {
        data.computeIfAbsent(player.getScoreboardName(), name -> new PlayerData(player.getGameProfile()));
        setDirty();
    }

    public void remove(String name) {
        if (data.remove(name) != null) {
            setDirty();
        }
    }

    public void clear() {
        data.clear();
        setDirty();
    }

    // --

    public Optional<PlayerData> getDataOf(String playerName) {
        return Optional.ofNullable(data.get(playerName));
    }

    // --

    public Optional<Address.Block> getDefaultAddressOf(Player player) {
        return getDataOf(player.getScoreboardName()).flatMap(PlayerData::getDefaultAddress);
    }

    public Optional<Address.Block> getDefaultAddressOf(Address.Player playerAddress) {
        return getDataOf(playerAddress.toString()).flatMap(PlayerData::getDefaultAddress);
    }

    public void setDefaultAddress(Player player, Address.Block address) {
        update(player, data -> data.setDefaultAddress(address));
    }

    public void renameDefaultAddress(Address.Block oldAddress, Address.Block newAddress) {
        getData().values().forEach(data -> {
            if (data.getDefaultAddress().map(a -> a.equals(oldAddress)).orElse(false)) {
                data.setDefaultAddress(newAddress);
            }
        });
        setDirty();
    }

    public void removeDefaultAddress(Address.Block address) {
        getData().values().forEach(data -> {
            if (data.getDefaultAddress().map(a -> a.equals(address)).orElse(false)) {
                data.setDefaultAddress(null);
            }
        });
        setDirty();
    }

    // --

    public void update(Player player, Consumer<PlayerData> updater) {
        data.compute(player.getScoreboardName(), (name, data) -> {
            if (data == null) {
                data = new PlayerData(player.getGameProfile());
            }
            updater.accept(data);
            return data;
        });
        setDirty();
    }

//    public Optional<UUID> getUuid(String name) {
//        return Optional.ofNullable(map.get(name));
//    }
//
//    public Optional<UUID> getUuid(Address.Player address) {
//        return Optional.ofNullable(map.get(address.id()));
//    }
//
//    public Optional<String> getName(UUID uuid) {
//        for (var entry : map.entrySet()) {
//            if (Objects.equals(entry.getValue(), uuid)) {
//                return Optional.ofNullable(entry.getKey());
//            }
//        }
//        return Optional.empty();
//    }

    // --

    public @NotNull Set<Address.Player> getAllAddresses() {
        if (addresses == null) {
            addresses = data.keySet().stream()
                  .map(Address.Player::new)
                  .collect(Collectors.toSet());
        }
        return addresses;
    }

    public Map<Address.Player, Address.Block> getDefaultAddresses() {
        if (defaultAddresses == null) {
            defaultAddresses = new HashMap<>();
            getData().forEach((name, data) ->
                  data.getDefaultAddress().ifPresent(defaultAddress -> defaultAddresses.put(data.getAddress(), defaultAddress)));
        }
        return defaultAddresses;
    }

    @Override
    public void setDirty() {
        super.setDirty();
        resetCache();
    }

    protected void resetCache() {
        addresses = null;
        defaultAddresses = null;
    }

    // -- Save / Load

    public static Players get(ServerLevel level, String name) {
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

    private static Players load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot load KnownPlayers: {}", e.message()))
                .result().map(Pair::getFirst).orElseGet(Players::new);
    }

    private static Factory<Players> factory() {
        return new Factory<>(Players::new, Players::load, null);
    }

    // --

    public static class PlayerData {
        public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
              ExtraCodecs.GAME_PROFILE_WITHOUT_PROPERTIES.codec().fieldOf("profile").forGetter(PlayerData::getProfile),
              Address.Block.STRING_CODEC.optionalFieldOf("default_address").forGetter(PlayerData::getDefaultAddress)
        ).apply(i, PlayerData::new));

        protected final GameProfile profile;
        protected final Address.Player address;
        protected Optional<Address.Block> defaultAddress;

        public PlayerData(GameProfile profile, Optional<Address.Block> defaultAddress) {
            this.profile = profile;
            this.address = new Address.Player(profile.getName());
            this.defaultAddress = defaultAddress;
        }

        public PlayerData(GameProfile profile) {
            this(profile, Optional.empty());
        }

        public GameProfile getProfile() {
            return profile;
        }

        public Address.Player getAddress() {
            return address;
        }

        public Optional<Address.Block> getDefaultAddress() {
            return defaultAddress;
        }

        public void setDefaultAddress(@Nullable Address.Block address) {
            this.defaultAddress = Optional.ofNullable(address);
        }
    }
}
