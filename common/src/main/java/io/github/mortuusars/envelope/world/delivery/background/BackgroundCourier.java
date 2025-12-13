package io.github.mortuusars.envelope.world.delivery.background;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.delivery.*;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public record BackgroundCourier(SpawnableEntityData entityData, Delivery delivery) implements Courier, DeliveryHandler {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(BackgroundCourier::entityData),
          Delivery.CODEC.fieldOf("delivery").forGetter(BackgroundCourier::delivery)
    ).apply(instance, BackgroundCourier::new));

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
        if (delivery.getOrigin().isLocal()) {
            FinishedBackgroundCourier courier = new FinishedBackgroundCourier(
                  entityData(), delivery.getOrigin().getPos(), delivery.getMail().getItemForReading());
            level.getEnvelopeContext().getBackgroundDelivery().addFinishedCourier(courier);
        }
    }

    // --

    public void tick(ServerLevel level) {
        delivery().tick(level, this);
    }
}
