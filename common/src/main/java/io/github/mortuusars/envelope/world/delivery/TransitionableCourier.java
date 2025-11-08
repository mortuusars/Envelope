package io.github.mortuusars.envelope.world.delivery;

import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import net.minecraft.server.level.ServerLevel;

public interface TransitionableCourier extends Courier {
    boolean isService();
    void setService(boolean service);
    BackgroundCourier toBackgroundCourier();
    void onAppeared(ServerLevel level);
    void onVanished(ServerLevel level);
}
