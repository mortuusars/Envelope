package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.storage.PlayerInfoSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerInformation {
    protected final KnownPlayers knownPlayers = new KnownPlayers();
    protected final DefaultAddress defaultAddress = new DefaultAddress();
    protected final ServerLevel level;

    public PlayerInformation(ServerLevel level) {
        this.level = level;
    }

    public KnownPlayers getKnownPlayers() {
        return knownPlayers;
    }

    public class KnownPlayers {
        @Nullable
        protected Set<Address.Player> addresses;

        public void add(Player player) {
            PlayerInfoSavedData data = data();
            data.getNames().put(player.getScoreboardName(), player.getUUID());
            data.setDirty();
            addresses = null;
        }

        public void remove(String name) {
            PlayerInfoSavedData data = data();
            if (data.getNames().remove(name) != null) {
                data.setDirty();
                addresses = null;
            }
        }

        public void clear() {
            PlayerInfoSavedData data = data();
            data.getNames().clear();
            data.setDirty();
            addresses = null;
        }

        public Map<String, UUID> getAll() {
            return Collections.unmodifiableMap(data().getNames());
        }

        public Optional<UUID> getUuid(String name) {
            return Optional.ofNullable(data().getNames().get(name));
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

    public DefaultAddress getDefaultAddress() {
        return defaultAddress;
    }

    public class DefaultAddress {
        public Optional<Address.Pigeonhole> of(UUID uuid) {
            return Optional.ofNullable(data().getDefaultAddresses().get(uuid));
        }

        public Optional<Address.Pigeonhole> of(Player player) {
            return Optional.ofNullable(data().getDefaultAddresses().get(player.getUUID()));
        }

        public void set(UUID uuid, Address.Pigeonhole address) {
            PlayerInfoSavedData data = data();
            data.getDefaultAddresses().put(uuid, address);
            data.setDirty();
        }

        public void set(Player player, Address.Pigeonhole address) {
            PlayerInfoSavedData data = data();
            data.getDefaultAddresses().put(player.getUUID(), address);
            data.setDirty();
        }

        public void remove(Address.Pigeonhole address) {
            PlayerInfoSavedData data = data();
            if (data.getDefaultAddresses().entrySet().removeIf(entry -> entry.getValue().equals(address))) {
                data.setDirty();
            }
        }
    }

    // --

    protected PlayerInfoSavedData data() {
        return PlayerInfoSavedData.get(level);
    }
}
