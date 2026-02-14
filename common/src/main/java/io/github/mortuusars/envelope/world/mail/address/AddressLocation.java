package io.github.mortuusars.envelope.world.mail.address;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.mail.delivery.TravelDuration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface AddressLocation {
    int ASCEND_DISTANCE = 16;

    Codec<AddressLocation> CODEC = Type.CODEC.dispatch(AddressLocation::getType, Type::getCodec);

    Type getType();

    Optional<BlockPos> getPosition();

    int getDistanceTo(BlockPos pos);

    default int getDistanceTo(Optional<BlockPos> pos) {
        return pos.map(this::getDistanceTo).orElse(Config.Server.DELIVERY_DEFAULT_DISTANCE.get());
    }

    default TravelDuration getTravelDurationTo(BlockPos pos) {
        return TravelDuration.basedOnDistance(getDistanceTo(pos));
    }

    default TravelDuration getTravelDurationTo(Optional<BlockPos> pos) {
        return TravelDuration.basedOnDistance(getDistanceTo(pos));
    }

    default Optional<BlockPos> getNearestHub() {
        return getPosition().map(pos -> Position.snapToGrid(pos, 1024).atY(320));
    }

    default Optional<BlockPos> ascendTowards(Level level, Optional<BlockPos> targetPos) {
        return getPosition().map(pos -> targetPos
                    .map(blockPos -> Position.ascendTowards(pos, blockPos, ASCEND_DISTANCE))
                    .orElseGet(() -> Position.towardsRandomHorizontalDirection(pos, ASCEND_DISTANCE, hashCode())))
              .map(pos -> Position.aboveGround(level, pos, 5));
    }

    // --

    static AddressLocation exact(BlockPos pos) {
        return new Exact(pos);
    }

    record Exact(@NotNull BlockPos pos) implements AddressLocation {
        public static final MapCodec<Exact> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              BlockPos.CODEC.fieldOf("pos").forGetter(Exact::pos)
        ).apply(i, Exact::new));

        @Override
        public Type getType() {
            return Type.EXACT;
        }

        @Override
        public Optional<BlockPos> getPosition() {
            return Optional.of(pos);
        }

        @Override
        public int getDistanceTo(BlockPos pos) {
            return Position.getDistanceBetween(this.pos, pos);
        }
    }

    record Relative(int distance) implements AddressLocation {
        public static final MapCodec<Relative> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
              ExtraCodecs.NON_NEGATIVE_INT.fieldOf("distance").forGetter(Relative::distance)
        ).apply(i, Relative::new));

        @Override
        public Type getType() {
            return Type.RELATIVE;
        }

        @Override
        public Optional<BlockPos> getPosition() {
            return Optional.empty();
        }

        @Override
        public int getDistanceTo(BlockPos pos) {
            return distance;
        }
    }

    AddressLocation DEFAULT = new AddressLocation() {
        @Override
        public Type getType() {
            return Type.DEFAULT;
        }

        @Override
        public Optional<BlockPos> getPosition() {
            return Optional.empty();
        }

        @Override
        public int getDistanceTo(BlockPos pos) {
            return Config.Server.DELIVERY_DEFAULT_DISTANCE.get();
        }
    };

    // --

    enum Type implements StringRepresentable {
        EXACT("exact", Exact.CODEC),
        RELATIVE("relative", Relative.CODEC),
        DEFAULT("default", MapCodec.unit(AddressLocation.DEFAULT));

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends AddressLocation> codec;

        Type(String name, MapCodec<? extends AddressLocation> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends AddressLocation> getCodec() {
            return codec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
