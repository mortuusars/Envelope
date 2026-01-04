package io.github.mortuusars.envelope.world.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

public record DeliveryMetadata(Optional<UUID> owner, long timestamp) {
    public static final Codec<DeliveryMetadata> CODEC = RecordCodecBuilder.create(i -> i.group(
          UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(DeliveryMetadata::owner),
          Codec.LONG.optionalFieldOf("timestamp", -1L).forGetter(DeliveryMetadata::timestamp)
    ).apply(i, DeliveryMetadata::new));

    public static final DeliveryMetadata EMPTY = new DeliveryMetadata(Optional.empty(), -1L);

    public DeliveryMetadata withOwner(UUID owner) {
        return new DeliveryMetadata(Optional.ofNullable(owner), timestamp);
    }

    public DeliveryMetadata withTimestamp(long timestamp) {
        return new DeliveryMetadata(owner, timestamp);
    }

    public DeliveryMetadata withTimestampIfMissing(long timestamp) {
        return this.timestamp < 0 ? new DeliveryMetadata(owner, timestamp) : this;
    }
}
