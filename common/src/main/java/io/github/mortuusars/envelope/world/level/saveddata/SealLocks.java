package io.github.mortuusars.envelope.world.level.saveddata;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.clientbound.ClientboundSyncDeathLockDataPacket;
import io.github.mortuusars.envelope.world.item.component.SealLock;
import io.github.mortuusars.mortaar.Platform;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SealLocks extends SavedData {
    public static final Codec<SealLocks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.list(SealLock.CODEC).xmap(Set::copyOf, List::copyOf).optionalFieldOf("locks", Set.of()).forGetter(SealLocks::getLocks)
    ).apply(instance, SealLocks::new));

    private static @Nullable SealLocks clientInstance;

    private final Set<SealLock> locks;

    protected SealLocks(Collection<SealLock> locks) {
        this.locks = new HashSet<>(locks); // Make sure it's mutable
    }

    protected SealLocks() {
        this.locks = new HashSet<>();
    }

    public Set<SealLock> getLocks() {
        return locks;
    }

    public boolean isLocked(SealLock lock) {
        return locks.contains(lock);
    }

    public boolean lock(SealLock lock) {
        if (locks.add(lock)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean unlock(SealLock lock) {
        if (locks.remove(lock)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean unlockAllFrom(String owner) {
        if (locks.removeIf(lock -> lock.owner().equalsIgnoreCase(owner))) {
            setDirty();
            return true;
        }
        return false;
    }

    // --

    @Override
    public void setDirty(boolean dirty) {
        super.setDirty(dirty);
        if (dirty) {
            syncToAllClients();
        }
    }

    public void syncToAllClients() {
        if (Platform.getCurrentServer() != null) {
            new ClientboundSyncDeathLockDataPacket(List.copyOf(locks)).sendToAllClients();
        }
    }

    public void syncToClient(ServerPlayer player) {
        new ClientboundSyncDeathLockDataPacket(List.copyOf(locks)).sendToClient(player);
    }

    // -- Save / Load

    public static SealLocks get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(factory(), "envelope_seal_locks");
        } else {
            if (clientInstance == null) {
                clientInstance = new SealLocks();
            }
            return clientInstance;
        }
    }

    public static void setClientData(Level level, List<SealLock> locks) {
        Preconditions.checkArgument(level.isClientSide(), "setClientData called on the server! Don't do that.");
        SealLocks data = get(level);
        data.locks.clear();
        data.locks.addAll(locks);
    }

    public static @NotNull Factory<SealLocks> factory() {
        return new Factory<>(
              SealLocks::new,
              (tag, provider) -> CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(e -> Envelope.LOGGER.error("Cannot load DeathLockSavedData: {}", e))
                    .orElse(null),
              null);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .resultOrPartial(e -> Envelope.LOGGER.error("Cannot save DeathLockSavedData: {}", e))
              .orElse(tag);
    }
}
