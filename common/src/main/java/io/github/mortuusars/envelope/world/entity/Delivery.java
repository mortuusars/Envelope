package io.github.mortuusars.envelope.world.entity;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.mail.Address;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

public class Delivery {
    public static final Codec<Delivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("mail").forGetter(Delivery::getMail),
            Address.CODEC.optionalFieldOf("sender", Address.UNKNOWN).forGetter(Delivery::getRecipient),
            Address.CODEC.fieldOf("recipient").forGetter(Delivery::getRecipient),
            Codec.INT.optionalFieldOf("travel_duration", -1).forGetter(Delivery::getTravelDuration),
            BlockPos.CODEC.optionalFieldOf("home_pos").forGetter(Delivery::getHomePos),
            Phase.CODEC.optionalFieldOf("phase", Phase.BEGINNING).forGetter(Delivery::getCurrentPhase)
    ).apply(instance, Delivery::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Delivery> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, Delivery::getMail,
            Address.STREAM_CODEC, Delivery::getSender,
            Address.STREAM_CODEC, Delivery::getRecipient,
            ByteBufCodecs.VAR_INT, Delivery::getTravelDuration,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Delivery::getHomePos,
            Phase.STREAM_CODEC, Delivery::getCurrentPhase,
            Delivery::new
    );

    public static final EntityDataSerializer<Delivery> ENTITY_DATA_SERIALIZER = new EntityDataSerializer<>() {
        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, Delivery> codec() {
            return STREAM_CODEC;
        }

        public @NotNull Delivery copy(Delivery data) {
            return new Delivery(data.getMail().copy(), data.getSender(), data.getRecipient(), data.getTravelDuration(), data.getHomePos(), data.getCurrentPhase().copy());
        }
    };

    public static final Delivery EMPTY = new Delivery(ItemStack.EMPTY, Address.UNKNOWN, Address.UNKNOWN, -1, Optional.empty(),
            new Phase(Phase.Type.LEAVING_HOME, Optional.empty(), Optional.empty(), 1));

    protected ItemStack mail;
    protected Address sender;
    protected Address recipient;
    protected int travelDuration;
    protected Optional<BlockPos> homePos;
    protected Phase phase;
    protected int ticksAtCurrentPhase = 0;

    public Delivery(ItemStack mail, Address sender, Address recipient, int travelDuration, Optional<BlockPos> homePos, Phase phase) {
        this.mail = mail;
        this.sender = sender;
        this.recipient = recipient;
        this.travelDuration = travelDuration;
        this.homePos = homePos;
        this.phase = phase;
    }

    public Delivery(ItemStack mail, Address sender, Address recipient, int travelDuration, Optional<BlockPos> homePos) {
        this(mail, sender, recipient, travelDuration, homePos, Phase.BEGINNING);
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

    public Address getRecipient() {
        return recipient;
    }

    public Delivery setRecipient(Address recipient) {
        this.recipient = recipient;
        return this;
    }

    public int getTravelDuration() {
        return travelDuration;
    }

    public Delivery setTravelDuration(int travelDuration) {
        this.travelDuration = travelDuration;
        return this;
    }

    public Optional<BlockPos> getHomePos() {
        return homePos;
    }

    public Delivery setHomePos(Optional<BlockPos> homePos) {
        this.homePos = homePos;
        return this;
    }

    public @NotNull Delivery.Phase getCurrentPhase() {
        return phase;
    }

    public Delivery setPhase(Phase phase) {
        this.phase = phase;
        return this;
    }

    // --

    public boolean isEmpty() {
        return this.equals(EMPTY) || getRecipient().equals(Address.UNKNOWN);
    }

    public boolean tick() {
        if (isEmpty()) return false;
        ticksAtCurrentPhase++;
        return ticksAtCurrentPhase >= getCurrentPhase().getDuration();
    }

    public Delivery updatePhase(Function<Phase, Phase> oldToNew) {
        phase = oldToNew.apply(phase);
        return this;
    }

    public Delivery resetTimer() {
        ticksAtCurrentPhase = 0;
        return this;
    }

    // --

    public record Phase(Delivery.Phase.Type type, Optional<BlockPos> start, Optional<BlockPos> end, int durationTicks) {
        public static final int DEFAULT_DURATION_TICKS = 100; // 5s

        public static final Codec<Phase> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Type.CODEC.optionalFieldOf("phase", Type.LEAVING_HOME).forGetter(Phase::type),
                BlockPos.CODEC.optionalFieldOf("start_pos").forGetter(Phase::start),
                BlockPos.CODEC.optionalFieldOf("end_pos").forGetter(Phase::end),
                Codec.INT.optionalFieldOf("duration", DEFAULT_DURATION_TICKS).forGetter(Phase::getDuration)
        ).apply(instance, Phase::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Phase> STREAM_CODEC = StreamCodec.composite(
                Type.STREAM_CODEC, Phase::type,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Phase::start,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), Phase::end,
                ByteBufCodecs.VAR_INT, Phase::getDuration,
                Phase::new
        );

        public static final Phase BEGINNING = new Phase(Type.LEAVING_HOME, Optional.empty(), Optional.empty(), DEFAULT_DURATION_TICKS);

        public int getDuration() {
            return durationTicks;
        }

        // --

        public Phase next() {
            return new Phase(type.next(), end, Optional.empty(), DEFAULT_DURATION_TICKS);
        }

        public Phase ofType(Type type) {
            return new Phase(type, start, end, durationTicks);
        }

        public Phase start(@Nullable BlockPos start) {
            return new Phase(type, Optional.ofNullable(start), end, durationTicks);
        }

        public Phase end(@Nullable BlockPos end) {
            return new Phase(type, start, Optional.ofNullable(end), durationTicks);
        }

        public Phase duration(int durationTicks) {
            return new Phase(type, start, end, durationTicks);
        }

        public Phase copy() {
            return new Phase(type, start, end, durationTicks);
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

            public static Optional<Type> byName(String name) {
                for (Type value : values()) {
                    if (value.getSerializedName().equals(name)) {
                        return Optional.of(value);
                    }
                }
                return Optional.empty();
            }

            public Type next() {
                Preconditions.checkState(this != Type.APPROACHING_HOME,
                        "Cannot advance past the last stage. Delivery is finished.");
                return values()[ordinal() + 1];
            }

            public boolean isTraveling() {
                return this == TRAVELING_TO_TARGET || this == TRAVELING_TO_HOME;
            }
        }
    }
}
