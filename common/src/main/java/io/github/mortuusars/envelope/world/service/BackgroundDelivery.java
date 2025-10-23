package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BackgroundDelivery extends SavedData {
    public static final Codec<BackgroundDelivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.list(BackgroundCourier.CODEC).fieldOf("couriers").forGetter(BackgroundDelivery::getCouriers)
    ).apply(instance, BackgroundDelivery::new));

    protected final List<BackgroundCourier> couriers;

    public BackgroundDelivery(List<BackgroundCourier> couriers) {
        this.couriers = new ArrayList<>(couriers);
    }

    public BackgroundDelivery() {
        this.couriers = new ArrayList<>();
    }

    public List<BackgroundCourier> getCouriers() {
        return couriers;
    }

    public void add(BackgroundCourier courier) {
        Preconditions.checkNotNull(courier.delivery(), "Courier must be delivering mail.");
        couriers.add(courier);
        setDirty();
    }

    public void tick(ServerLevel level) {
        couriers.removeIf(courier -> {
            @Nullable Delivery delivery = courier.delivery();
            if (delivery != null) {
                courier.tickDelivery(level, delivery);
            }
            setDirty();
            return courier.shouldBeRemoved();
        });
    }

    // -- Save / Load

    public static BackgroundDelivery get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), "envelope_background_delivery");
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.encode(this, registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot save BackgroundDelivery: {}", e.message()))
              .result()
              .filter(t -> t instanceof CompoundTag)
              .map(t -> ((CompoundTag) t))
              .orElse(tag);
    }

    private static BackgroundDelivery load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.decode(registries.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> Envelope.LOGGER.error("Cannot load BackgroundDelivery: {}", e.message()))
              .result()
              .map(Pair::getFirst)
              .orElseGet(BackgroundDelivery::new);
    }

    private static Factory<BackgroundDelivery> factory() {
        return new Factory<>(BackgroundDelivery::new, BackgroundDelivery::load, null);
    }
}