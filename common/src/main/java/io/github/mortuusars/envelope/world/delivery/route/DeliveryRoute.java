package io.github.mortuusars.envelope.world.delivery.route;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Describes a path courier takes to deliver mail.<br>
 * The idea is to create a route similar to how airplanes fly:<br>
 * Ascend to cruise height > Travel > Descend to destination. And back.
 */
public class DeliveryRoute {
    public static final Codec<DeliveryRoute> CODEC = RecordCodecBuilder.create(i -> i.group(
          BlockPos.CODEC.optionalFieldOf("sender_pos").forGetter(DeliveryRoute::senderPos),
          BlockPos.CODEC.optionalFieldOf("sender_ascend_pos").forGetter(DeliveryRoute::senderAscendPos),
          BlockPos.CODEC.optionalFieldOf("recipient_ascend_pos").forGetter(DeliveryRoute::recipientAscendPos),
          BlockPos.CODEC.optionalFieldOf("recipient_pos").forGetter(DeliveryRoute::recipientPos)
    ).apply(i, DeliveryRoute::new));

    public static final int DEFAULT_ASCEND_DISTANCE = 12;

    public static final DeliveryRoute EMPTY = new DeliveryRoute(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    private final Optional<BlockPos> senderPos;
    private final Optional<BlockPos> senderAscendPos;
    private final Optional<BlockPos> recipientAscendPos;
    private final Optional<BlockPos> recipientPos;

    private @Nullable Map<DeliveryPhase, Segment> segments;

    public DeliveryRoute(Optional<BlockPos> senderPos, Optional<BlockPos> senderAscendPos,
                         Optional<BlockPos> recipientAscendPos, Optional<BlockPos> recipientPos) {
        this.senderPos = senderPos;
        this.senderAscendPos = senderAscendPos;
        this.recipientAscendPos = recipientAscendPos;
        this.recipientPos = recipientPos;
    }

    public static DeliveryRoute build(ServerLevel level, Address sender, Address recipient) {
        Optional<BlockPos> senderPos = Position.ofAddress(level, sender);
        Optional<BlockPos> recipientPos = Position.ofAddress(level, recipient);
        int ascendDistance = Position.getDistanceBetween(senderPos, recipientPos)
              .map(distance -> Math.min(DEFAULT_ASCEND_DISTANCE, distance / 2))
              .orElse(DEFAULT_ASCEND_DISTANCE);
        // Address#hashCode is used as seed, to make random direction (if pos is unknown) of a specific address be always the same
        Optional<BlockPos> senderAscendPos = Position.ascendTowards(level, senderPos, recipientPos, ascendDistance, recipient.hashCode());
        Optional<BlockPos> recipientAscendPos = Position.ascendTowards(level, recipientPos, senderPos, ascendDistance, sender.hashCode());
        return new DeliveryRoute(senderPos, senderAscendPos, recipientAscendPos, recipientPos);
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

    // --

    public Optional<Integer> getDistance() {
        return Position.getDistanceBetween(senderPos, recipientPos);
    }

    public Segment getSegment(DeliveryPhase phase) {
        if (segments == null) {
            segments = buildSegments();
        }
        return segments.getOrDefault(phase, Segment.EMPTY);
    }

    protected Map<DeliveryPhase, Segment> buildSegments() {
        Map<DeliveryPhase, Segment> map = new HashMap<>();
        map.put(DeliveryPhase.STARTED, new Segment(senderPos, senderPos));
        map.put(DeliveryPhase.DEPARTING_SENDER, new Segment(senderPos, senderAscendPos));
        map.put(DeliveryPhase.LOCATING_RECIPIENT, new Segment(senderAscendPos, Optional.empty()));
        map.put(DeliveryPhase.TRAVELING_TO_RECIPIENT, new Segment(Optional.empty(), recipientAscendPos));
        map.put(DeliveryPhase.APPROACHING_RECIPIENT, new Segment(recipientAscendPos, recipientPos));
        map.put(DeliveryPhase.HANDLING_DELIVERY, new Segment(recipientPos, recipientPos));
        map.put(DeliveryPhase.DEPARTING_RECIPIENT, new Segment(recipientPos, recipientAscendPos));
        map.put(DeliveryPhase.TRAVELING_TO_SENDER, new Segment(recipientAscendPos, senderAscendPos));
        map.put(DeliveryPhase.APPROACHING_SENDER, new Segment(senderAscendPos, senderPos));
        map.put(DeliveryPhase.HANDLING_RETURN, new Segment(senderPos, senderPos));
        map.put(DeliveryPhase.FINISHED, new Segment(senderPos, senderPos));
        return map;
    }

    public record Segment(Optional<BlockPos> startPos, Optional<BlockPos> endPos) {
        public static final Segment EMPTY = new Segment(Optional.empty(), Optional.empty());
    }
}