package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.DeliveryHandler;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class PigeonDeliverMailGoal extends Goal implements DeliveryHandler {
    protected final Pigeon pigeon;

    public PigeonDeliverMailGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public Pigeon pigeon() {
        return pigeon;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return pigeon.isDelivering();
    }

    @Override
    public void stop() {
        pigeon.setDelivery(null);
        pigeon.getNavigation().stop();
        pigeon.getNavigation().resetMaxVisitedNodesMultiplier();
    }

    @Override
    public void tick() {
        if (!(pigeon.level() instanceof ServerLevel level)) {
            return;
        }

        pigeon.getDelivery().ifPresent(delivery -> {
            delivery.tick(level, pigeon.getDeliveryHandler());

            delivery.getRoute().getSegment(delivery.getCurrentPhase()).endPos()
                  .ifPresent(pos -> {
                      if ((delivery.getCurrentPhase().isAscending() || delivery.getCurrentPhase().isDescending())
                            && pigeon.hasReachedTarget(pos)) {
                          delivery.getProgress().complete();
                      } else if (!pigeon.getNavigation().isInProgress() || !pos.equals(pigeon.getNavigation().getTargetPos())) {
                          if (!pigeon.pathfindDirectlyTowards(pos)) {
                              pigeon.pathfindRandomlyTowards(pos);
                          }
                      }
                  });
        });
    }

    @Override
    public int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            // Longer approach/depart phases to allow for pathfinding to finish
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> (int)Ticks.fromSeconds(30);
            default -> DeliveryHandler.super.getPhaseDuration(level, delivery, phase);
        };
    }

    @Override
    public void phaseStarted(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        DeliveryHandler.super.phaseStarted(level, delivery, phase);
        if (phase.isTraveling()) {
            pigeon().transitionToBackground(level);
        }
        pigeon().onDeliveryChanged();
    }

    @Override
    public DeliveryPhase advancePhase(ServerLevel level, Delivery delivery, DeliveryPhase currentPhase) {
        if (currentPhase == DeliveryPhase.DEPARTING_SENDER && !hasReachedSegmentEndPos(delivery)) {
            delivery.updateMail(mail -> mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .withMessage(DeliveryRecord.Message.UNABLE_TO_REACH)));
            return DeliveryPhase.APPROACHING_SENDER;
        }

        if (currentPhase == DeliveryPhase.APPROACHING_RECIPIENT && !hasReachedSegmentEndPos(delivery)) {
            delivery.updateMail(mail -> mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                  .withMessage(DeliveryRecord.Message.UNABLE_TO_REACH)));
            return DeliveryPhase.DEPARTING_RECIPIENT;
        }

        return DeliveryHandler.super.advancePhase(level, delivery, currentPhase);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (!delivery.getMail().isEmpty()) {
            pigeon().spawnAtLocation(delivery.getMail().getItemCopy());
            delivery.setMail(Mail.EMPTY);
            Pigeon.LOGGER.info("{} has dropped undelivered mail on the ground because it cannot be delivered to sender Pigeonhole.",
                  pigeon().getName().getString());
        }

        pigeon().setDelivery(null);

        if (delivery.getOrigin().isService()) {
            pigeon().onVanished(level);
            pigeon().discard();
        } else {
            pigeon().getPigeonholeHandler().setCurrentPos(pigeon().getPigeonholeHandler().getLastReleasePos());
            // Prevent Pigeon entering Pigeonhole immediately:
            pigeon().getPigeonholeHandler().setWantCooldown(40);
        }
    }

    protected boolean hasReachedSegmentEndPos(Delivery delivery) {
        return delivery.getRoute().getSegment(delivery.getCurrentPhase()).endPos()
              .map(pos -> pigeon().hasReachedTarget(pos))
              .orElse(true);
    }
}