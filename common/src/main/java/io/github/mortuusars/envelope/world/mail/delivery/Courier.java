package io.github.mortuusars.envelope.world.mail.delivery;

import java.util.Optional;

public interface Courier {
    Optional<Delivery> getCurrentDelivery();
    DeliveryExecutor getDeliveryExecutor();
    CourierOrigin getOrigin();

    default boolean isDelivering() {
        return getCurrentDelivery().isPresent();
    }
}
