package io.github.mortuusars.envelope.world.delivery;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class Delivery {
    public static final Codec<Delivery> CODEC = RecordCodecBuilder.create(i -> i.group(
          Address.CODEC.fieldOf("sender").forGetter(Delivery::getSender),
          Address.CODEC.fieldOf("recipient").forGetter(Delivery::getRecipient),
          TravelDuration.CODEC.fieldOf("travel_duration").forGetter(Delivery::getTravelDuration),
          DeliveryRoute.CODEC.fieldOf("route").forGetter(Delivery::getRoute),
          DeliveryProgress.CODEC.fieldOf("progress").forGetter(Delivery::getProgress),
          Mail.CODEC.fieldOf("mail").forGetter(Delivery::getMail)
    ).apply(i, Delivery::new));

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Address sender;
    private final Address recipient;
    private TravelDuration travelDuration;
    private DeliveryRoute route;
    private DeliveryProgress progress;
    private Mail mail;

    protected Delivery(Address sender, Address recipient, TravelDuration travelDuration,
                       DeliveryRoute route, DeliveryProgress progress, Mail mail) {
        this.sender = sender;
        this.recipient = recipient;
        this.travelDuration = travelDuration;
        this.progress = progress;
        this.route = route;
        this.mail = mail;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder of(Mail mail) {
        return new Builder()
              .deliver(mail)
              .from(mail.getSender())
              .to(mail.getRecipient());
    }

    public Address getSender() {
        return sender;
    }

    public Address getRecipient() {
        return recipient;
    }

    public TravelDuration getTravelDuration() {
        return travelDuration;
    }

    public Delivery setTravelDuration(TravelDuration travelDuration) {
        this.travelDuration = travelDuration;
        return this;
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

    public Delivery setProgress(DeliveryProgress progress) {
        this.progress = progress;
        return this;
    }

    public DeliveryPhase getCurrentPhase() {
        return getProgress().getPhase();
    }

    public Mail getMail() {
        return mail;
    }

    public Delivery setMail(Mail mail) {
        this.mail = mail;
        return this;
    }

    public void updateMail(UnaryOperator<Mail> updater) {
        setMail(updater.apply(getMail()));
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

    // --

    public static class Builder {
        private Mail mail = Mail.EMPTY;
        private Address sender = Address.UNKNOWN;
        private Address recipient = Address.UNKNOWN;
        private TravelDuration.Supplier travelDuration = TravelDuration.basedOnSenderToRecipientDistance();
        private DeliveryProgress progress = DeliveryProgress.start();

        public Builder deliver(@NotNull Mail mail) {
            this.mail = Objects.requireNonNull(mail);
            return this;
        }

        public Builder from(@NotNull Address sender) {
            this.sender = Objects.requireNonNull(sender);
            return this;
        }

        public Builder to(@NotNull Address recipient) {
            this.recipient = Objects.requireNonNull(recipient);
            return this;
        }

        public Builder travelDuration(@NotNull TravelDuration.Supplier supplier) {
            this.travelDuration = Objects.requireNonNull(supplier);
            return this;
        }

        public Builder withProgress(@NotNull DeliveryProgress progress) {
            this.progress = Objects.requireNonNull(progress);
            return this;
        }

        public Builder atPhase(@NotNull DeliveryPhase phase, int duration) {
            this.progress = new DeliveryProgress(Objects.requireNonNull(phase), duration, 0);
            return this;
        }

        public Delivery create(ServerLevel level) {
            TravelDuration travelDuration = this.travelDuration.get(level, sender, recipient);
            DeliveryRoute route = DeliveryRoute.build(level, sender, recipient);
            return new Delivery(sender, recipient, travelDuration, route, progress, mail);
        }
    }
}