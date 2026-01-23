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
    TRAVELING_FROM_SENDER_TO_HUB("traveling_from_sender_to_hub"),
    DISPATCHING_DELIVERY("dispatching_delivery"),
    TRAVELING_FROM_HUB_TO_RECIPIENT("traveling_from_hub_to_recipient"),
    APPROACHING_RECIPIENT("approaching_recipient"),
    HANDLING_DELIVERY("handling_delivery"),
    DEPARTING_RECIPIENT("departing_recipient"),
    TRAVELING_FROM_RECIPIENT_TO_HUB("traveling_from_recipient_to_hub"),
    DISPATCHING_RETURN("dispatching_return"),
    TRAVELING_FROM_HUB_TO_SENDER("traveling_from_hub_to_sender"),
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

    public boolean isTraveling() {
        return this == TRAVELING_FROM_SENDER_TO_HUB || this == DISPATCHING_DELIVERY || this == TRAVELING_FROM_HUB_TO_RECIPIENT
            || this == TRAVELING_FROM_RECIPIENT_TO_HUB || this == DISPATCHING_RETURN || this == TRAVELING_FROM_HUB_TO_SENDER;
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

    public boolean isOnRecipientSide() {
        return (this.ordinal() >= TRAVELING_FROM_HUB_TO_RECIPIENT.ordinal() && this.ordinal() <= DEPARTING_RECIPIENT.ordinal());
    }

    public boolean isSpawnable() {
        return !isTraveling() && this != DeliveryPhase.FINISHED;
    }

    public boolean isLast() {
        return ordinal() == values().length - 1;
    }

    public boolean hasNext() {
        return !isLast();
    }

    public DeliveryPhase next() {
        Preconditions.checkState(!isLast(), "There is no next phase.");
        return values()[ordinal() + 1];
    }

    @SuppressWarnings("deprecation")
    public String toPrettyString() {
        return WordUtils.capitalize(getSerializedName().replace('_', ' '));
    }
}