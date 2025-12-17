package io.github.mortuusars.envelope.world.delivery;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.util.result.Error;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class Delivery {
    public static final Codec<Delivery> CODEC = RecordCodecBuilder.<Delivery>create(i -> i.group(
          Address.CODEC.fieldOf("sender").forGetter(Delivery::getSender),
          Address.CODEC.fieldOf("recipient").forGetter(Delivery::getRecipient),
          DeliveryOrigin.CODEC.optionalFieldOf("origin", DeliveryOrigin.service()).forGetter(Delivery::getOrigin),
          TravelDuration.CODEC.fieldOf("travel_duration").forGetter(Delivery::getTravelDuration),
          DeliveryRoute.CODEC.fieldOf("route").forGetter(Delivery::getRoute),
          DeliveryProgress.CODEC.fieldOf("progress").forGetter(Delivery::getProgress),
          Mail.CODEC.fieldOf("mail").forGetter(Delivery::getMail)
    ).apply(i, Delivery::new)).validate(delivery -> delivery.validate().asDataResult());

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Result<Delivery> ERROR_SAME_ADDRESSES = Result.error(new Error(
          "Recipient address cannot be the same as sender address.",
          "error.envelope.delivery.same_addresses"));
    private static final Result<Delivery> ERROR_RECIPIENT_UNKNOWN = Result.error(new Error(
          "Cannot deliver to unknown address.",
          "error.envelope.delivery.unknown_address"));
    private static final Result<Delivery> ERROR_NO_MAIL = Result.error(new Error(
          "Mail is empty.",
          "error.envelope.delivery.no_mail"));

    private final Address sender;
    private final Address recipient;
    private final DeliveryOrigin origin;
    private TravelDuration travelDuration;
    private DeliveryRoute route;
    private DeliveryProgress progress;
    private Mail mail;

    protected Delivery(Address sender, Address recipient, DeliveryOrigin origin, TravelDuration travelDuration,
                       DeliveryRoute route, DeliveryProgress progress, Mail mail) {
        this.sender = sender;
        this.recipient = recipient;
        this.origin = origin;
        this.travelDuration = travelDuration;
        this.progress = progress;
        this.route = route;
        this.mail = mail;
    }

    protected Result<Delivery> validate() {
        if (getRecipient().matches(Address.UNKNOWN)) return ERROR_RECIPIENT_UNKNOWN;
        if (getSender().matches(getRecipient())) return ERROR_SAME_ADDRESSES;
        if (getCurrentPhase() == DeliveryPhase.STARTED && getMail().isEmpty()) return ERROR_NO_MAIL;
        return Result.success(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder service() {
        return new Builder().service();
    }

    public static Builder real(BlockPos pos) {
        return new Builder().real(pos);
    }

    public static Result<Delivery> service(ServerLevel level, Mail mail, Address from, Address to) {
        return builder()
              .deliver(mail)
              .from(from)
              .to(to)
              .origin(DeliveryOrigin.service())
              .create(level);
    }

    public static Result<Delivery> create(ServerLevel level, Mail mail, Address from, Address to, DeliveryOrigin origin) {
        return builder()
              .deliver(mail)
              .from(from)
              .to(to)
              .origin(origin)
              .create(level);
    }

    public static Result<Delivery> create(ServerLevel level, Mail mail, DeliveryOrigin origin) {
        Address recipient = mail.shouldBeHandledByMailService() ? Address.MAIL_SERVICE : mail.getRecipient();
        return create(level, mail, mail.getSender(), recipient, origin);
    }

    public Address getSender() {
        return sender;
    }

    public Address getRecipient() {
        return recipient;
    }

    public DeliveryOrigin getOrigin() {
        return origin;
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
        private DeliveryOrigin origin = DeliveryOrigin.service();
        private TravelDuration.Supplier travelDuration = TravelDuration.basedOnSenderToRecipientDistance();
        private DeliveryProgress progress = DeliveryProgress.start();

        public Builder deliver(Mail mail) {
            this.mail = Objects.requireNonNull(mail);
            return this;
        }

        public Builder from(Address sender) {
            this.sender = Objects.requireNonNull(sender);
            return this;
        }

        public Builder to(Address recipient) {
            this.recipient = Objects.requireNonNull(recipient);
            return this;
        }

        public Builder origin(DeliveryOrigin origin) {
            this.origin = origin;
            return this;
        }

        public Builder service() {
            this.origin = DeliveryOrigin.service();
            return this;
        }

        public Builder real(BlockPos pos) {
            this.origin = DeliveryOrigin.local(Objects.requireNonNull(pos));
            return this;
        }

        public Builder travelDuration(TravelDuration.Supplier travelDurationSupplier) {
            this.travelDuration = travelDurationSupplier;
            return this;
        }

        public Builder withProgress(DeliveryProgress progress) {
            this.progress = progress;
            return this;
        }

        public Builder startAt(DeliveryPhase phase, int duration) {
            this.progress = new DeliveryProgress(phase, duration, 0);
            return this;
        }

        public Result<Delivery> create(ServerLevel level) {
            TravelDuration travelDuration = this.travelDuration.get(level, sender, recipient);
            DeliveryRoute route = DeliveryRoute.build(level, sender, recipient);
            Result<Delivery> delivery = new Delivery(sender, recipient, origin, travelDuration, route, progress, mail).validate();
            if (delivery.isSuccess() && Bugger.isEnabled()) {
                LOGGER.info("Delivery of {} - '{} > {}' is created.", mail.toString(), sender, recipient);
            }
            return delivery;
        }

        public Delivery createOrThrow(ServerLevel level) {
            return create(level).getValue().orElseThrow();
        }
    }
}