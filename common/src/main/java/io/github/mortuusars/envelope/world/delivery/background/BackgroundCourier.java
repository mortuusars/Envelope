package io.github.mortuusars.envelope.world.delivery.background;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.delivery.*;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BackgroundCourier implements Courier, DeliveryHandler {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(BackgroundCourier::getEntityData),
          CourierOrigin.CODEC.fieldOf("origin").forGetter(BackgroundCourier::getOrigin),
          Delivery.CODEC.fieldOf("delivery").forGetter(BackgroundCourier::delivery)
    ).apply(instance, BackgroundCourier::new));

    protected final SpawnableEntityData entityData;
    protected final CourierOrigin origin;
    protected final Delivery delivery;

    public BackgroundCourier(SpawnableEntityData entityData, CourierOrigin origin, Delivery delivery) {
        this.entityData = entityData;
        this.delivery = delivery;
        this.origin = origin;
    }

    public SpawnableEntityData getEntityData() {
        return entityData;
    }

    public @NotNull Delivery delivery() {
        return delivery;
    }

    public CourierOrigin getOrigin() {
        return origin;
    }

    // -- Courier

    @Override
    public Component getName() {
        return Component.literal("Background Courier");
    }

    @Override
    public boolean isService() {
        return getOrigin().isService();
    }

    @Override
    public Optional<Delivery> getDelivery() {
        return Optional.ofNullable(delivery);
    }

    @Override
    public void continueDelivery(ServerLevel level, Delivery delivery) {
        delivery.adjust(level, this);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (getOrigin().isReal()) {
            var courier = new FinishedBackgroundCourier(getEntityData(), getOrigin().getHomePos(), delivery.getMail());
            level.getEnvelopeContext().getBackgroundDelivery().addFinishedCourier(courier);
        }
    }

    // --

    public boolean tick(ServerLevel level) {
        delivery().tick(level, this);
        return delivery().isFinished();
    }
}
