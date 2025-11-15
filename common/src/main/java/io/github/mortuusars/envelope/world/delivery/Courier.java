package io.github.mortuusars.envelope.world.delivery;

import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public interface Courier {
    Optional<Delivery> getDelivery();
    DeliveryHandler getDeliveryHandler();

    default void continueDelivery(ServerLevel level, Delivery delivery) {
        delivery.getProgress().update(getDeliveryHandler().getPhaseDuration(level, delivery, delivery.getCurrentPhase()));
    }

    default boolean isDelivering() {
        return getDelivery().isPresent();
    }
}
