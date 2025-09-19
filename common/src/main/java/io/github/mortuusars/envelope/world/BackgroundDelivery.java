package io.github.mortuusars.envelope.world;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.BackgroundCourier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BackgroundDelivery extends SavedData {
    protected static final String SAVED_DATA_NAME = "envelope_background_delivery";

    public static final Codec<BackgroundDelivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(BackgroundCourier.CODEC).fieldOf("couriers").forGetter(BackgroundDelivery::getCouriers)
    ).apply(instance, BackgroundDelivery::new));

    protected List<BackgroundCourier> couriers;

    public BackgroundDelivery(List<BackgroundCourier> couriers) {
        this.couriers = new ArrayList<>(couriers);
    }

    public List<BackgroundCourier> getCouriers() {
        return couriers;
    }

    public void add(BackgroundCourier courier) {
        Preconditions.checkNotNull(courier.getDelivery(), "Courier must be delivering mail.");
        couriers.add(courier);
        setDirty();
    }

    public void tick(ServerLevel level) {
        couriers.removeIf(pigeon -> {
            pigeon.tickDelivery(level);
            setDirty();
            return pigeon.shouldBeRemoved();
        });
    }

    // -- Save / Load

    public static BackgroundDelivery get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), SAVED_DATA_NAME);
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encodeStart(NbtOps.INSTANCE, this)
                .ifError(e -> Envelope.LOGGER.error("Cannot save BackgroundDelivery: {}", e.message()))
                .result()
                .filter(t -> t instanceof CompoundTag)
                .map(t -> ((CompoundTag) t))
                .orElse(tag);
    }

    private static Factory<BackgroundDelivery> factory() {
        return new Factory<>(BackgroundDelivery::createEmpty, BackgroundDelivery::loadFromTag, null);
    }

    private static BackgroundDelivery createEmpty() {
        return new BackgroundDelivery(new ArrayList<>());
    }

    private static BackgroundDelivery loadFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(NbtOps.INSTANCE, tag)
                .ifError(e -> Envelope.LOGGER.error("Cannot load BackgroundDelivery: {}", e.message()))
                .result().map(Pair::getFirst).orElse(createEmpty());
    }
}