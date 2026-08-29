package io.github.mortuusars.envelope.world.mail.delivery;

import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.ai.CourierNavigation;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.entity.ai.goal.courier.DeliverMailGoal;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableEntityData;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface PhysicalCourier extends Courier {
    Level level();
    Vec3 position();
    BlockPos blockPosition();
    PathNavigation getNavigation();
    MailboxHandler getMailboxHandler();
    SpawnableEntityData toSpawnableCourierData();
    void setOrigin(CourierOrigin origin);

    void setDelivery(Delivery delivery);

    default boolean canStartDelivery() {
        return true;
    }

    default void startDelivery(Delivery delivery) {
        if (getCurrentDelivery().isPresent()) {
            LOGGER.warn("Starting new delivery when the courier is already delivering. This might be an error.");
        }
        if (!getCourierOrigin().isService()) {
            setOrigin(CourierOrigin.regular(blockPosition()));
        }
        setDelivery(delivery);
        asCourierEntity().stopRiding();
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
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
    }

    default void onVanished(ServerLevel level) {
        Vec3 pos = ((Entity) this).position();
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
    }

    default Mob asCourierEntity() {
        return (Mob) this;
    }

    default boolean hasReachedTarget(BlockPos localPos) {
        return CourierNavigation.hasReachedTarget(this, localPos, CourierNavigation.getReachDistance());
    }

    default boolean hasReachedTarget(BlockPos localPos, double distance) {
        return CourierNavigation.hasReachedTarget(this, localPos, distance);
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

        Mob entity = asCourierEntity();

        Vec3 vec32 = entity instanceof PathfinderMob pathfinderMob
              ? AirRandomPos.getPosTowards(pathfinderMob, k, l, i, vec3, (float) (Math.PI / 10))
              : DeliverMailGoal.MobAdapter.getPosTowards(entity, k, l, i, vec3, (float) (Math.PI / 10));
        if (vec32 != null) {
            getNavigation().setMaxVisitedNodesMultiplier(1.0F);
            getNavigation().moveTo(vec32.x, vec32.y, vec32.z, 1);
        }
    }
}