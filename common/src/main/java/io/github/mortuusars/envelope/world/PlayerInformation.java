package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.world.storage.PlayerInfoSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerInformation {
    protected final KnownNames knownNames = new KnownNames();
    protected final DefaultAddress defaultAddress = new DefaultAddress();
    protected final ServerLevel level;

    public PlayerInformation(ServerLevel level) {
        this.level = level;
    }

    public KnownNames knownNames() {
        return knownNames;
    }

    public class KnownNames {
        public void add(Player player) {
            PlayerInfoSavedData data = data();
            data.getNames().put(player.getScoreboardName().toLowerCase(), player.getUUID());
            data.setDirty();
        }

        public void remove(String name) {
            PlayerInfoSavedData data = data();
            if (data.getNames().remove(name.toLowerCase()) != null) {
                data.setDirty();
            }
        }

        public void clear() {
            PlayerInfoSavedData data = data();
            data.getNames().clear();
            data.setDirty();
        }

        public Map<String, UUID> getAll() {
            return Collections.unmodifiableMap(data().getNames());
        }

        public Optional<UUID> getUuid(String name) {
            return Optional.ofNullable(data().getNames().get(name.toLowerCase()));
        }
    }

    // --

    public DefaultAddress defaultAddress() {
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
