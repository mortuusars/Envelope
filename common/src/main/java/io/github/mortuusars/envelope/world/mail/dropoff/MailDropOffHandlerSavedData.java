package io.github.mortuusars.envelope.world.mail.dropoff;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class MailDropOffHandlerSavedData extends SavedData {
    public static final Codec<MailDropOffHandlerSavedData> CODEC = CompoundTag.CODEC.xmap(MailDropOffHandlerSavedData::new, MailDropOffHandlerSavedData::getData);

    protected final CompoundTag data;

    public MailDropOffHandlerSavedData(CompoundTag data) {
        this.data = data;
    }

    public MailDropOffHandlerSavedData() {
        this.data = new CompoundTag();
    }

    public CompoundTag getData() {
        return data;
    }

    public CompoundTag get(ResourceLocation location) {
        return getData().getCompound(location.toString());
    }

    public void set(ResourceLocation location, CompoundTag data) {
        getData().put(location.toString(), data);
        setDirty();
    }

    // -- Save / Load

    public static MailDropOffHandlerSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory(), "envelope_mail_dropoff_handlers");
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot save MailDropOffHandlerSavedData: {}", e.message()))
                .result()
                .filter(t -> t instanceof CompoundTag)
                .map(t -> ((CompoundTag) t))
                .orElse(tag);
    }

    private static MailDropOffHandlerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot load MailDropOffHandlerSavedData: {}", e.message()))
                .result().map(Pair::getFirst).orElseGet(MailDropOffHandlerSavedData::new);
    }

    private static Factory<MailDropOffHandlerSavedData> factory() {
        return new Factory<>(MailDropOffHandlerSavedData::new, MailDropOffHandlerSavedData::load, null);
    }
}
