package io.github.mortuusars.envelope.world.mail;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.PersistentData;
import io.github.mortuusars.mortaar.Platform;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CustomMailSavedData extends SavedData {
    public static final Codec<CustomMailSavedData> CODEC = CompoundTag.CODEC.xmap(CustomMailSavedData::new, CustomMailSavedData::getData);
    public static final Logger LOGGER = LogUtils.getLogger();

    protected final Map<PersistentData.Type<?>, PersistentData> typedData = new HashMap<>();
    protected final CompoundTag data;

    public CustomMailSavedData(CompoundTag data) {
        this.data = data;
    }

    public CustomMailSavedData() {
        this.data = new CompoundTag();
    }

    public CompoundTag getData() {
        return data;
    }

    /**
     * Gets the tag by ID.<br>
     * Don't forget to call {@link #setDirty()} or {@link #set(ResourceLocation, CompoundTag)} to ensure that changes will be saved.
     * @param location ID of the data.
     */
    public CompoundTag get(ResourceLocation location) {
        return getData().getCompound(location.toString());
    }

    public void set(ResourceLocation location, CompoundTag data) {
        getData().put(location.toString(), data);
        setDirty();
    }

    @SuppressWarnings("unchecked")
    public <T extends PersistentData> T getOrCreateTyped(PersistentData.Type<T> type) {
        return (T) typedData.computeIfAbsent(type, t -> {
            String key = t.id().toString();
            if (data.contains(key)) {
                RegistryAccess registryAccess = Platform.getCurrentServerOrThrow().registryAccess();
                return t.codec().decode(registryAccess.createSerializationContext(NbtOps.INSTANCE), data.getCompound(key))
                      .ifError(e -> LOGGER.error("Cannot decode typed data '{}': {}", t, e))
                      .result()
                      .map(p -> (PersistentData) p.getFirst())
                      .orElse(t.constructor().get());
            }

            return t.constructor().get();
        });
    }

    // -- Save / Load

    private static final Factory<CustomMailSavedData> FACTORY =
          new Factory<>(CustomMailSavedData::new, CustomMailSavedData::load, null);

    public static CustomMailSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "envelope_mail_data");
    }

    @Override
    public boolean isDirty() {
        return super.isDirty() || typedData.values().stream().anyMatch(PersistentData::isDirty);
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        for (Map.Entry<PersistentData.Type<?>, PersistentData> typedData : typedData.entrySet()) {
            if (typedData.getValue().isDirty()) {
                try {
                    data.put(typedData.getKey().id().toString(), encodeTypedData(typedData.getKey(), typedData.getValue(), ops));
                    typedData.getValue().setDirty(false);
                } catch (Exception e) {
                    LOGGER.error("Cannot encode typed data '{}':", typedData.getKey(), e);
                }
            }
        }

        return CODEC.encode(this, NbtOps.INSTANCE, tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot save CustomMailSavedData: {}", e.message()))
              .result()
              .filter(t -> t instanceof CompoundTag)
              .map(t -> ((CompoundTag) t))
              .orElse(tag);
    }

    @SuppressWarnings("unchecked")
    private <T extends PersistentData> Tag encodeTypedData(PersistentData.Type<T> type, PersistentData data, RegistryOps<Tag> ops) {
        return type.codec().encodeStart(ops, (T)data).getOrThrow();
    }

    private static CustomMailSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot load CustomMailSavedData: {}", e.message()))
              .result().map(Pair::getFirst).orElseGet(CustomMailSavedData::new);
    }
}
