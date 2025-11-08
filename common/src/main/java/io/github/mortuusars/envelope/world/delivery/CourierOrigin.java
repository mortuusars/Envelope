package io.github.mortuusars.envelope.world.delivery;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record CourierOrigin(Optional<BlockPos> homePos) {
    public static final Codec<CourierOrigin> CODEC = BlockPos.CODEC.optionalFieldOf("home_pos")
          .xmap(CourierOrigin::new, CourierOrigin::homePos)
          .codec();

    public static CourierOrigin real(BlockPos homePos) {
        return new CourierOrigin(Optional.ofNullable(homePos));
    }

    public static CourierOrigin service() {
        return new CourierOrigin(Optional.empty());
    }

    // --

    public @NotNull BlockPos getHomePos() throws IllegalStateException {
        Preconditions.checkState(homePos.isPresent(),
              "Cannot get home pos of a service courier. Should check with isReal/isService first.");
        return homePos.orElseThrow();
    }

    public boolean isReal() {
        return homePos.isPresent();
    }

    public boolean isService() {
        return !isReal();
    }
}
