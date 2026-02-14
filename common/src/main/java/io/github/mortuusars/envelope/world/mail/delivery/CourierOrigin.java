package io.github.mortuusars.envelope.world.mail.delivery;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Service deliveries have no position defined, and thus courier will be despawned after delivery ends.<br>
 * Real entities will have it set to spawn there after delivery (if delivery is finished in the background).
 * @param pos Spawn position.
 */
public record CourierOrigin(Optional<BlockPos> pos) {
    public static final Codec<CourierOrigin> CODEC = BlockPos.CODEC.optionalFieldOf("pos")
          .xmap(CourierOrigin::new, CourierOrigin::pos)
          .codec();

    private static final CourierOrigin SERVICE = new CourierOrigin(Optional.empty());

    public static CourierOrigin regular(BlockPos homePos) {
        return new CourierOrigin(Optional.ofNullable(homePos));
    }

    public static CourierOrigin service() {
        return SERVICE;
    }

    // --

    public @NotNull BlockPos getPos() throws IllegalStateException {
        Preconditions.checkState(pos.isPresent(),
              "Cannot get position of a service origin. Should check with isRegular/isService first.");
        return pos.orElseThrow();
    }

    public boolean isRegular() {
        return pos.isPresent();
    }

    public boolean isService() {
        return !isRegular();
    }
}
