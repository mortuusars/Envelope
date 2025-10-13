package io.github.mortuusars.envelope.util;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.NotNull;

/**
 * Copy of forge's sound type, for use with Suppliers.
 */
public class DeferredSoundType extends SoundType {
    private final Supplier<SoundEvent> breakSound;
    private final Supplier<SoundEvent> stepSound;
    private final Supplier<SoundEvent> placeSound;
    private final Supplier<SoundEvent> hitSound;
    private final Supplier<SoundEvent> fallSound;

    public DeferredSoundType(float volume, float pitch, Supplier<SoundEvent> breakSound, Supplier<SoundEvent> stepSound, Supplier<SoundEvent> placeSound, Supplier<SoundEvent> hitSound, Supplier<SoundEvent> fallSound) {
        // Pass defaults instead of null, in case that nulls might break something.
        super(volume, pitch, SoundEvents.STONE_PLACE, SoundEvents.STONE_PLACE, SoundEvents.STONE_PLACE, SoundEvents.STONE_PLACE, SoundEvents.STONE_PLACE);
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.placeSound = placeSound;
        this.hitSound = hitSound;
        this.fallSound = fallSound;
    }

    @Override
    public @NotNull SoundEvent getBreakSound() {
        return breakSound.get();
    }

    @Override
    public @NotNull SoundEvent getStepSound() {
        return stepSound.get();
    }

    @Override
    public @NotNull SoundEvent getPlaceSound() {
        return placeSound.get();
    }

    @Override
    public @NotNull SoundEvent getHitSound() {
        return hitSound.get();
    }

    @Override
    public @NotNull SoundEvent getFallSound() {
        return fallSound.get();
    }
}
