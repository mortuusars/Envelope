package io.github.mortuusars.envelope.world.delivery;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Service deliveries have no position defined, and thus courier will be despawned after delivery ends.<br>
 * Real entities will have it set to spawn there after delivery (if delivery is finished in background).
 * @param pos Basically the starting position of a delivery.
 */
public record DeliveryOrigin(Optional<BlockPos> pos) {
    public static final Codec<DeliveryOrigin> CODEC = BlockPos.CODEC.optionalFieldOf("pos")
          .xmap(DeliveryOrigin::new, DeliveryOrigin::pos)
          .codec();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static @Nullable DeliveryOrigin parse(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag)
              .ifError(e -> LOGGER.error("Cannot parse DeliveryOrigin from tag '{}': {}", tag, e))
              .result()
              .orElse(null);
    }

    public static DeliveryOrigin regular(BlockPos homePos) {
        return new DeliveryOrigin(Optional.ofNullable(homePos));
    }

    public static DeliveryOrigin service() {
        return new DeliveryOrigin(Optional.empty());
    }

    // --

    public @NotNull BlockPos getStartPos() throws IllegalStateException {
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
