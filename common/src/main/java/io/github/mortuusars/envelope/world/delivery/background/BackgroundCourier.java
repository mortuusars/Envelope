package io.github.mortuusars.envelope.world.delivery.background;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.delivery.*;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class BackgroundCourier implements Courier, DeliveryHandler {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(BackgroundCourier::getEntityData),
          CourierOrigin.CODEC.optionalFieldOf("origin", CourierOrigin.service()).forGetter(BackgroundCourier::getOrigin),
          Delivery.CODEC.fieldOf("delivery").forGetter(BackgroundCourier::getDelivery)
    ).apply(instance, BackgroundCourier::new));

    private final SpawnableEntityData entityData;
    private final Delivery delivery;
    private final CourierOrigin origin;

    public BackgroundCourier(SpawnableEntityData entityData, CourierOrigin origin, Delivery delivery) {
        this.entityData = entityData;
        this.delivery = delivery;
        this.origin = origin;
    }

    public SpawnableEntityData getEntityData() {
        return entityData;
    }

    @Override
    public CourierOrigin getOrigin() {
        return origin;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    @Override
    public Optional<Delivery> getCurrentDelivery() {
        return Optional.ofNullable(delivery);
    }

    @Override
    public DeliveryHandler getDeliveryHandler() {
        return this;
    }

    // --

    public void tick(ServerLevel level) {
        tickDelivery(level, getDelivery());
    }
}
