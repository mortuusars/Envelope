package io.github.mortuusars.envelope.event;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.dispenser.PlaceBlockDispenseItemBehavior;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class CommonEvents {
    public static void commonSetup() {
        MailService.init();

        PlaceBlockDispenseItemBehavior placeBlockBehavior = new PlaceBlockDispenseItemBehavior();
        DispenserBlock.registerBehavior(Envelope.Items.PACKAGE.get(), placeBlockBehavior);
        DispenserBlock.registerBehavior(Envelope.Items.SEALED_PACKAGE.get(), placeBlockBehavior);
    }

    public static void levelTick(Level level) {
    }

    /**
     * Fired before level data storage is saved.
     */
    public static void saveLevelData(ServerLevel level) {
    }

    public static void entityLeaveLevel(Level level, Entity entity) {
    }
}
