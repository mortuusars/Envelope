package io.github.mortuusars.envelope.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;

public class PigeonFlyingPathNavigation extends FlyingPathNavigation {
    public PigeonFlyingPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        // By default, only positions that have solid block underneath them are valid.
        // But pigeons can fly to any position.
        // Without this, pigeons only wander near the ground, and basically never fly higher, unless there's blocks there.
        return true;
    }
}
