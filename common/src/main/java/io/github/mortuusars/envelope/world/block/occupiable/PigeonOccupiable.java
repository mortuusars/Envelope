package io.github.mortuusars.envelope.world.block.occupiable;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

public interface PigeonOccupiable extends Occupiable {
    @Override
    default boolean canBeOccupiedBy(Entity entity) {
        return entity.getType().is(Envelope.Tags.EntityTypes.PIGEONHOLE_INHABITORS);
    }

    @Override
    default SoundEvent getOccupantEnterSound(Entity entity) {
        return Envelope.SoundEvents.PIGEONHOLE_ENTER.get();
    }

    @Override
    default SoundEvent getOccupantExitSound(Entity entity) {
        return Envelope.SoundEvents.PIGEONHOLE_EXIT.get();
    }

    @Override
    default SoundEvent getOccupantWorkSound() {
        return getOccupants().size() > 1
              ? Envelope.SoundEvents.PIGEONHOLE_WORK_MULTIPLE.get()
              : Envelope.SoundEvents.PIGEONHOLE_WORK.get();
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
