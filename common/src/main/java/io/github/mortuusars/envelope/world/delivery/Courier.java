package io.github.mortuusars.envelope.world.delivery;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.item.component.MailDeliveryLog;
import io.github.mortuusars.envelope.world.pigeonhole.PigeonholeManager;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface Courier {
    @Nullable
    Delivery getDelivery();

    void setDelivery(@Nullable Delivery delivery);

    Optional<BlockPos> getCurrentPos();

    void startDeliveryPhase(ServerLevel level);

    void endDeliveryPhase(ServerLevel level);

    default @NotNull Delivery getDeliveryOrThrow() {
        Preconditions.checkNotNull(getDelivery(), "Courier is not delivering.");
        return getDelivery();
    }

    default boolean isDelivering() {
        return getDelivery() != null;
    }

    default void startDelivery(ServerLevel level, ItemStack mail, @Nullable BlockPos homePos) {
        mail.remove(Envelope.DataComponents.MAIL_DELIVERY_LOG); // Remove previous log before new send

        MailDeliveryLog.addRecords(mail,
            MailDeliveryLog.TravelingRecord.sentFrom(mail.getOrDefault(Envelope.DataComponents.MAIL_SENDER, Address.UNKNOWN)).atTime(level.getGameTime()),
            MailDeliveryLog.TravelingRecord.travelingTo(mail.getOrDefault(Envelope.DataComponents.MAIL_RECIPIENT, Address.UNKNOWN)));

        setDelivery(Delivery.start(level, mail).setSenderPos(Optional.ofNullable(homePos)));
        startDeliveryPhase(level);
        onDeliveryChanged(level);

        Envelope.LOGGER.debug("Starting delivery '{}'", getDeliveryOrThrow().createSenderToRecipientComponent("➡"));
    }

    default void advanceDeliveryPhase(ServerLevel level) {
        Preconditions.checkNotNull(getDelivery());
        getDelivery().advancePhase();
    }

    default void tickDelivery(ServerLevel level) {
        if (getDelivery() == null) return;

        getDelivery().getPhase().tick();

        if (getDelivery().getPhase().isComplete()) {
            endDeliveryPhase(level);

            if (getDelivery().getPhase().getType().hasNext()) {
                advanceDeliveryPhase(level);
                updateAddressPositions(level, getDelivery());
                startDeliveryPhase(level);
            } else {
                Envelope.LOGGER.debug("Delivery '{}' is finished.", getDeliveryOrThrow().createSenderToRecipientComponent("➡"));
                setDelivery(null);
            }

            onDeliveryChanged(level);
        }
    }

    default void onDeliveryChanged(ServerLevel level) {

    }

    default boolean tryDeliverMail(ServerLevel level, ItemStack mail, Address address) {
        return address.map(pigeonhole -> {
                PigeonholeManager pigeonholeManager = level.getEnvelopePigeonholeManager();
                if (pigeonholeManager.putMail(pigeonhole, mail)) {
                    MailDeliveryLog.addRecords(mail, MailDeliveryLog.TravelingRecord.arrivedTo(pigeonhole));

                    pigeonholeManager.getPositionOf(pigeonhole).ifPresent(pos -> {
                        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                            blockEntity.onMailDelivered(level, mail);
                        }
                    });

                    return true;
                }
                return false;
            },
            player -> level.getEnvelopePlayerInformation().getDefaultAddress().of(player)
                .map(pigeonholeAddress -> tryDeliverMail(level, mail, pigeonholeAddress))
                .orElse(false),
            npc -> {
                throw new NotImplementedException("NPC addresses are not implemented yet");
            });
    }

    // --

    static void updateAddressPositions(ServerLevel level, Delivery delivery) {
        Position.ofAddress(level, delivery.getSender()).ifPresent(pos -> delivery.setSenderPos(Optional.of(pos)));
        Position.ofAddress(level, delivery.getRecipient()).ifPresent(pos -> delivery.setRecipientPos(Optional.of(pos)));
    }
}
