package io.github.mortuusars.envelope.world.entity.spawner;

import net.minecraft.server.level.ServerLevel;

public interface Spawner {
    void tick(ServerLevel level);
}
