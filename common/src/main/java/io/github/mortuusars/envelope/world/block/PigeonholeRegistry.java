package io.github.mortuusars.envelope.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry of pigeonhole block positions for moving-structure-aware target discovery.
 * Stores local identity positions; spatial checks must project before comparing distances.
 */
public final class PigeonholeRegistry {
    private static final WeakHashMap<ServerLevel, Set<BlockPos>> REGISTRY = new WeakHashMap<>();

    private PigeonholeRegistry() {
    }

    public static void register(ServerLevel level, BlockPos pos) {
        getOrCreate(level).add(pos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        getOrCreate(level).remove(pos);
    }

    public static Set<BlockPos> getAll(ServerLevel level) {
        return Collections.unmodifiableSet(getOrCreate(level));
    }

    private static Set<BlockPos> getOrCreate(ServerLevel level) {
        return REGISTRY.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet());
    }
}
