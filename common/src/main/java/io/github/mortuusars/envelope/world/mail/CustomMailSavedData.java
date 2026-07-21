package io.github.mortuusars.envelope.world.mail;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class CustomMailSavedData extends SavedData {
    public static final Codec<CustomMailSavedData> CODEC = CompoundTag.CODEC.xmap(CustomMailSavedData::new, CustomMailSavedData::getData);

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

    // -- Save / Load

    private static final Factory<CustomMailSavedData> FACTORY =
          new Factory<>(CustomMailSavedData::new, CustomMailSavedData::load, null);

    public static CustomMailSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "envelope_mail_data");
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, NbtOps.INSTANCE, tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot save CustomMailSavedData: {}", e.message()))
              .result()
              .filter(t -> t instanceof CompoundTag)
              .map(t -> ((CompoundTag) t))
              .orElse(tag);
    }

    private static CustomMailSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot load CustomMailSavedData: {}", e.message()))
              .result().map(Pair::getFirst).orElseGet(CustomMailSavedData::new);
    }
}
