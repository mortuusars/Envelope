package io.github.mortuusars.envelope.event;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.service.BackgroundDelivery;
import io.github.mortuusars.envelope.world.block.dispenser.PackageDispenseItemBehavior;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class CommonEvents {
    public static void commonSetup() {
        DispenserBlock.registerBehavior(Envelope.Items.PACKAGE.get(), new PackageDispenseItemBehavior());
    }

    public static void levelTick(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getEnvelopeContext().getBackgroundDelivery().tick(serverLevel);
        }
    }

    /**
     * Fired before level data storage is saved.
     */
    public static void saveLevelData(ServerLevel level) {
    }

    public static void entityLeaveLevel(Level level, Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Pigeon pigeon) {
            pigeon.unloaded(serverLevel);
        }
    }
}
