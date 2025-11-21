package io.github.mortuusars.envelope.world.delivery;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.UnaryOperator;

public class Delivery {
    public static final Codec<Delivery> CODEC = RecordCodecBuilder.create(i -> i.group(
          Address.CODEC.fieldOf("sender").forGetter(Delivery::getSender),
          Address.CODEC.fieldOf("recipient").forGetter(Delivery::getRecipient),
          Codec.intRange(0, Integer.MAX_VALUE).fieldOf("travel_duration").forGetter(Delivery::getTravelDuration),
          DeliveryOrigin.CODEC.optionalFieldOf("origin", DeliveryOrigin.service()).forGetter(Delivery::getOrigin),
          Mail.CODEC.fieldOf("mail").forGetter(Delivery::getMail),
          DeliveryRoute.CODEC.fieldOf("route").forGetter(Delivery::getRoute),
          DeliveryProgress.CODEC.fieldOf("progress").forGetter(Delivery::getProgress)
    ).apply(i, Delivery::new));

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Address sender;
    private final Address recipient;
    private final int travelDuration;
    private final DeliveryOrigin origin;
    private final DeliveryProgress progress;
    private DeliveryRoute route;
    private Mail mail;

    public Delivery(Address sender, Address recipient, int travelDuration, DeliveryOrigin origin, Mail mail, DeliveryRoute route, DeliveryProgress progress) {
        this.sender = sender;
        this.recipient = recipient;
        this.travelDuration = travelDuration;
        this.origin = origin;
        this.mail = mail;
        this.route = route;
        this.progress = progress;
    }

    public static @Nullable Delivery parse(CompoundTag tag, RegistryAccess registryAccess) {
        if (tag.isEmpty()) return null;
        return CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), tag)
              .ifError(e -> LOGGER.error("Cannot parse Delivery from tag '{}': {}", tag, e))
              .result()
              .orElse(null);
    }

    public Tag encode(RegistryAccess registryAccess) throws IllegalStateException {
        return Delivery.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
    }

    public static Delivery create(ServerLevel level, ItemStack mailStack, DeliveryOrigin origin) {
        Mail mail = new Mail(mailStack);

        Address sender = mail.getSenderOrThrow();
        Address recipient = mail.getRecipientOrThrow();

        DeliveryRoute route = DeliveryRoute.build(level, sender, recipient);

        int travelDuration = calculateTravelDuration(level, sender, recipient);

        if (Bugger.isEnabled()) {
            LOGGER.info("Delivery of {} - '{} > {}' is created.", mailStack.getHoverName().getString(), sender, recipient);
        }

        return new Delivery(sender, recipient, travelDuration, origin, mail, route, DeliveryProgress.start());
    }

    public static int calculateTravelDuration(ServerLevel level, Address sender, Address recipient) {
        int distance = level.getEnvelopeContext().addresses().getDistanceTo(sender, recipient)
              .orElse(Config.Server.DELIVERY_DEFAULT_DISTANCE.get());
        return calculateTravelDuration(distance);
    }

    public static int calculateTravelDuration(int distance) {
        distance = Math.min(distance, Config.Server.DELIVERY_TRAVEL_DURATION_DISTANCE_CAP.get());
        double seconds = distance / Config.Server.DELIVERY_COURIER_TRAVEL_SPEED.get();
        return Math.max(1, Ticks.fromSeconds(seconds));
    }

    public Address getSender() {
        return sender;
    }

    public Address getRecipient() {
        return recipient;
    }

    public int getTravelDuration() {
        return travelDuration;
    }

    public DeliveryOrigin getOrigin() {
        return origin;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public void updateMail(UnaryOperator<Mail> updater) {
        setMail(updater.apply(getMail()));
    }

    public DeliveryRoute getRoute() {
        return route;
    }

    public void setRoute(DeliveryRoute route) {
        this.route = route;
    }

    public DeliveryProgress getProgress() {
        return progress;
    }

    public DeliveryPhase getCurrentPhase() {
        return getProgress().getPhase();
    }

    // --

    public boolean isFinished() {
        return getProgress().getPhase() == DeliveryPhase.FINISHED && getProgress().isDone();
    }

    public void tick(ServerLevel level, DeliveryHandler handler) {
        if (isFinished()) return;

        if (getProgress().getTicks() == 0) {
            setRoute(DeliveryRoute.build(level, sender, recipient));
            handler.phaseStarted(level, this, getCurrentPhase());
        }

        getProgress().tick();

        if (getProgress().isDone()) {
            handler.phaseCompleted(level, this, getCurrentPhase());

            if (getCurrentPhase() == DeliveryPhase.FINISHED) {
                if (Bugger.isEnabled()) {
                    LOGGER.info("Delivery '{} > {}' is finished.", getSender(), getRecipient());
                }
                handler.endDelivery(level, this);
            } else {
                DeliveryPhase nextPhase = handler.advancePhase(level, this, getCurrentPhase());
                int nextPhaseDuration = handler.getPhaseDuration(level, this, nextPhase);
                getProgress().advance(nextPhase, nextPhaseDuration);
            }
        } else {
            handler.phaseTicked(level, this, getCurrentPhase());
        }
    }

    public Optional<BlockPos> estimateCurrentPos() {
        DeliveryRoute.Segment segment = getRoute().getSegment(getCurrentPhase());
        if (segment.startPos().isPresent() && segment.endPos().isPresent()) {
            Vec3 pos = Position.lerp(segment.startPos().get(), segment.endPos().get(), getProgress().getCompleteness());
            return Optional.of(BlockPos.containing(pos));
        }
        return Optional.empty();
    }
}