package io.github.mortuusars.envelope.world.block.occupiable;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

public interface PigeonOccupiable extends Occupiable {
    @Override
    default boolean canBeOccupiedBy(Entity entity) {
        return entity instanceof Pigeon;
    }

    @Override
    default SoundEvent getOccupantEnterSound(Entity entity) {
        return SoundEvents.BEEHIVE_ENTER;
    }

    @Override
    default SoundEvent getOccupantExitSound(Entity entity) {
        return SoundEvents.BEEHIVE_EXIT;
    }

    @Override
    default SoundEvent getOccupantWorkSound() {
        return Envelope.SoundEvents.PIGEON_AMBIENT.get();
    }

    @Override
    default int getMinimumTicksInsideForOccupant(Entity entity) {
        return Config.Server.PIGEON_MIN_TICKS_INSIDE_PIGEONHOLE.get();
    }

    @Override
    default String getSerializedOccupantsName() {
        return "pigeons";
    }

    @Override
    default void cleanupOccupantEntityTag(CompoundTag tag) {
        Pigeon.IGNORED_TAGS.forEach(tag::remove);
    }
}
