package io.github.mortuusars.envelope.world.service;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.BackgroundCourier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

public class BackgroundDelivery extends SavedData {
    public static final Codec<BackgroundDelivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.list(BackgroundCourier.CODEC)
                .optionalFieldOf("couriers", Collections.emptyList())
                .forGetter(BackgroundDelivery::getCouriers)
    ).apply(instance, BackgroundDelivery::new));
    private static final Logger LOGGER = LogUtils.getLogger();

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
        Preconditions.checkArgument(courier.isDelivering(), "Courier must be delivering mail.");
        couriers.add(courier);
        setDirty();
    }

    public void tick(ServerLevel level) {
        couriers.removeIf(courier -> {
            if (courier.canBeRemoved()) {
                if (Envelope.debug()) LOGGER.info("{} has been removed.", courier.getName().getString());
                return true;
            }
            courier.tick(level);
            return courier.trySpawn(level).isPresent();
        });

        setDirty();
    }

    // -- Save / Load

    public static BackgroundDelivery get(ServerLevel level, String name) {
        return level.getDataStorage().computeIfAbsent(factory(), name);
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