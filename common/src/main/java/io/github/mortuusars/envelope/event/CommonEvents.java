package io.github.mortuusars.envelope.event;

import io.github.mortuusars.envelope.world.DeliveringPigeons;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class CommonEvents {
    public static void levelTick(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            DeliveringPigeons.get(serverLevel).tick(serverLevel);
        }
    }

    public static void entityLeaveLevel(Level level, Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Pigeon pigeon) {
            pigeon.unloaded(serverLevel);
        }
    }
}
