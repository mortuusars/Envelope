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
import java.util.function.IntFunction;

public class PigeonDelivery {
    public static final int DEFAULT_PHASE_DURATION_TICKS = 200; // 10s

    public static final Codec<PigeonDelivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("home_pos", null).forGetter(PigeonDelivery::getHomePos),
            Address.CODEC.fieldOf("sender").forGetter(PigeonDelivery::getRecipient),
            Address.CODEC.fieldOf("recipient").forGetter(PigeonDelivery::getRecipient),
            Codec.INT.fieldOf("traveling_duration").forGetter(PigeonDelivery::getTravelingDurationTicks),
            ItemStack.OPTIONAL_CODEC.fieldOf("mail").forGetter(PigeonDelivery::getMail),
            DeliveryPhase.CODEC.fieldOf("phase").forGetter(PigeonDelivery::getCurrentPhase),
            BlockPos.CODEC.optionalFieldOf("phase_start_pos", null).forGetter(PigeonDelivery::getPhaseStartPos),
            BlockPos.CODEC.optionalFieldOf("phase_end_pos", null).forGetter(PigeonDelivery::getPhaseEndPos),
            Codec.INT.fieldOf("phase_duration_ticks").forGetter(PigeonDelivery::getPhaseDurationTicks),
            Codec.INT.fieldOf("phase_ticks").forGetter(PigeonDelivery::getPhaseTicks)
    ).apply(instance, PigeonDelivery::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PigeonDelivery> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull PigeonDelivery decode(RegistryFriendlyByteBuf buffer) {
            return new PigeonDelivery(
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buffer).orElse(null),
                    Address.STREAM_CODEC.decode(buffer),
                    Address.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    DeliveryPhase.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buffer).orElse(null),
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buffer).orElse(null),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PigeonDelivery data) {
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buffer, Optional.ofNullable(data.getHomePos()));
            Address.STREAM_CODEC.encode(buffer, data.getSender());
            Address.STREAM_CODEC.encode(buffer, data.getRecipient());
            ByteBufCodecs.VAR_INT.encode(buffer, data.getTravelingDurationTicks());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, data.getMail());
            DeliveryPhase.STREAM_CODEC.encode(buffer, data.getCurrentPhase());
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buffer, Optional.ofNullable(data.getPhaseStartPos()));
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buffer, Optional.ofNullable(data.getPhaseEndPos()));
            ByteBufCodecs.VAR_INT.encode(buffer, data.getPhaseDurationTicks());
            ByteBufCodecs.VAR_INT.encode(buffer, data.getPhaseTicks());
        }
    };

    public static final EntityDataSerializer<PigeonDelivery> ENTITY_DATA_SERIALIZER = new EntityDataSerializer<>() {
        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, PigeonDelivery> codec() {
            return STREAM_CODEC;
        }

        public @NotNull PigeonDelivery copy(PigeonDelivery data) {
            return new PigeonDelivery(data.getHomePos(), data.getSender(), data.getRecipient(),
                    data.getTravelingDurationTicks(), data.getMail().copy(), data.getCurrentPhase(),
                    data.getPhaseStartPos(), data.getPhaseEndPos(), data.getPhaseDurationTicks(), data.getPhaseTicks());
        }
    };

    public static final PigeonDelivery EMPTY = new PigeonDelivery(null, Address.UNKNOWN, Address.UNKNOWN,
            0, ItemStack.EMPTY, DeliveryPhase.LEFT_HOME,
            null, null, 0, 0);

    protected final @Nullable BlockPos homePos;
    protected final Address sender;
    protected final Address recipient;
    protected final int travelingDurationTicks;
    protected ItemStack mail;
    protected DeliveryPhase currentPhase;
    protected @Nullable BlockPos phaseStartPos;
    protected @Nullable BlockPos phaseEndPos;
    protected int phaseDurationTicks;
    protected int phaseTicks;

    public PigeonDelivery(@Nullable BlockPos homePos, Address sender, Address recipient, int travelingDurationTicks,
                          ItemStack mail, DeliveryPhase currentPhase, @Nullable BlockPos phaseStartPos, @Nullable BlockPos phaseEndPos,
                          int phaseDurationTicks, int phaseTicks) {
        this.homePos = homePos;
        this.sender = sender;
        this.recipient = recipient;
        this.travelingDurationTicks = travelingDurationTicks;
        this.mail = mail;
        this.currentPhase = currentPhase;
        this.phaseStartPos = phaseStartPos;
        this.phaseEndPos = phaseEndPos;
        this.phaseDurationTicks = phaseDurationTicks;
        this.phaseTicks = phaseTicks;
    }

    public static PigeonDelivery start(@Nullable BlockPos homePos, Address sender, Address recipient, int travelingDurationTicks, @NotNull ItemStack mail) {
        return new PigeonDelivery(homePos, sender, recipient, travelingDurationTicks, mail,
                DeliveryPhase.LEFT_HOME, null, null, DEFAULT_PHASE_DURATION_TICKS, 0);
    }

    public PigeonDelivery setMail(ItemStack mail) {
        this.mail = mail;
        return this;
    }

    public PigeonDelivery setPhaseStartPos(@Nullable BlockPos phaseStartPos) {
        this.phaseStartPos = phaseStartPos;
        return this;
    }

    public PigeonDelivery setPhaseEndPos(@Nullable BlockPos phaseEndPos) {
        this.phaseEndPos = phaseEndPos;
        return this;
    }

    // --

    public @Nullable BlockPos getHomePos() {
        return homePos;
    }

    public Address getSender() {
        return sender;
    }

    public Address getRecipient() {
        return recipient;
    }

    public int getTravelingDurationTicks() {
        return travelingDurationTicks;
    }

    public ItemStack getMail() {
        return mail;
    }

    public DeliveryPhase getCurrentPhase() {
        return currentPhase;
    }

    public @Nullable BlockPos getPhaseStartPos() {
        return phaseStartPos;
    }

    public @Nullable BlockPos getPhaseEndPos() {
        return phaseEndPos;
    }

    public int getPhaseDurationTicks() {
        return phaseDurationTicks;
    }

    public int getPhaseTicks() {
        return phaseTicks;
    }

    // --

    public boolean isEmpty() {
        return this.equals(EMPTY) || (getSender().equals(Address.UNKNOWN) && getRecipient().equals(Address.UNKNOWN));
    }

    public boolean tick() {
        if (isEmpty()) return false;
        phaseTicks++;
        return phaseTicks >= phaseDurationTicks;
    }

    public PigeonDelivery advancePhase() {
        Preconditions.checkState(currentPhase != DeliveryPhase.APPROACHING_HOME, "Cannot advance past the last phase. Delivery is finished.");
        currentPhase = currentPhase.next();
        phaseStartPos = getPhaseStartPos();
        phaseEndPos = null;
        phaseDurationTicks = currentPhase.isTraveling() ? travelingDurationTicks : DEFAULT_PHASE_DURATION_TICKS;
        phaseTicks = 0;
        return this;
    }

    // --

    public enum DeliveryPhase implements StringRepresentable {
        LEFT_HOME("left_home"),
        TRAVELING_TO_TARGET("traveling_to_target"),
        APPROACHING_TARGET("approaching_target"),
        LEFT_TARGET("left_target"),
        TRAVELING_TO_HOME("traveling_to_home"),
        APPROACHING_HOME("approaching_home");

        public static final Codec<DeliveryPhase> CODEC = StringRepresentable.fromEnum(DeliveryPhase::values);
        public static final IntFunction<DeliveryPhase> BY_ID = ByIdMap.continuous(DeliveryPhase::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, DeliveryPhase> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DeliveryPhase::ordinal);

        private final String name;

        DeliveryPhase(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public static Optional<DeliveryPhase> byName(String name) {
            for (DeliveryPhase value : values()) {
                if (value.getSerializedName().equals(name)) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }

        public DeliveryPhase next() {
            return values()[ordinal() + 1];
        }

        public boolean isTraveling() {
            return this == TRAVELING_TO_TARGET || this == TRAVELING_TO_HOME;
        }
    }
}
