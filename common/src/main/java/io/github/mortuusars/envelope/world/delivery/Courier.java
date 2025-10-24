package io.github.mortuusars.envelope.world.delivery;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.*;
import io.github.mortuusars.envelope.world.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    Component getName();

    void handleUndeliveredMail(ServerLevel level, ItemStack mail);

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

        if (Envelope.debug()) LOGGER.info("{} started delivering '{}' from '{}' to '{}'", getName().getString(), delivery.getMail(),
              delivery.getSenderAddress().getDisplayName().getString(), delivery.getRecipientAddress().getDisplayName().getString());

        startDeliveryPhase(level, delivery);
        onDeliveryChanged(level);
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

        if (delivery.getPhase().getType() == Delivery.Phase.Type.APPROACHING_TARGET
              && !level.getEnvelopeContext().getKnownAddresses().isKnown(delivery.getTargetAddress())) {

            delivery.setPhase(delivery.getPhase()
                  .setType(Delivery.Phase.Type.TRAVELING_TO_HOME)
                  .setTicks(0));
            delivery.setMail(Mail.returnedRecipientNotFound(delivery.getMail()));

            if (Envelope.debug()) LOGGER.info("{}: returning: recipient not found.", toLoggableString());

            onDeliveryChanged(level);
        }
    }

    default void startDeliveryPhase(ServerLevel level, Delivery delivery) {
        if (Envelope.debug())
            LOGGER.info("{}: > '{}'", toLoggableString(), delivery.getPhase().getType().getSerializedName());
        updatePhasePositions(level, delivery);
    }

    default void endDeliveryPhase(ServerLevel level, Delivery delivery) {
        if (delivery.getMail().isEmpty()) {
            return;
        }

        switch (delivery.getPhase().getType()) {
            case APPROACHING_TARGET -> {
                if (Envelope.debug()) LOGGER.info("{}: delivered '{}'", toLoggableString(), delivery.getMail().getItem());

                ItemStack result = delivery.getTargetAddress().receiveMail(level, delivery.getMail());
                delivery.setMail(result);

                if (Envelope.debug() && !delivery.getMail().isEmpty())
                    LOGGER.info("{}: returning home with '{}'", toLoggableString(), delivery.getMail().getItem());
            }
            case APPROACHING_HOME -> {
                if (Envelope.debug()) LOGGER.info("{}: returned home with '{}'", toLoggableString(), delivery.getMail().getItem());

                ItemStack result = delivery.getTargetAddress().receiveMail(level, delivery.getMail());
                delivery.setMail(result);
            }
        }
    }

    default void endDelivery(ServerLevel level, Delivery delivery) {
        if (!delivery.getMail().isEmpty()) {
            handleUndeliveredMail(level, delivery.getMail());
            delivery.setMail(ItemStack.EMPTY);
        }
        if (Envelope.debug()) LOGGER.info("{}: finished.", toLoggableString());
        setDelivery(null);
    }

    default void onDeliveryChanged(ServerLevel level) {

    }

    // --

    default void updateAddressPositions(ServerLevel level, Delivery delivery) {
        Position.ofAddress(level, delivery.getSenderAddress()).ifPresent(pos -> delivery.setSenderPos(Optional.of(pos)));
        Position.ofAddress(level, delivery.getRecipientAddress()).ifPresent(pos -> delivery.setRecipientPos(Optional.of(pos)));
    }

    default void updatePhasePositions(ServerLevel level, Delivery delivery) {
        final Optional<BlockPos> recipientPos = delivery.getRecipientPos();
        final Optional<BlockPos> senderPos = delivery.getSenderPos();
        final int distance = getAscendPosDistance();

        Optional<BlockPos> startPos = switch (delivery.getPhase().getType()) {
            case LEAVING_HOME -> senderPos;
            case TRAVELING_TO_TARGET -> Position.ascendTowards(level, senderPos, recipientPos, distance);
            case APPROACHING_TARGET -> Position.ascendTowards(level, recipientPos, senderPos, distance);
            case LEAVING_TARGET -> recipientPos;
            case TRAVELING_TO_HOME -> Position.ascendTowards(level, recipientPos, senderPos, distance);
            case APPROACHING_HOME -> Position.ascendTowards(level, senderPos, recipientPos, distance);
        };

        Optional<BlockPos> endPos = switch (delivery.getPhase().getType()) {
            case LEAVING_HOME -> Position.ascendTowards(level, senderPos, recipientPos, distance);
            case TRAVELING_TO_TARGET -> Position.ascendTowards(level, recipientPos, senderPos, distance);
            case APPROACHING_TARGET -> recipientPos;
            case LEAVING_TARGET -> Position.ascendTowards(level, recipientPos, senderPos, distance);
            case TRAVELING_TO_HOME -> Position.ascendTowards(level, senderPos, recipientPos, distance);
            case APPROACHING_HOME -> senderPos;
        };

        delivery.getPhase().setStart(startPos);
        delivery.getPhase().setEnd(endPos);
    }

    default int getAscendPosDistance() {
        return 10;
    }

    default String toLoggableString() {
        return getDelivery()
              .map(delivery -> getName().getString() + " [" + delivery.toShortString() + "]")
              .orElseGet(() -> getName().getString());
    }

    default boolean isInSafeSimulationDistance(ServerLevel level, BlockPos pos) {
        int simDistance = level.getServer().getPlayerList().getSimulationDistance();
        int range = simDistance - 1; // Reduce by 1 chunk to be safe.
        return level.players().stream().anyMatch(player -> {
            double dx = Math.abs(pos.getX() - player.getX()) / 16.0;
            double dz = Math.abs(pos.getZ() - player.getZ()) / 16.0;
            return Math.max(dx, dz) <= range;
        });
    }
}
