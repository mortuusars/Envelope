package io.github.mortuusars.envelope.world.mail.delivery;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Describes a path courier takes to deliver mail.<br>
 * The idea is to create a route similar to how airplanes fly:<br>
 * Ascend to cruise height > Travel > Hub > Travel > Descend to destination. And back.
 */
public class DeliveryRoute {
    public static final Codec<DeliveryRoute> CODEC = RecordCodecBuilder.create(i -> i.group(
          BlockPos.CODEC.optionalFieldOf("sender_pos").forGetter(DeliveryRoute::getSenderPos),
          BlockPos.CODEC.optionalFieldOf("sender_ascend_pos").forGetter(DeliveryRoute::getSenderAscendPos),
          TravelDuration.CODEC.optionalFieldOf("sender_to_hub_duration", TravelDuration.DEFAULT).forGetter(DeliveryRoute::getSenderToHubDuration),
          BlockPos.CODEC.optionalFieldOf("hub_pos").forGetter(DeliveryRoute::getHubPos),
          TravelDuration.CODEC.optionalFieldOf("recipient_to_hub_duration", TravelDuration.DEFAULT).forGetter(DeliveryRoute::getRecipientToHubDuration),
          BlockPos.CODEC.optionalFieldOf("recipient_ascend_pos").forGetter(DeliveryRoute::getRecipientAscendPos),
          BlockPos.CODEC.optionalFieldOf("recipient_pos").forGetter(DeliveryRoute::getRecipientPos)
    ).apply(i, DeliveryRoute::new));

    public static final DeliveryRoute EMPTY = new DeliveryRoute(
          Optional.empty(), Optional.empty(),
          TravelDuration.DEFAULT, Optional.empty(), TravelDuration.DEFAULT,
          Optional.empty(), Optional.empty());

    private final Optional<BlockPos> senderPos;
    private final Optional<BlockPos> senderAscendPos;
    private final TravelDuration senderToHubDuration;
    private final Optional<BlockPos> hubPos;
    private final TravelDuration recipientToHubDuration;
    private final Optional<BlockPos> recipientAscendPos;
    private final Optional<BlockPos> recipientPos;

    private @Nullable Map<DeliveryPhase, Segment> segments;

    public DeliveryRoute(Optional<BlockPos> senderPos, Optional<BlockPos> senderAscendPos,
                         TravelDuration senderToHubDuration, Optional<BlockPos> hubPos, TravelDuration recipientToHubDuration,
                         Optional<BlockPos> recipientAscendPos, Optional<BlockPos> recipientPos) {
        this.senderPos = senderPos;
        this.senderAscendPos = senderAscendPos;
        this.senderToHubDuration = senderToHubDuration;
        this.hubPos = hubPos;
        this.recipientToHubDuration = recipientToHubDuration;
        this.recipientAscendPos = recipientAscendPos;
        this.recipientPos = recipientPos;
    }

    public static DeliveryRoute build(ServerLevel level, Address sender, Address recipient) {
        MailService mailService = MailService.of(level);

        AddressLocation senderLocation = mailService.getLocationOf(sender);
        AddressLocation recipientLocation = mailService.getLocationOf(recipient);

        Optional<BlockPos> senderPos = senderLocation.getPosition();
        Optional<BlockPos> recipientPos = recipientLocation.getPosition();
        Optional<BlockPos> hubPos = getHubPosition(senderLocation, recipientLocation);

        return new DeliveryRoute(
              senderPos,
              senderLocation.ascendTowards(level, hubPos),
              senderLocation.getTravelDurationTo(hubPos),
              hubPos,
              recipientLocation.getTravelDurationTo(hubPos),
              recipientLocation.ascendTowards(level, hubPos),
              recipientPos);
    }

    public static Optional<BlockPos> getHubPosition(AddressLocation senderLocation, AddressLocation recipientLocation) {
        return senderLocation.getNearestHub().or(recipientLocation::getNearestHub);
    }

    // --

    public Optional<BlockPos> getSenderPos() {
        return senderPos;
    }

    public Optional<BlockPos> getSenderAscendPos() {
        return senderAscendPos;
    }

    public TravelDuration getSenderToHubDuration() {
        return senderToHubDuration;
    }

    public Optional<BlockPos> getHubPos() {
        return hubPos;
    }

    public TravelDuration getRecipientToHubDuration() {
        return recipientToHubDuration;
    }

    public Optional<BlockPos> getRecipientAscendPos() {
        return recipientAscendPos;
    }

    public Optional<BlockPos> getRecipientPos() {
        return recipientPos;
    }

    public TravelDuration getFullTravelDuration() {
        return new TravelDuration(getSenderToHubDuration().ticks() + getRecipientToHubDuration().ticks());
    }

    public Optional<Integer> getDistance() {
        return Optional.of(Position.getDistanceBetween(senderAscendPos, hubPos).orElse(0)
              + Position.getDistanceBetween(hubPos, recipientAscendPos).orElse(0));
    }

    // --

    public Segment getSegment(DeliveryPhase phase) {
        if (segments == null) {
            segments = buildSegments();
        }
        return Objects.requireNonNull(segments.get(phase));
    }

    protected Map<DeliveryPhase, Segment> buildSegments() {
        Map<DeliveryPhase, Segment> map = new HashMap<>();
        map.put(DeliveryPhase.STARTED, new Segment(senderPos, senderPos));
        map.put(DeliveryPhase.DEPARTING_SENDER, new Segment(senderPos, senderAscendPos));
        map.put(DeliveryPhase.TRAVELING_FROM_SENDER_TO_HUB, new Segment(senderAscendPos, hubPos));
        map.put(DeliveryPhase.DISPATCHING_DELIVERY, new Segment(hubPos, hubPos));
        map.put(DeliveryPhase.TRAVELING_FROM_HUB_TO_RECIPIENT, new Segment(hubPos, recipientAscendPos));
        map.put(DeliveryPhase.APPROACHING_RECIPIENT, new Segment(recipientAscendPos, recipientPos));
        map.put(DeliveryPhase.HANDLING_DELIVERY, new Segment(recipientPos, recipientPos));
        map.put(DeliveryPhase.DEPARTING_RECIPIENT, new Segment(recipientPos, recipientAscendPos));
        map.put(DeliveryPhase.TRAVELING_FROM_RECIPIENT_TO_HUB, new Segment(recipientAscendPos, hubPos));
        map.put(DeliveryPhase.DISPATCHING_RETURN, new Segment(hubPos, hubPos));
        map.put(DeliveryPhase.TRAVELING_FROM_HUB_TO_SENDER, new Segment(hubPos, senderAscendPos));
        map.put(DeliveryPhase.APPROACHING_SENDER, new Segment(senderAscendPos, senderPos));
        map.put(DeliveryPhase.HANDLING_RETURN, new Segment(senderPos, senderPos));
        map.put(DeliveryPhase.FINISHED, new Segment(senderPos, senderPos));
        Preconditions.checkState(map.size() == DeliveryPhase.values().length, "Built segments do not cover all delivery phases!");
        return map;
    }

    public record Segment(Optional<BlockPos> startPos, Optional<BlockPos> endPos) {
        public static final Segment EMPTY = new Segment(Optional.empty(), Optional.empty());

        public Optional<BlockPos> getCurrentLocation(float progress) {
            if (startPos().isPresent() && endPos().isPresent()) {
                Vec3 pos = Position.lerp(startPos().get(), endPos().get(), Mth.clamp(progress, 0, 1));
                return Optional.of(BlockPos.containing(pos));
            }
            return Optional.empty();
        }
    }
}