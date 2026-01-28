package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.TravelDuration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface AddressLocation {
    int ASCEND_DISTANCE = 16;

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
        @Override
        public Optional<BlockPos> getPosition() {
            return Optional.of(pos);
        }

        @Override
        public int getDistanceTo(BlockPos pos) {
            return Position.getDistanceBetween(this.pos, pos);
        }
    }

    AddressLocation VIRTUAL = new AddressLocation() {
        @Override
        public Optional<BlockPos> getPosition() {
            return Optional.empty();
        }

        @Override
        public int getDistanceTo(BlockPos pos) {
            BlockPos virtualPos = Position.snapToGrid(pos, 2048);
            return Position.getDistanceBetween(virtualPos, pos);
        }
    };

    AddressLocation UNKNOWN = new AddressLocation() {
        @Override
        public Optional<BlockPos> getPosition() {
            return Optional.empty();
        }

        @Override
        public int getDistanceTo(BlockPos pos) {
            return Config.Server.DELIVERY_DEFAULT_DISTANCE.get();
        }
    };
}
