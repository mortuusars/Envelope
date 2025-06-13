package io.github.mortuusars.envelope.world;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KnownPlayers extends SavedData {
    private final Map<String, UUID> players = new HashMap<>();

    public Map<String, UUID> get() {
        return Collections.unmodifiableMap(players);
    }

    public void add(Player player) {
        players.put(player.getScoreboardName().toLowerCase(), player.getUUID());
        setDirty();
    }

    // --

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<String, UUID> entry : players.entrySet()) {
            tag.putUUID(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        players.clear();
        for (String key : tag.getAllKeys()) {
            try {
                UUID uuid = tag.getUUID(key);
                players.put(key, uuid);
            } catch (Exception e) {
                Envelope.LOGGER.error("Cannot load known player: {}", e.getMessage());
            }
        }
    }

    public static @NotNull KnownPlayers loadOrCreate(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(KnownPlayers.factory(), "horseman_horse_calling");
    }

    private static Factory<KnownPlayers> factory() {
        return new Factory<>(KnownPlayers::new,
                (tag, provider) -> {
            KnownPlayers instance = new KnownPlayers();
            instance.load(tag, provider);
            return instance;
        }, null);
    }
}
