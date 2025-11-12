package io.github.mortuusars.envelope.world.delivery.route;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public final class DeliveryRoute {
    public static final Codec<DeliveryRoute> CODEC = RecordCodecBuilder.create(i -> i.group(
          BlockPos.CODEC.optionalFieldOf("sender_pos").forGetter(DeliveryRoute::senderPos),
          BlockPos.CODEC.optionalFieldOf("sender_ascend_pos").forGetter(DeliveryRoute::senderAscendPos),
          BlockPos.CODEC.optionalFieldOf("recipient_pos").forGetter(DeliveryRoute::recipientPos),
          BlockPos.CODEC.optionalFieldOf("recipient_ascend_pos").forGetter(DeliveryRoute::recipientAscendPos),
          Codec.intRange(0, 128).optionalFieldOf("ascend_distance", 0).forGetter(DeliveryRoute::ascendDistance)
    ).apply(i, DeliveryRoute::new));

    private Optional<BlockPos> senderPos;
    private Optional<BlockPos> senderAscendPos;
    private Optional<BlockPos> recipientAscendPos;
    private Optional<BlockPos> recipientPos;
    private int ascendDistance;

    public DeliveryRoute(Optional<BlockPos> senderPos, Optional<BlockPos> senderAscendPos,
                         Optional<BlockPos> recipientAscendPos, Optional<BlockPos> recipientPos,
                         int ascendDistance) {
        this.senderPos = senderPos;
        this.senderAscendPos = senderAscendPos;
        this.recipientAscendPos = recipientAscendPos;
        this.recipientPos = recipientPos;
        this.ascendDistance = ascendDistance;
    }

    public static DeliveryRoute create(ServerLevel level, Address sender, Address recipient, int ascendDistance) {
        return new DeliveryRoute(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), ascendDistance)
              .update(level, sender, recipient, ascendDistance);
    }

    public DeliveryRoute update(ServerLevel level, Address sender, Address recipient, int ascendDistance) {
        senderPos = Position.ofAddress(level, sender);
        recipientPos = Position.ofAddress(level, recipient);
        senderAscendPos = Position.ascendTowards(level, senderPos, recipientPos, ascendDistance);
        recipientAscendPos = Position.ascendTowards(level, recipientPos, senderPos, ascendDistance);
        this.ascendDistance = ascendDistance;
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public DeliveryRoute update(ServerLevel level, Address sender, Address recipient) {
        update(level, sender, recipient, ascendDistance);
        return this;
    }

    // --

    public Optional<BlockPos> senderPos() {
        return senderPos;
    }

    public Optional<BlockPos> senderAscendPos() {
        return senderAscendPos;
    }

    public Optional<BlockPos> recipientAscendPos() {
        return recipientAscendPos;
    }

    public Optional<BlockPos> recipientPos() {
        return recipientPos;
    }

    public int ascendDistance() {
        return ascendDistance;
    }

    // --

    public int getTotalDistance() {
        if (senderPos.isEmpty() || recipientPos.isEmpty()) return Integer.MAX_VALUE;
        return (int)Math.sqrt(senderPos.get().distSqr(recipientPos.get()));
    }

    public boolean canSkipTraveling() {
        if (senderPos.isEmpty() || recipientPos.isEmpty()) return false;
        return getTotalDistance() < ascendDistance * 2;
    }

    public Segment getSegment(DeliveryPhase phase) {
        return switch (phase) {
            case DEPARTING_SENDER -> new Segment(senderPos, senderAscendPos);
            case LOCATING_RECIPIENT -> new Segment(senderAscendPos, Optional.empty());
            case TRAVELING_TO_RECIPIENT -> new Segment(Optional.empty(), recipientAscendPos);
            case APPROACHING_RECIPIENT -> new Segment(recipientAscendPos, recipientPos);
            case HANDLING_DELIVERY -> new Segment(recipientPos, recipientPos);
            case DEPARTING_RECIPIENT -> new Segment(recipientPos, recipientAscendPos);
            case TRAVELING_TO_SENDER -> new Segment(recipientAscendPos, senderAscendPos);
            case APPROACHING_SENDER -> new Segment(senderAscendPos, senderPos);
            case STARTED, HANDLING_RETURN, FINISHED -> new Segment(senderPos, senderPos);
        };
    }

    public record Segment(Optional<BlockPos> startPos, Optional<BlockPos> endPos) {
    }
}