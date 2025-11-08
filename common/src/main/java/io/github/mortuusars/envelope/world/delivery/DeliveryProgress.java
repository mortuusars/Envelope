package io.github.mortuusars.envelope.world.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import net.minecraft.util.Mth;

public class DeliveryProgress {
    public static final Codec<DeliveryProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
          DeliveryPhase.CODEC.fieldOf("phase").forGetter(DeliveryProgress::getPhase),
          Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("duration", 0).forGetter(DeliveryProgress::getDuration),
          Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("progress", 0).forGetter(DeliveryProgress::getTicks)
    ).apply(i, DeliveryProgress::new));

    private DeliveryPhase phase;
    private int duration;
    private int ticks;

    public DeliveryProgress(DeliveryPhase phase, int duration, int ticks) {
        this.phase = phase;
        this.duration = duration;
        this.ticks = ticks;
    }

    public static DeliveryProgress start() {
        return new DeliveryProgress(DeliveryPhase.STARTED, 0, 0);
    }

    public DeliveryPhase getPhase() {
        return phase;
    }

    public int getDuration() {
        return duration;
    }

    public int getTicks() {
        return ticks;
    }

    // --

    public double getCompleteness() {
        if (duration <= 0) {
            return 1.0;
        }
        return Mth.clamp((double) ticks / duration, 0.0, 1.0);
    }

    public boolean isDone() {
        return ticks >= duration;
    }

    // --

    public void tick() {
        ticks++;
    }

    public void complete() {
        ticks = duration;
    }

    public void advance(DeliveryPhase nextPhase, int duration) {
        this.phase = nextPhase;
        this.duration = Math.max(0, duration);
        this.ticks = 0;
    }

    public void adjustForChangedDuration(int duration) {
        if (this.duration != duration) {
            double completeness = getCompleteness();
            this.duration = duration;
            this.ticks = Mth.clamp((int)(duration * completeness), 0, duration);
        }
    }
}
