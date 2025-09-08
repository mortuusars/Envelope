package io.github.mortuusars.envelope.world;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.BackgroundPigeon;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DeliveringPigeons extends SavedData {
    protected static final String SAVED_DATA_NAME = "envelope_pigeons";

    public static final Codec<DeliveringPigeons> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(BackgroundPigeon.CODEC).fieldOf("pigeons").forGetter(DeliveringPigeons::getPigeons)
    ).apply(instance, DeliveringPigeons::new));

    protected List<BackgroundPigeon> pigeons;

    public DeliveringPigeons(List<BackgroundPigeon> pigeons) {
        this.pigeons = new ArrayList<>(pigeons);
    }

    public List<BackgroundPigeon> getPigeons() {
        return pigeons;
    }

    public void add(Pigeon pigeon) {
        Preconditions.checkNotNull(pigeon.getDelivery(), "Pigeon must be delivering mail.");
        CompoundTag tag = new CompoundTag();
        if (!pigeon.saveAsPassenger(tag)) { // Weird ass name for this method. It just saves an entity with its id, WITH PASSENGERS.
            Envelope.LOGGER.error("Failed to save delivering Pigeon to a tag.");
            return;
        }

        Pigeon.IGNORED_TAGS.forEach(tag::remove);

        pigeons.add(new BackgroundPigeon(tag, pigeon.getDelivery()));
        setDirty();
    }

    public void tick(ServerLevel level) {
        pigeons.removeIf(pigeon -> {
            setDirty();
            return pigeon.tickDelivery(level);
        });
    }

    // --

    public static DeliveringPigeons get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), SAVED_DATA_NAME);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encodeStart(NbtOps.INSTANCE, this)
                .ifError(e -> Envelope.LOGGER.error("Cannot save DeliveringPigeons: {}", e.message()))
                .result()
                .filter(t -> t instanceof CompoundTag)
                .map(t -> ((CompoundTag) t))
                .orElse(tag);
    }

    private static Factory<DeliveringPigeons> factory() {
        return new Factory<>(DeliveringPigeons::createEmpty, DeliveringPigeons::loadFromTag, null);
    }

    private static DeliveringPigeons createEmpty() {
        return new DeliveringPigeons(new ArrayList<>());
    }

    private static DeliveringPigeons loadFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot load DeliveringPigeons: {}", e.message()))
                .result().map(Pair::getFirst).orElse(createEmpty());
    }
}