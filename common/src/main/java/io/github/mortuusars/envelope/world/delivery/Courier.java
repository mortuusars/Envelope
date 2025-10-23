package io.github.mortuusars.envelope.world.delivery;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.*;
import io.github.mortuusars.envelope.world.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

public interface Courier {
    Logger LOGGER = LogUtils.getLogger();

    @Nullable
    Delivery delivery();

    void setDelivery(@Nullable Delivery delivery);

    String getCourierName();

    default Optional<Delivery> getDelivery() {
        return Optional.ofNullable(delivery());
    }

    Optional<BlockPos> getCurrentPos();

    default boolean isDelivering() {
        return delivery() != null;
    }

    default void startDelivery(ServerLevel level, ItemStack mail) {
        Delivery delivery = Delivery.start(level, Mail.sent(mail, level));

        setDelivery(delivery);
        startDeliveryPhase(level, delivery);
        onDeliveryChanged(level);

        if (Envelope.debug())
            LOGGER.info("{}[{}]: started delivering '{}'", getCourierName(), delivery.getMail(), delivery.toShortString());
    }

    default void tickDelivery(ServerLevel level, Delivery delivery) {
        delivery.getPhase().tick();

        if (delivery.getPhase().isComplete()) {
            endDeliveryPhase(level, delivery);

            if (delivery.getPhase().getType().hasNext()) {
                advanceDeliveryPhase(level, delivery);
                updateAddressPositions(level, delivery);
                startDeliveryPhase(level, delivery);
            } else {
                endDelivery(level, delivery);
            }

            onDeliveryChanged(level);
        }
    }

    default void advanceDeliveryPhase(ServerLevel level, Delivery delivery) {
        delivery.advancePhase();
    }

    default void startDeliveryPhase(ServerLevel level, Delivery delivery) {
        if (Envelope.debug())
            LOGGER.info("{}[{}]: starting phase '{}'", getCourierName(), delivery.toShortString(), delivery.getPhase().getType().getSerializedName());
        updatePhasePositions(level, delivery);
    }

    default void endDeliveryPhase(ServerLevel level, Delivery delivery) {
        switch (delivery.getPhase().getType()) {
            case APPROACHING_TARGET, APPROACHING_HOME -> {
                if (Envelope.debug() && !delivery.getMail().isEmpty())
                    LOGGER.info("{}[{}]: dropping off '{}'", getCourierName(), delivery.toShortString(), delivery.getMail().toString());

                ItemStack result = delivery.getTargetAddress().receiveMail(level, delivery.getMail());
                delivery.setMail(result);

                if (Envelope.debug() && delivery.getPhase().getType().hasNext() && !delivery.getMail().isEmpty())
                    LOGGER.info("{}[{}]: returning home with '{}'", getCourierName(), delivery.toShortString(), delivery.getMail().toString());
            }
        }
    }

    default void endDelivery(ServerLevel level, Delivery delivery) {
        if (!delivery.getMail().isEmpty()) {
            handleUndeliveredMail(level, delivery);
        }
        LOGGER.debug("{}[{}]: finished.", getCourierName(), delivery.toShortString());
        setDelivery(null);
    }

    default void handleUndeliveredMail(ServerLevel level, Delivery delivery) {

    }

    default void onDeliveryChanged(ServerLevel level) {

    }

    // --

    default void updateAddressPositions(ServerLevel level, Delivery delivery) {
        Position.ofAddress(level, delivery.getSenderAddress()).ifPresent(pos -> delivery.setSenderPos(Optional.of(pos)));
        Position.ofAddress(level, delivery.getRecipientAddress()).ifPresent(pos -> delivery.setRecipientPos(Optional.of(pos)));
    }

    default void updatePhasePositions(ServerLevel level, Delivery delivery) {
        final Optional<BlockPos> currentPos = getCurrentPos();
        final Optional<BlockPos> recipientPos = delivery.getRecipientPos();
        final Optional<BlockPos> senderPos = delivery.getSenderPos();
        final int distance = getAscendPosDistance();

        delivery.getPhase().setStart(currentPos);

        Optional<BlockPos> endPos = switch (delivery.getPhase().getType()) {
            case LEAVING_HOME -> Position.ascendTowards(level, currentPos, recipientPos, distance);
            case TRAVELING_TO_TARGET -> Position.ascendTowards(level, recipientPos, currentPos, distance);
            case APPROACHING_TARGET -> recipientPos;
            case LEAVING_TARGET -> Position.ascendTowards(level, currentPos, senderPos, distance);
            case TRAVELING_TO_HOME -> Position.ascendTowards(level, senderPos, currentPos, distance);
            case APPROACHING_HOME -> senderPos;
        };

        delivery.getPhase().setEnd(endPos);
    }

    default int getAscendPosDistance() {
        return 10;
    }
}
