package io.github.mortuusars.envelope.world.service.pigeonhole;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PigeonholeRegistry extends SavedData {
    public static final Codec<PigeonholeRegistry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.unboundedMap(Address.Block.STRING_CODEC, PigeonholeData.CODEC)
                .optionalFieldOf("pigeonholes", Collections.emptyMap()).forGetter(PigeonholeRegistry::getPigeonholes)
    ).apply(instance, PigeonholeRegistry::new));

    private final HashMap<Address.Block, PigeonholeData> pigeonholes;

    protected PigeonholeRegistry(Map<Address.Block, PigeonholeData> pigeonholes) {
        this.pigeonholes = new HashMap<>(pigeonholes);
    }

    protected PigeonholeRegistry() {
        this(new HashMap<>());
    }

    public HashMap<Address.Block, PigeonholeData> getPigeonholes() {
        return pigeonholes;
    }

    // -- Save / Load

    public static PigeonholeRegistry get(ServerLevel level, String name) {
        return level.getDataStorage().computeIfAbsent(factory(), name);
    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || getPigeonholes().values().stream().anyMatch(PigeonholeData::isDirty);
    }

    @Override
    public void setDirty(boolean dirty) {
        super.setDirty(dirty);
        if (!dirty) {
            getPigeonholes().values().forEach(data -> data.setDirty(false));
        }
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot save PigeonholeSavedData: {}", e.message()))
              .result()
              .filter(t -> t instanceof CompoundTag)
              .map(t -> ((CompoundTag) t))
              .orElse(tag);
    }

    private static PigeonholeRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot load PigeonholeSavedData: {}", e.message()))
              .result()
              .map(Pair::getFirst)
              .orElseGet(PigeonholeRegistry::new);
    }

    private static Factory<PigeonholeRegistry> factory() {
        return new Factory<>(PigeonholeRegistry::new, PigeonholeRegistry::load, null);
    }
}