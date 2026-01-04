package io.github.mortuusars.envelope.world.delivery.background;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BackgroundDelivery extends SavedData {
    public static final Codec<BackgroundDelivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.list(BackgroundCourier.CODEC)
                .optionalFieldOf("couriers", Collections.emptyList())
                .forGetter(BackgroundDelivery::getCouriers),
          Codec.list(FinishedBackgroundCourier.CODEC)
                .optionalFieldOf("finished_couriers", Collections.emptyList())
                .forGetter(BackgroundDelivery::getFinishedCouriers)
    ).apply(instance, BackgroundDelivery::new));

    protected final List<BackgroundCourier> couriers;
    protected final List<BackgroundCourier> pendingCouriers = new ArrayList<>();
    protected final List<FinishedBackgroundCourier> finishedCouriers;

    public BackgroundDelivery(List<BackgroundCourier> couriers, List<FinishedBackgroundCourier> finishedCouriers) {
        this.couriers = new ArrayList<>(couriers);
        this.finishedCouriers = new ArrayList<>(finishedCouriers);
    }

    public BackgroundDelivery() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public List<BackgroundCourier> getCouriers() {
        return couriers;
    }

    public List<FinishedBackgroundCourier> getFinishedCouriers() {
        return finishedCouriers;
    }

    public void addCourier(BackgroundCourier courier) {
        pendingCouriers.add(courier);
        setDirty();
    }

    public void removeCourier(BackgroundCourier courier) {
        if (couriers.remove(courier)) {
            setDirty();
        }
    }

    public void addFinishedCourier(FinishedBackgroundCourier courier) {
        finishedCouriers.add(courier);
        setDirty();
    }

    public void removeFinishedCourier(FinishedBackgroundCourier courier) {
        if (finishedCouriers.remove(courier)) {
            setDirty();
        }
    }

    public void tick(ServerLevel level) {
        couriers.removeIf(courier -> {
            courier.tick(level);

            boolean ended = courier.getDelivery().isEnded();
            if (ended && courier.getOrigin().isRegular()) {
                FinishedBackgroundCourier finishedCourier = new FinishedBackgroundCourier(
                      courier.getEntityData(), courier.getOrigin().getPos(), courier.getDelivery().getMail().getItem());
                addFinishedCourier(finishedCourier);
            }

            return ended;
        });
        processPendingCouriers();
    }

    protected void processPendingCouriers() {
        // Using buffer for new couriers, to avoid ConcurrentModification when courier addition is caused by courier tick
        couriers.addAll(pendingCouriers);
        pendingCouriers.clear();
    }

    // -- Save / Load

    public static BackgroundDelivery get(ServerLevel level, String name) {
        return level.getDataStorage().computeIfAbsent(factory(), name);
    }

    @Override
    public boolean isDirty() {
        return !couriers.isEmpty() || super.isDirty();
    }

    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        processPendingCouriers();
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