package io.github.mortuusars.envelope.world.delivery;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.world.Position;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.IntFunction;

public class Delivery {
    public static final Codec<Delivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("mail").forGetter(Delivery::getMail),
            Address.CODEC.optionalFieldOf("sender", Address.UNKNOWN).forGetter(Delivery::getSender),
            BlockPos.CODEC.optionalFieldOf("sender_pos").forGetter(Delivery::getSenderPos),
            Address.CODEC.fieldOf("recipient").forGetter(Delivery::getRecipient),
            BlockPos.CODEC.optionalFieldOf("recipient_pos").forGetter(Delivery::getRecipientPos),
            Codec.INT.optionalFieldOf("travel_duration", -1).forGetter(Delivery::getTravelDuration),
            Phase.CODEC.optionalFieldOf("phase", null).forGetter(Delivery::getPhase)
    ).apply(instance, Delivery::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Delivery> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull Delivery decode(RegistryFriendlyByteBuf buffer) {
            return new Delivery(
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    Address.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buffer),
                    Address.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    Phase.STREAM_CODEC.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Delivery delivery) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, delivery.getMail());
            Address.STREAM_CODEC.encode(buffer, delivery.getSender());
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buffer, delivery.getSenderPos());
            Address.STREAM_CODEC.encode(buffer, delivery.getRecipient());
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buffer, delivery.getRecipientPos());
            ByteBufCodecs.VAR_INT.encode(buffer, delivery.getTravelDuration());
            Phase.STREAM_CODEC.encode(buffer, delivery.getPhase());
        }
    };

    protected ItemStack mail;
    protected Address sender;
    protected Optional<BlockPos> senderPos;
    protected Address recipient;
    protected Optional<BlockPos> recipientPos;
    protected int travelDuration;
    protected Phase phase;

    public Delivery(ItemStack mail, Address sender, Optional<BlockPos> senderPos, Address recipient,
                    Optional<BlockPos> recipientPos, int travelDuration, @Nullable Phase phase) {
        this.mail = mail;
        this.sender = sender;
        this.senderPos = senderPos;
        this.recipient = recipient;
        this.recipientPos = recipientPos;
        this.travelDuration = travelDuration;
        if (phase == null) {
            phase = new Phase(Phase.Type.LEAVING_HOME, Optional.empty(), Optional.empty(), Phase.DEFAULT_DURATION, 0);
        }
        this.phase = phase;
    }

    public static Delivery start(ServerLevel level, ItemStack mail) {
        @Nullable Address sender = mail.get(Envelope.DataComponents.MAIL_SENDER);
        Preconditions.checkNotNull(sender, "Mail '" + mail + "' does not have 'envelope:mail_sender' defined.");
        @Nullable Address recipient = mail.get(Envelope.DataComponents.MAIL_RECIPIENT);
        Preconditions.checkNotNull(recipient, "Mail '" + mail + "' does not have 'envelope:mail_recipient' defined.");

        return new Delivery(mail,
                sender,
                Position.ofAddress(level, sender),
                mail.get(Envelope.DataComponents.MAIL_RECIPIENT),
                Position.ofAddress(level, recipient),
                mail.getOrDefault(Envelope.DataComponents.MAIL_TRAVEL_DURATION, Config.Server.MAIL_TRAVEL_DURATION.get()),
                Phase.start());
    }

    // --

    public ItemStack getMail() {
        return mail;
    }

    public Delivery setMail(ItemStack mail) {
        this.mail = mail;
        return this;
    }

    public Address getSender() {
        return sender;
    }

    public Delivery setSender(Address sender) {
        this.sender = sender;
        return this;
    }

    public Optional<BlockPos> getSenderPos() {
        return senderPos;
    }

    public Delivery setSenderPos(Optional<BlockPos> senderPos) {
        this.senderPos = senderPos;
        return this;
    }

    public Address getRecipient() {
        return recipient;
    }

    public Delivery setRecipient(Address recipient) {
        this.recipient = recipient;
        return this;
    }

    public Optional<BlockPos> getRecipientPos() {
        return recipientPos;
    }

    public Delivery setRecipientPos(Optional<BlockPos> recipientPos) {
        this.recipientPos = recipientPos;
        return this;
    }

    public int getTravelDuration() {
        return travelDuration;
    }

    public Delivery setTravelDuration(int travelDuration) {
        this.travelDuration = travelDuration;
        return this;
    }

    public Phase getPhase() {
        return phase;
    }

    public Delivery setPhase(Phase phase) {
        this.phase = phase;
        return this;
    }

    public void advancePhase() {
        getPhase().setType(phase.getType().next(this))
                .setStart(phase.getEnd())
                .setEnd(Optional.empty())
                .setDuration(phase.getType().isTraveling() ? getTravelDuration() : Delivery.Phase.DEFAULT_DURATION)
                .setTicks(0);
    }

    public boolean shouldSkipTravelingPhase() {
        if (senderPos.isEmpty() || recipientPos.isEmpty()) return false;
        return senderPos.get().distSqr(recipientPos.get()) < 1024; // 32 blocks
    }

    public MutableComponent createSenderToRecipientComponent(String middle) {
        return getSender().getDisplayName().append(middle).append(getRecipient().getDisplayName());
    }

    // --

    public static class Phase {
        public static final int DEFAULT_DURATION = 15 * SharedConstants.TICKS_PER_SECOND;

        public static final Codec<Phase> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Type.CODEC.optionalFieldOf("type", Type.LEAVING_HOME).forGetter(Phase::getType),
                BlockPos.CODEC.optionalFieldOf("start_pos").forGetter(Phase::getStart),
                BlockPos.CODEC.optionalFieldOf("end_pos").forGetter(Phase::getEnd),
                Codec.INT.optionalFieldOf("duration", DEFAULT_DURATION).forGetter(Phase::getDuration),
                Codec.INT.optionalFieldOf("ticks", 0).forGetter(Phase::getTicks)
        ).apply(instance, Phase::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Phase> STREAM_CODEC = StreamCodec.composite(
                Type.STREAM_CODEC, Phase::getType,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Phase::getStart,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Phase::getEnd,
                ByteBufCodecs.VAR_INT, Phase::getDuration,
                ByteBufCodecs.VAR_INT, Phase::getTicks,
                Phase::new
        );

        protected Type type;
        protected Optional<BlockPos> start;
        protected Optional<BlockPos> end;
        protected int duration;
        protected int ticks;

        public Phase(Type type, Optional<BlockPos> start, Optional<BlockPos> end, int duration, int ticks) {
            this.type = type;
            this.start = start;
            this.end = end;
            this.duration = duration;
        }

        public static Phase start() {
            return new Phase(Type.LEAVING_HOME, Optional.empty(), Optional.empty(), DEFAULT_DURATION, 0);
        }

        public Type getType() {
            return type;
        }

        public Phase setType(Type type) {
            this.type = type;
            return this;
        }

        public Optional<BlockPos> getStart() {
            return start;
        }

        public Phase setStart(Optional<BlockPos> start) {
            this.start = start;
            return this;
        }

        public Phase setStart(@NotNull BlockPos start) {
            this.start = Optional.of(start);
            return this;
        }

        public Optional<BlockPos> getEnd() {
            return end;
        }

        public Phase setEnd(Optional<BlockPos> end) {
            this.end = end;
            return this;
        }

        public Phase setEnd(@NotNull BlockPos end) {
            this.end = Optional.of(end);
            return this;
        }

        public int getDuration() {
            return duration;
        }

        public Phase setDuration(int duration) {
            this.duration = duration;
            return this;
        }

        public int getTicks() {
            return ticks;
        }

        public Phase setTicks(int ticks) {
            this.ticks = ticks;
            return this;
        }

        // --

        public void tick() {
            ticks++;
        }

        public boolean isComplete() {
            return ticks >= duration;
        }

        public float getProgress() {
            return (float) getTicks() / getDuration();
        }

        public Optional<BlockPos> estimateCurrentPos() {
            if (getStart().isPresent() && getEnd().isPresent()) {
                return Optional.of(BlockPos.containing(Position.lerp(getStart().get(), getEnd().get(), getProgress())));
            }
            return Optional.empty();
        }

        // --

        public enum Type implements StringRepresentable {
            LEAVING_HOME("leaving_home"),
            TRAVELING_TO_TARGET("traveling_to_target"),
            APPROACHING_TARGET("approaching_target"),
            LEAVING_TARGET("leaving_target"),
            TRAVELING_TO_HOME("traveling_to_home"),
            APPROACHING_HOME("approaching_home");

            public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
            public static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
            public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Type::ordinal);

            private final String name;

            Type(String name) {
                this.name = name;
            }

            @Override
            public @NotNull String getSerializedName() {
                return name;
            }

            public boolean isTraveling() {
                return this == TRAVELING_TO_TARGET || this == TRAVELING_TO_HOME;
            }

            public boolean hasNext() {
                return this != APPROACHING_HOME;
            }

            public Type next(Delivery delivery) {
                return switch (this) {
                    case LEAVING_HOME -> delivery.shouldSkipTravelingPhase() ? APPROACHING_TARGET : TRAVELING_TO_TARGET;
                    case TRAVELING_TO_TARGET -> APPROACHING_TARGET;
                    case APPROACHING_TARGET -> LEAVING_TARGET;
                    case LEAVING_TARGET -> delivery.shouldSkipTravelingPhase() ? APPROACHING_HOME : TRAVELING_TO_HOME;
                    case TRAVELING_TO_HOME -> APPROACHING_HOME;
                    case APPROACHING_HOME -> throw new IllegalStateException("Delivery is finished.");
                    //TODO: find graceful delivery end instead of throwing an exception.
                };
            }
        }
    }
}
