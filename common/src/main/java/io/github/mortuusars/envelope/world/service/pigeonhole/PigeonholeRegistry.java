package io.github.mortuusars.envelope.world.service.pigeonhole;

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

    public static PigeonholeRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), "envelope_pigeonholes");
    }

    public static @NotNull Factory<PigeonholeRegistry> factory() {
        return new Factory<>(
              PigeonholeRegistry::new,
              (tag, provider) -> CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag)
                    .resultOrPartial(e -> Envelope.LOGGER.error("Cannot load PigeonholeRegistry: {}", e))
                    .orElse(null),
              null);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return (CompoundTag) CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .resultOrPartial(e -> Envelope.LOGGER.error("Cannot save PigeonholeRegistry: {}", e))
              .orElse(tag);
    }
}