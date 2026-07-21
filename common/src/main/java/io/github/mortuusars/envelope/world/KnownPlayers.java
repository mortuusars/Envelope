package io.github.mortuusars.envelope.world;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
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

public class KnownPlayers extends SavedData {
    public static final Codec<KnownPlayers> CODEC = Codec.unboundedMap(Codec.STRING, PlayerData.CODEC)
          .xmap(KnownPlayers::new, KnownPlayers::getData);

    protected final Map<String, PlayerData> data;
    protected @Nullable Set<PlayerAddress> addresses;
    protected @Nullable Map<PlayerAddress, BlockAddress> defaultAddresses;

    public KnownPlayers(Map<String, PlayerData> data) {
        this.data = new HashMap<>(data); // Make sure it's modifiable
    }

    public KnownPlayers() {
        this(Collections.emptyMap());
    }

    protected Map<String, PlayerData> getData() {
        return data;
    }

    // --

    public void add(Player player) {
        if (player.getScoreboardName().equalsIgnoreCase("replay viewer")) {
            // Flashback mod uses space in the player name, which causes an error.
            // A mod about pigeons should not validate player names by the mojang's rules.
            return;
        }
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

    public Optional<PlayerData> getDataOf(UUID uuid) {
        return data.values().stream().filter(d -> d.profile.getId().equals(uuid)).findFirst();
    }

    // --

    public Optional<BlockAddress> getDefaultAddressOf(Player player) {
        return getDataOf(player.getScoreboardName()).flatMap(PlayerData::getDefaultAddress);
    }

    public Optional<BlockAddress> getDefaultAddressOf(PlayerAddress playerAddress) {
        return getDataOf(playerAddress.getString()).flatMap(PlayerData::getDefaultAddress);
    }

    public void setDefaultAddress(Player player, BlockAddress address) {
        update(player, data -> data.setDefaultAddress(address));
    }

    public void renameDefaultAddress(BlockAddress oldAddress, BlockAddress newAddress) {
        getData().values().forEach(data -> {
            if (data.getDefaultAddress().map(a -> a.equals(oldAddress)).orElse(false)) {
                data.setDefaultAddress(newAddress);
            }
        });
        setDirty();
    }

    public void removeDefaultAddress(BlockAddress address) {
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

    // --

    public @NotNull Set<PlayerAddress> getAllAddresses() {
        if (addresses == null) {
            addresses = data.keySet().stream()
                  .map(PlayerAddress::new)
                  .collect(Collectors.toSet());
        }
        return addresses;
    }

    public Map<PlayerAddress, BlockAddress> getDefaultAddresses() {
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

    // --

    public static class PlayerData {
        public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
              ExtraCodecs.GAME_PROFILE_WITHOUT_PROPERTIES.codec().fieldOf("profile").forGetter(PlayerData::getProfile),
              BlockAddress.STRING_CODEC.optionalFieldOf("default_address").forGetter(PlayerData::getDefaultAddress)
        ).apply(i, PlayerData::new));

        protected final GameProfile profile;
        protected final PlayerAddress address;
        protected Optional<BlockAddress> defaultAddress;

        public PlayerData(GameProfile profile, Optional<BlockAddress> defaultAddress) {
            this.profile = profile;
            this.address = new PlayerAddress(profile.getName());
            this.defaultAddress = defaultAddress;
        }

        public PlayerData(GameProfile profile) {
            this(profile, Optional.empty());
        }

        public GameProfile getProfile() {
            return profile;
        }

        public PlayerAddress getAddress() {
            return address;
        }

        public Optional<BlockAddress> getDefaultAddress() {
            return defaultAddress;
        }

        public void setDefaultAddress(@Nullable BlockAddress address) {
            this.defaultAddress = Optional.ofNullable(address);
        }
    }
}
