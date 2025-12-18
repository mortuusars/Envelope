package io.github.mortuusars.envelope.world.delivery.background;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.delivery.*;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public record BackgroundCourier(SpawnableEntityData entityData, Delivery delivery, CourierOrigin origin) implements Courier, DeliveryHandler {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(BackgroundCourier::entityData),
          Delivery.CODEC.fieldOf("delivery").forGetter(BackgroundCourier::delivery),
          CourierOrigin.CODEC.optionalFieldOf("origin", CourierOrigin.service()).forGetter(BackgroundCourier::origin)
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
    public CourierOrigin getOrigin() {
        return origin;
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (origin.isRegular()) {
            FinishedBackgroundCourier courier = new FinishedBackgroundCourier(
                  entityData(), origin.getPos(), delivery.getMail().getItemForReading());
            MailService.of(level).getBackgroundDelivery().addFinishedCourier(courier);
        }
    }

    // --

    public void tick(ServerLevel level) {
        delivery().tick(level, this);
    }
}
