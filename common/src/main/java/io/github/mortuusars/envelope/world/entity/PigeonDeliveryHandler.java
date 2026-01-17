package io.github.mortuusars.envelope.world.entity;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.DeliveryHandler;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.item.component.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class PigeonDeliveryHandler implements DeliveryHandler {
    private final Pigeon pigeon;

    public PigeonDeliveryHandler(Pigeon pigeon) {
        this.pigeon = pigeon;
    }

    public Pigeon pigeon() {
        return pigeon;
    }

    @Override
    public int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            // Longer approach/depart phases to allow for pathfinding to finish
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> 30 * Ticks.SECOND;
            default -> DeliveryHandler.super.getPhaseDuration(level, delivery, phase);
        };
    }

    @Override
    public void phaseStarted(ServerLevel level, Delivery delivery) {
        DeliveryHandler.super.phaseStarted(level, delivery);
        if (delivery.getPhase().isTraveling()) {
            pigeon().transitionToBackground(level);
        }
        pigeon().onDeliveryChanged();
    }

    @Override
    public void advancePhase(ServerLevel level, Delivery delivery) {
        if (delivery.getPhase() == DeliveryPhase.DEPARTING_SENDER && !hasReachedSegmentEndPos(delivery)) {
            Mail.writeToLog(delivery.getMail(), DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.UNABLE_TO_REACH));
            delivery.setPhaseAndResetProgress(DeliveryPhase.APPROACHING_SENDER);
            return;
        }

        if (delivery.getPhase() == DeliveryPhase.APPROACHING_RECIPIENT && !hasReachedSegmentEndPos(delivery)) {
            Mail.writeToLog(delivery.getMail(), DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.UNABLE_TO_REACH));
            delivery.setPhaseAndResetProgress(DeliveryPhase.DEPARTING_RECIPIENT);
            return;
        }

        DeliveryHandler.super.advancePhase(level, delivery);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (!delivery.getMail().isEmpty()) {
            pigeon().spawnAtLocation(delivery.getMail().copy());
            Pigeon.LOGGER.info("{} has dropped undelivered mail on the ground.", pigeon().getName().getString());
            delivery.setMail(ItemStack.EMPTY);
        }

        pigeon().setDelivery(null);

        if (pigeon().getOrigin().isService()) {
            pigeon().onVanished(level);
            pigeon().discard();
        } else {
            pigeon().setOrigin(null);
            pigeon().getPigeonholeHandler().setTargetPos(pigeon().getPigeonholeHandler().getLastReleasePos());
            // Prevent Pigeon entering Pigeonhole immediately:
            pigeon().getPigeonholeHandler().setWantCooldown(20);
            pigeon().setTiredTicks(Config.Server.PIGEON_TIRED_AFTER_DELIVERY_TICKS.get());
        }
    }

    protected boolean hasReachedSegmentEndPos(Delivery delivery) {
        return delivery.getRoute().getSegment(delivery.getPhase()).endPos()
              .map(pos -> pigeon().hasReachedTarget(pos))
              .orElse(true);
    }
}
