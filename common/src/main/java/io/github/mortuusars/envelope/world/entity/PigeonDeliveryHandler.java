package io.github.mortuusars.envelope.world.entity;

import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.DeliveryHandler;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;

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
            delivery.updateMail(mail -> mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.UNABLE_TO_REACH)));
            delivery.setPhaseAndResetProgress(DeliveryPhase.APPROACHING_SENDER);
            return;
        }

        if (delivery.getPhase() == DeliveryPhase.APPROACHING_RECIPIENT && !hasReachedSegmentEndPos(delivery)) {
            delivery.updateMail(mail -> mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .message(DeliveryRecord.Message.UNABLE_TO_REACH)));
            delivery.setPhaseAndResetProgress(DeliveryPhase.DEPARTING_RECIPIENT);
            return;
        }

        DeliveryHandler.super.advancePhase(level, delivery);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (!delivery.getMail().isEmpty()) {
            pigeon().spawnAtLocation(delivery.getMail().getItem().copy());
            Pigeon.LOGGER.info("{} has dropped undelivered mail on the ground.", pigeon().getName().getString());
            delivery.setMail(Mail.empty());
        }

        pigeon().setDelivery(null);

        if (pigeon().getOrigin().isService()) {
            pigeon().onVanished(level);
            pigeon().discard();
        } else {
            pigeon().setOrigin(null);
            pigeon().getPigeonholeHandler().setCurrentPos(pigeon().getPigeonholeHandler().getLastReleasePos());
            // Prevent Pigeon entering Pigeonhole immediately:
            pigeon().getPigeonholeHandler().setWantCooldown(40);
        }
    }

    protected boolean hasReachedSegmentEndPos(Delivery delivery) {
        return delivery.getRoute().getSegment(delivery.getPhase()).endPos()
              .map(pos -> pigeon().hasReachedTarget(pos))
              .orElse(true);
    }
}
