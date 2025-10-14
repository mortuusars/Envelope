package io.github.mortuusars.envelope.world.block.occupiable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Function;

public class Occupant {
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

    protected final CustomData entityData;
    protected final int slot;
    protected final int minTicksInside;
    protected int ticksInside;

    public Occupant(CustomData entityData, int slot, int minTicksInside, int ticksInside) {
        this.entityData = entityData;
        this.slot = slot;
        this.minTicksInside = minTicksInside;
        this.ticksInside = ticksInside;
    }

//        public static Occupant of(Entity entity, int slot, int minTicksInside) {
//            CompoundTag tag = new CompoundTag();
//            entity.save(tag);
//            Pigeon.IGNORED_TAGS.forEach(tag::remove);
//            return new Occupant(CustomData.of(tag), slot, minTicksInside, 0);
//        }
//
//        public static Occupant of(Entity entity, int slot) {
//            return of(entity, slot, 100);
//        }
//
//        public static Occupant create(int slot, int ticksInside) {
//            CompoundTag compoundTag = new CompoundTag();
//            compoundTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(Envelope.EntityTypes.PIGEON.get()).toString());
//            return new Occupant(CustomData.of(compoundTag), slot, ticksInside, 600);
//        }
//
//        @Nullable
//        public Entity createEntity(Level level, BlockPos pos) {
//            CompoundTag compoundTag = entityData.copyTag();
//            Pigeon.IGNORED_TAGS.forEach(compoundTag::remove);
//            Entity entity = EntityType.loadEntityRecursive(compoundTag, level, Function.identity());
//            if (entity == null || !entity.getType().is(Envelope.Tags.EntityTypes.PIGEONHOLE_INHABITORS)) {
//                return null;
//            }
//
//            entity.setNoGravity(true);
//
//            if (entity instanceof Pigeon pigeon) {
//                pigeon.getPigeonholeHandler().setPigeonholePos(pos);
//                setPigeonReleaseData(ticksInside, pigeon);
//            }
//
//            return entity;
//        }

//        private void setPigeonReleaseData(int ticksInside, Pigeon pigeon) {
//            int i = pigeon.getAge();
//            if (i < 0) {
//                pigeon.setAge(Math.min(0, i + ticksInside));
//            } else if (i > 0) {
//                pigeon.setAge(Math.max(0, i - ticksInside));
//            }
//
//            pigeon.setInLoveTime(Math.max(0, pigeon.getInLoveTime() - ticksInside));
//        }

    public CustomData entityData() {
        return entityData;
    }

    public int slot() {
        return slot;
    }

    public int minTicksInside() {
        return minTicksInside;
    }

    public int ticksInside() {
        return ticksInside;
    }

    // --

    public boolean tick() {
        return this.ticksInside++ >= minTicksInside;
    }

    public Entity createEntity(Level level, BlockPos zero) {
        return EntityType.loadEntityRecursive(entityData.copyTag(), level, Function.identity());
    }
}
