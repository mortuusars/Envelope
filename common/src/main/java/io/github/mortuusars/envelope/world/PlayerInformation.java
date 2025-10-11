package io.github.mortuusars.envelope.world;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.storage.PlayerInfoSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerInformation {
    protected static final Logger LOGGER = LogUtils.getLogger();

    protected final ServerLevel level;
    protected final KnownPlayers knownPlayers = new KnownPlayers();
    protected final DefaultAddress defaultAddress = new DefaultAddress();

    protected @Nullable PlayerInfoSavedData data = null;

    public PlayerInformation(ServerLevel level) {
        this.level = level;
    }

    protected PlayerInfoSavedData data() {
        if (data == null) {
            data = PlayerInfoSavedData.get(level);
        }
        return data;
    }

    // --

    public KnownPlayers getKnownPlayers() {
        return knownPlayers;
    }

    public DefaultAddress getDefaultAddress() {
        return defaultAddress;
    }

    // --

    public class KnownPlayers {
        protected @Nullable Set<Address.Player> addresses = null;

        public void add(Player player) {
            data().getNames().put(player.getScoreboardName(), player.getUUID());
            data().setDirty();
            addresses = null;
        }

        public void remove(String name) {
            if (data().getNames().remove(name) != null) {
                data().setDirty();
                addresses = null;
            }
        }

        public void clear() {
            data().getNames().clear();
            data().setDirty();
            addresses = null;
        }

        public Map<String, UUID> getAll() {
            return Collections.unmodifiableMap(data().getNames());
        }

        public Optional<UUID> getUuid(String name) {
            return Optional.ofNullable(data().getNames().get(name));
        }

        public Optional<String> getName(UUID uuid) {
            for (var entry : data().getNames().entrySet()) {
                if (Objects.equals(entry.getValue(), uuid)) {
                    return Optional.ofNullable(entry.getKey());
                }
            }
            return Optional.empty();
        }

        public @NotNull Set<Address.Player> getAllAddresses() {
            if (addresses == null) {
                addresses = data().getNames().keySet().stream()
                    .map(Address.Player::new)
                    .collect(Collectors.toSet());
            }
            return addresses;
        }
    }

    // --

    public class DefaultAddress {
        public void set(UUID uuid, Address.Pigeonhole address) {
            data().getDefaultAddresses().put(uuid, address);
            data().setDirty();
            LOGGER.debug("Default address of player '{}' has been set to '{}'.", getKnownPlayers().getName(uuid).orElse(uuid.toString()), address.id());
        }

        public void set(Player player, Address.Pigeonhole address) {
            set(player.getUUID(), address);
        }

        public void remove(Address.Pigeonhole address) {
            if (data().getDefaultAddresses().entrySet().removeIf(entry -> entry.getValue().equals(address))) {
                data().setDirty();
            }
        }

        public Optional<Address.Pigeonhole> of(UUID uuid) {
            return Optional.ofNullable(data().getDefaultAddresses().get(uuid));
        }

        public Optional<Address.Pigeonhole> of(Player player) {
            return of(player.getUUID());
        }

        public Optional<Address.Pigeonhole> of(Address.Player address) {
            return getKnownPlayers().getUuid(address.id())
                .flatMap(uuid -> getDefaultAddress().of(uuid));
        }
    }
}
