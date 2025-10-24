package io.github.mortuusars.envelope.world.delivery;

import net.minecraft.server.level.ServerLevel;

public interface RealCourier extends Courier {
    void onCourierSpawned(ServerLevel level);
    void onCourierDespawned(ServerLevel level);
}
