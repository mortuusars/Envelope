package io.github.mortuusars.envelope.world.block.occupiable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public record Occupant(CustomData entityData, int slot, int minTicksInside, int ticksInside) {
    public static final Codec<Occupant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CustomData.CODEC.optionalFieldOf("entity_data", CustomData.EMPTY).forGetter(Occupant::entityData),
                Codec.INT.fieldOf("slot").forGetter(Occupant::slot),
                Codec.INT.fieldOf("min_ticks_inside").forGetter(Occupant::minTicksInside),
                Codec.INT.optionalFieldOf("ticks_inside", 0).forGetter(Occupant::ticksInside))
          .apply(instance, Occupant::new)
    );

    public static final Codec<List<Occupant>> LIST_CODEC = CODEC.listOf();

    @SuppressWarnings("deprecation")
    public static final StreamCodec<ByteBuf, Occupant> STREAM_CODEC = StreamCodec.composite(
          CustomData.STREAM_CODEC, Occupant::entityData,
          ByteBufCodecs.VAR_INT, Occupant::slot,
          ByteBufCodecs.VAR_INT, Occupant::minTicksInside,
          ByteBufCodecs.VAR_INT, Occupant::ticksInside,
          Occupant::new
    );

    public Mutable toMutable() {
        return new Mutable(this);
    }

    public static class Mutable {
        protected final Occupant occupant;
        protected int ticksInside;

        public Mutable(Occupant occupant) {
            this.occupant = occupant;
            this.ticksInside = occupant.ticksInside();
        }

        public Occupant getOccupant() {
            return occupant;
        }

        public int getTicksInside() {
            return ticksInside;
        }

        public boolean tick() {
            return this.ticksInside++ >= occupant.minTicksInside();
        }

        public Occupant toImmutable() {
            return new Occupant(occupant.entityData(), occupant.slot(), occupant.minTicksInside(), getTicksInside());
        }
    }
}