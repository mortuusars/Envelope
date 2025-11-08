package io.github.mortuusars.envelope.world.delivery.phase;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

public enum DeliveryPhase implements StringRepresentable {
    STARTED("started"),
    DEPARTING_SENDER("departing_sender"),
    LOCATING_RECIPIENT("locating_recipient"),
    TRAVELING_TO_RECIPIENT("traveling_to_recipient"),
    APPROACHING_RECIPIENT("approaching_recipient"),
    HANDLING_DELIVERY("handling_delivery"),
    DEPARTING_RECIPIENT("departing_recipient"),
    TRAVELING_TO_SENDER("traveling_to_sender"),
    APPROACHING_SENDER("approaching_sender"),
    HANDLING_RETURN("handling_return"),
    FINISHED("finished");

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

    public boolean isLast() {
        return ordinal() == values().length - 1;
    }

    public boolean hasNext() {
        return !isLast();
    }

    public boolean isTraveling() {
        return this == LOCATING_RECIPIENT || this == TRAVELING_TO_RECIPIENT || this == TRAVELING_TO_SENDER;
    }

    public boolean isReturning() {
        return ordinal() >= DEPARTING_RECIPIENT.ordinal() && this != FINISHED;
    }

    public boolean isAscending() {
        return this == DEPARTING_SENDER || this == DEPARTING_RECIPIENT;
    }

    public boolean isDescending() {
        return this == APPROACHING_RECIPIENT || this == APPROACHING_SENDER;
    }

    public DeliveryPhase next(boolean skipTraveling) {
        Preconditions.checkState(!isLast(), "There is no next phase.");

        if (skipTraveling) {
            for (int i = ordinal() + 1; i < values().length; i++) {
                DeliveryPhase phase = values()[i];
                if (!phase.isTraveling()) {
                    return phase;
                }
            }
        }

        return values()[ordinal() + 1];
    }

    @SuppressWarnings("deprecation")
    public String toPrettyString() {
        return WordUtils.capitalize(getSerializedName().replace('_', ' '));
    }
}