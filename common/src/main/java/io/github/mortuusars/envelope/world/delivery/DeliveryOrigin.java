package io.github.mortuusars.envelope.world.delivery;

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
public record DeliveryOrigin(Optional<BlockPos> pos) {
    public static final Codec<DeliveryOrigin> CODEC = BlockPos.CODEC.optionalFieldOf("pos")
          .xmap(DeliveryOrigin::new, DeliveryOrigin::pos)
          .codec();

    private static final DeliveryOrigin SERVICE = new DeliveryOrigin(Optional.empty());

    public static DeliveryOrigin local(BlockPos homePos) {
        return new DeliveryOrigin(Optional.ofNullable(homePos));
    }

    public static DeliveryOrigin service() {
        return SERVICE;
    }

    // --

    public @NotNull BlockPos getPos() throws IllegalStateException {
        Preconditions.checkState(pos.isPresent(),
              "Cannot get position of a service origin. Should check with isLocal/isService first.");
        return pos.orElseThrow();
    }

    public boolean isLocal() {
        return pos.isPresent();
    }

    public boolean isService() {
        return !isLocal();
    }
}
