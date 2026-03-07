package io.github.mortuusars.envelope.world.mail.delivery.background;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

public class BackgroundDelivery extends SavedData {
    public static final Codec<BackgroundDelivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.list(BackgroundCourier.CODEC)
                .optionalFieldOf("couriers", Collections.emptyList())
                .forGetter(BackgroundDelivery::getActiveCouriers),
          Codec.list(FinishedBackgroundCourier.CODEC)
                .optionalFieldOf("finished_couriers", Collections.emptyList())
                .forGetter(BackgroundDelivery::getFinishedCouriers),
          Codec.list(SpawnableItem.CODEC)
                .optionalFieldOf("dropped_items", Collections.emptyList())
                .forGetter(BackgroundDelivery::getDroppedMail)
    ).apply(instance, BackgroundDelivery::new));
    public static final Logger LOGGER = LogUtils.getLogger();

    protected final List<BackgroundCourier> couriers;
    protected final List<BackgroundCourier> pendingCouriers = new ArrayList<>();
    protected final List<FinishedBackgroundCourier> finishedCouriers;
    protected final List<SpawnableItem> droppedItems;

    public BackgroundDelivery(List<BackgroundCourier> couriers, List<FinishedBackgroundCourier> finishedCouriers, List<SpawnableItem> droppedItems) {
        this.couriers = new ArrayList<>(couriers);
        this.finishedCouriers = new ArrayList<>(finishedCouriers);
        this.droppedItems = new ArrayList<>(droppedItems);
    }

    public BackgroundDelivery() {
        this(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public List<BackgroundCourier> getActiveCouriers() {
        return couriers;
    }

    public List<FinishedBackgroundCourier> getFinishedCouriers() {
        return finishedCouriers;
    }

    public List<SpawnableItem> getDroppedMail() {
        return droppedItems;
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

    public void addDroppedMail(SpawnableItem item) {
        droppedItems.add(item);
        setDirty();
    }

    public void removeDroppedMail(SpawnableItem item) {
        if (droppedItems.remove(item)) {
            setDirty();
        }
    }

    // --

    public void tick(ServerLevel level) {
        couriers.removeIf(courier -> {
            if (courier.isRemoved()) {
                setDirty();
                return true;
            }
            courier.tick(level);
            return false;
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