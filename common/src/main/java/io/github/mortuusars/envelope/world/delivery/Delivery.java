package io.github.mortuusars.envelope.world.delivery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.delivery.route.DeliveryRoute;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class Delivery {
    public static final Codec<Delivery> CODEC = RecordCodecBuilder.create(i -> i.group(
          Address.CODEC.fieldOf("sender").forGetter(Delivery::getSender),
          Address.CODEC.fieldOf("recipient").forGetter(Delivery::getRecipient),
          DeliveryMetadata.CODEC.optionalFieldOf("metadata", DeliveryMetadata.EMPTY).forGetter(Delivery::getMetadata),
          Mail.CODEC.optionalFieldOf("mail", Mail.EMPTY).forGetter(Delivery::getMail),
          DeliveryRoute.CODEC.optionalFieldOf("route", DeliveryRoute.EMPTY).forGetter(Delivery::getRoute),
          DeliveryPhase.CODEC.optionalFieldOf("phase", DeliveryPhase.STARTED).forGetter(Delivery::getPhase),
          Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("phase_progress", 0).forGetter(Delivery::getPhaseProgress),
          Codec.BOOL.optionalFieldOf("ended", false).forGetter(Delivery::isEnded)
    ).apply(i, Delivery::new));

    private final Address sender;
    private final Address recipient;
    private DeliveryMetadata metadata;
    private Mail mail;
    private DeliveryRoute route;
    private DeliveryPhase phase;
    private int phaseProgress;
    private boolean ended;

    public Delivery(Address sender, Address recipient, DeliveryMetadata metadata, Mail mail,
                    DeliveryRoute route, DeliveryPhase phase, int phaseProgress, boolean ended) {
        this.sender = sender;
        this.recipient = recipient;
        this.metadata = metadata;
        this.mail = mail;
        this.route = route;
        this.phase = phase;
        this.phaseProgress = phaseProgress;
        this.ended = ended;
    }

    public static Builder builder() {
        return new Builder();
    }

    // --

    public Address getSender() {
        return sender;
    }

    public Address getRecipient() {
        return recipient;
    }

    public DeliveryMetadata getMetadata() {
        return metadata;
    }

    public Delivery setMetadata(DeliveryMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public Delivery updateMetadata(UnaryOperator<DeliveryMetadata> updater) {
        this.metadata = updater.apply(this.metadata);
        return this;
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

    public DeliveryRoute getRoute() {
        return route;
    }

    public void setRoute(DeliveryRoute route) {
        this.route = route;
    }

    public void updateRoute(ServerLevel level) {
        setRoute(DeliveryRoute.build(level, getSender(), getRecipient()));
    }

    public DeliveryPhase getPhase() {
        return phase;
    }

    public void setPhase(DeliveryPhase currentPhase, int progress) {
        this.phase = currentPhase;
        setPhaseProgress(progress);
    }

    public void setPhaseAndResetProgress(DeliveryPhase currentPhase) {
        this.phase = currentPhase;
        setPhaseProgress(0);
    }

    public int getPhaseProgress() {
        return phaseProgress;
    }

    public void setPhaseProgress(int progress) {
        this.phaseProgress = Math.max(0, progress);
    }

    public void incrementCurrentPhaseProgress() {
        phaseProgress++;
    }

    public boolean isEnded() {
        return ended;
    }

    public void end() {
        this.ended = true;
    }

    public Optional<BlockPos> estimateCurrentPos() {
        DeliveryRoute.Segment segment = getRoute().getSegment(getPhase());
        if (segment.startPos().isPresent() && segment.endPos().isPresent()) {
            //TODO: simplify and move to Segment class
            Vec3 pos = Position.lerp(segment.startPos().get(), segment.endPos().get(), getPhaseProgress());
            return Optional.of(BlockPos.containing(pos));
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Delivery{" +
              "sender=" + sender +
              ", recipient=" + recipient +
              ", mail=" + mail +
              ", phase=" + phase +
              ", progress=" + phaseProgress +
              '}';
    }

    // --

    public static class Builder {
        private Address sender = Address.UNKNOWN;
        private Address recipient = Address.UNKNOWN;
        private @Nullable UUID owner = null;
        private Mail mail = Mail.EMPTY;
        private DeliveryPhase phase = DeliveryPhase.STARTED;

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

        public Builder owner(@Nullable UUID owner) {
            this.owner = owner;
            return this;
        }

        public Builder atPhase(@NotNull DeliveryPhase phase) {
            this.phase = phase;
            return this;
        }

        public Delivery create() {
            DeliveryMetadata metadata = DeliveryMetadata.EMPTY.withOwner(owner);
            return new Delivery(sender, recipient, metadata, mail, DeliveryRoute.EMPTY, phase, 0, false);
        }
    }
}