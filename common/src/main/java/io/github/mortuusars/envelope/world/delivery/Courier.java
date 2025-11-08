package io.github.mortuusars.envelope.world.delivery;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public interface Courier {
    Component getName();
    boolean isService();

    Optional<Delivery> getDelivery();

    void continueDelivery(ServerLevel level, Delivery delivery);

    default boolean isDelivering() {
        return getDelivery().isPresent();
    }
}
