package io.github.mortuusars.envelope.world.mail.delivery;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.ai.CourierNavigation;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableEntityData;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.mortaar.util.Ticks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface PhysicalCourier extends Courier {
    Level level();

    Vec3 position();

    BlockPos blockPosition();

    PathNavigation getNavigation();

    SpawnableEntityData toSpawnableCourierData();

    MailboxHandler getMailboxHandler();

    void setMailboxHandler(MailboxHandler mailboxHandler);

    void setDelivery(Delivery delivery);

    default boolean canStartDelivery() {
        return true;
    }

    default void startDelivery(Delivery delivery) {
        if (getCurrentDelivery().isPresent()) {
            LOGGER.warn("Starting new delivery when the courier is already delivering. This might be an error.");
        }
        setDelivery(delivery);
        asCourierEntity().stopRiding();
    }

    @Override
    default int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            // Longer approach/depart phases to allow for pathfinding to finish
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER ->
                  Mth.ceil(Ticks.fromSeconds(40) * (Config.Server.DELIVERY_ASCEND_DISTANCE.get() / 24f));
            default -> Courier.super.getPhaseDuration(level, delivery, phase);
        };
    }

    @Override
    default void phaseStarted(ServerLevel level, Delivery delivery) {
        Courier.super.phaseStarted(level, delivery);
        if (delivery.getPhase().isTraveling()) {
            transitionToBackground(level);
        }
        onDeliveryChanged();
    }

    default void onDeliveryChanged() {
    }

    @Override
    default boolean handlePhaseTransition(ServerLevel level, Delivery delivery) {
        if (delivery.getPhase() == DeliveryPhase.DEPARTING_SENDER && !hasReachedSegmentEndPos(delivery)) {
            Mail.returned(delivery.getMail(), DeliveryRecord.Message.UNABLE_TO_REACH);
            delivery.beginPhase(DeliveryPhase.APPROACHING_SENDER);
            return true;
        }

        if (delivery.getPhase() == DeliveryPhase.APPROACHING_RECIPIENT && !hasReachedSegmentEndPos(delivery)) {
            Mail.returned(delivery.getMail(), DeliveryRecord.Message.UNABLE_TO_REACH);
            delivery.beginPhase(DeliveryPhase.DEPARTING_RECIPIENT);
            return true;
        }

        return Courier.super.handlePhaseTransition(level, delivery);
    }

    default void diedWhileDelivering(ServerLevel level, DamageSource damageSource, Delivery delivery) {
        PathfinderMob entity = asCourierEntity();

        String message = damageSource.getLocalizedDeathMessage(entity).getString();
        String carriedItem = !delivery.getMail().isEmpty()
              ? " a " + delivery.getMail().getHoverName().getString()
              : "";
        String addresses = delivery.getSender().getString()
              + " to "
              + delivery.getRecipient().getString();
        String service = getCourierOrigin().isService() ? "Service " : "";
        Envelope.LOGGER.info("{}{} at [{}] while delivering{} from {}!", service, message, blockPosition().toShortString(), carriedItem, addresses);

        if (shouldSendDeathNotice()) {
            MailService.of(level).sendCourierDeathNotice(entity, delivery, damageSource);
        }

        if (!delivery.getMail().isEmpty()) {
            ItemStack mail = delivery.getPhase().isOnRecipientSide()
                  ? Mail.asDelivered(delivery.getMail())
                  : delivery.getMail();
            entity.spawnAtLocation(mail);
            delivery.setMail(ItemStack.EMPTY);
        }
    }

    default boolean shouldSendDeathNotice() {
        return !getCourierOrigin().isService();
    }

    default boolean canEat(ItemStack food) {
        return asCourierEntity() instanceof Animal animal && animal.isFood(food);
    }

    default BackgroundCourier transitionToBackground(ServerLevel level) {
        Delivery delivery = getCurrentDelivery().orElseThrow(() -> new IllegalStateException("Cannot transition: courier is not delivering."));
        BackgroundCourier backgroundCourier = new BackgroundCourier(toSpawnableCourierData(), getCourierOrigin(), delivery);
        MailService.of(level).getBackgroundDelivery().addCourier(backgroundCourier);
        onVanished(level);
        ((Entity) this).discard();
        return backgroundCourier;
    }

    default void onAppeared(ServerLevel level) {
        Vec3 pos = ((Entity) this).position();
        sendLongDistanceParticles(level, getTransitionParticle(), pos.x, pos.y, pos.z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
    }

    default ParticleOptions getTransitionParticle() {
        return ParticleTypes.CLOUD;
    }

    default void onVanished(ServerLevel level) {
        Vec3 pos = ((Entity) this).position();
        sendLongDistanceParticles(level, getTransitionParticle(), pos.x, pos.y, pos.z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
    }

    default <T extends ParticleOptions> void sendLongDistanceParticles(ServerLevel level, T type, double posX, double posY, double posZ,
                                                                       int particleCount, double xOffset, double yOffset, double zOffset, double speed) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, type, true, posX, posY, posZ, particleCount,
                  (float) xOffset, (float) yOffset, (float) zOffset, (float) speed);
        }
    }

    default PathfinderMob asCourierEntity() {
        return (PathfinderMob) this;
    }

    default boolean hasReachedTarget(BlockPos localPos) {
        return CourierNavigation.hasReachedTarget(this, localPos, CourierNavigation.getReachDistance());
    }

    default boolean hasReachedTarget(BlockPos localPos, double distance) {
        return CourierNavigation.hasReachedTarget(this, localPos, distance);
    }

    default boolean hasReachedSegmentEndPos(Delivery delivery) {
        return delivery.getRoute().getSegment(delivery.getPhase()).endPos()
              .map(endPos -> hasReachedTarget(
                    CourierNavigation.getSegmentApproachTarget(level(), endPos, delivery.getPhase())))
              .orElse(true);
    }

    default boolean closerThan(BlockPos localPos, double distance) {
        return CourierNavigation.isWithinReach(this, localPos, distance);
    }

    default boolean pathfindDirectlyTowards(BlockPos localPos) {
        BlockPos navigationPos = CourierNavigation.getNavigationPos(this, localPos);
        getNavigation().setMaxVisitedNodesMultiplier(10.0F);
        getNavigation().moveTo(navigationPos.getX(), navigationPos.getY(), navigationPos.getZ(), 1, 1);
        return getNavigation().getPath() != null && getNavigation().getPath().canReach();
    }

    default void pathfindRandomlyTowards(BlockPos localPos) {
        Vec3 vec3 = Position.getGlobalCenter(level(), localPos).subtract(0, 0.5, 0);
        int i = 0;
        BlockPos blockPos = this.blockPosition();
        int j = (int) vec3.y - blockPos.getY();
        if (j > 2) {
            i = 4;
        } else if (j < -2) {
            i = -4;
        }

        int k = 6;
        int l = 8;
        int m = blockPos.distManhattan(Position.getNavigationPos(level(), localPos));
        if (m < 15) {
            k = m / 2;
            l = m / 2;
        }

        Vec3 pos = AirRandomPos.getPosTowards(asCourierEntity(), k, l, i, vec3, (float) (Math.PI / 10));
        if (pos != null) {
            getNavigation().setMaxVisitedNodesMultiplier(1.0F);
            getNavigation().moveTo(pos.x, pos.y, pos.z, 1);
        }
    }
}