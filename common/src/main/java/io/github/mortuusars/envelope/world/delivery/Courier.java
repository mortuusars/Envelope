package io.github.mortuusars.envelope.world.delivery;

import java.util.Optional;

public interface Courier {
    Optional<Delivery> getCurrentDelivery();
    DeliveryHandler getDeliveryHandler();
    CourierOrigin getOrigin();

    default boolean isDelivering() {
        return getCurrentDelivery().isPresent();
    }
}
