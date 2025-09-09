package io.github.mortuusars.envelope.world.entity;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.mail.log.MailDeliveryLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.world.PigeonholeNetwork;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface DeliveringPigeon {
    @Nullable Delivery getDelivery();
    void setDelivery(@Nullable Delivery delivery);

    Optional<BlockPos> getCurrentPos();

    void startDeliveryPhase(ServerLevel level);
    void endDeliveryPhase(ServerLevel level);

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
                setDelivery(null);
            }
        }
    }

    default boolean tryDeliverMail(ServerLevel level, ItemStack mail, Address address) {
        return address.map(pigeonhole -> {
                    PigeonholeNetwork pigeonholeNetwork = PigeonholeNetwork.get(level);
                    if (pigeonholeNetwork.putMail(pigeonhole, mail)) {
                        MailDeliveryLog.addRecords(mail, TravelingRecord.arrivedTo(pigeonhole));

                        pigeonholeNetwork.getPositionOf(pigeonhole).ifPresent(pos -> {
                            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                                blockEntity.onMailDelivered(level, mail);
                            }
                        });

                        return true;
                    }
                    return false;
                },
                player -> {
                    throw new NotImplementedException("Player addresses are not implemented yet");
                },
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
