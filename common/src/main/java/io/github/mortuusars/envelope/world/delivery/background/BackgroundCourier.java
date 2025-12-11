package io.github.mortuusars.envelope.world.delivery.background;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.delivery.*;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BackgroundCourier implements Courier, DeliveryHandler {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(BackgroundCourier::getEntityData),
          Delivery.CODEC.fieldOf("delivery").forGetter(BackgroundCourier::delivery)
    ).apply(instance, BackgroundCourier::new));

    private final SpawnableEntityData entityData;
    private final Delivery delivery;

    public BackgroundCourier(SpawnableEntityData entityData, Delivery delivery) {
        this.entityData = entityData;
        this.delivery = delivery;
    }

    public SpawnableEntityData getEntityData() {
        return entityData;
    }

    public @NotNull Delivery delivery() {
        return delivery;
    }

    // -- Courier

    @Override
    public Optional<Delivery> getDelivery() {
        return Optional.ofNullable(delivery);
    }

    @Override
    public DeliveryHandler getDeliveryHandler() {
        return this;
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (delivery.getOrigin().isReal()) {
            FinishedBackgroundCourier courier = new FinishedBackgroundCourier(
                  getEntityData(), delivery.getOrigin().getStartPos(), delivery.getMail().getItemForReading());
            level.getEnvelopeContext().getBackgroundDelivery().addFinishedCourier(courier);
        }
    }

    // --

    public boolean tick(ServerLevel level) {
        delivery().tick(level, this);
        return delivery().isFinished();
    }
}
