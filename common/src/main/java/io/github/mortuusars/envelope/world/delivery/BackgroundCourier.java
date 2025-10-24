package io.github.mortuusars.envelope.world.delivery;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;

public class BackgroundCourier implements Courier {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CustomData.CODEC.optionalFieldOf("entity", CustomData.EMPTY).forGetter(BackgroundCourier::getEntity),
            Delivery.CODEC.optionalFieldOf("delivery", null).forGetter(BackgroundCourier::delivery)
    ).apply(instance, BackgroundCourier::new));

    private static final Logger LOGGER = LogUtils.getLogger();

    protected final CustomData entity;
    protected @Nullable Delivery delivery;
    protected boolean remove;
    protected @Nullable BlockPos spawnPos;

    public BackgroundCourier(CustomData entity, @NotNull Delivery delivery) {
        this.entity = entity;
        this.delivery = delivery;
    }

    public CustomData getEntity() {
        return entity;
    }

    public @Nullable Delivery delivery() {
        return delivery;
    }

    @Override
    public void setDelivery(@Nullable Delivery delivery) {
        this.delivery = delivery;
    }

    @Override
    public String getCourierName() {
        return "Background Courier";
    }

    public boolean shouldBeRemoved() {
        return delivery() == null || remove;
    }

    @Override
    public Optional<BlockPos> getCurrentPos() {
        return getDelivery().flatMap(d -> d.getPhase().estimateCurrentPos());
    }

    public @Nullable BlockPos getSpawnPos() {
        return spawnPos;
    }

    // --

    @Override
    public void startDeliveryPhase(ServerLevel level, Delivery delivery) {
        Courier.super.startDeliveryPhase(level, delivery);

        switch (delivery.getPhase().getType()) {
            case APPROACHING_TARGET, APPROACHING_HOME -> {
                delivery.getTargetPos().flatMap(pos ->
                      trySpawnNearby(level, Position.ascendTowards(level, pos, delivery.getOriginPos(), getAscendPosDistance())));
            }
        }
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        spawnPos = delivery.getSenderPos()
              .filter(p -> !getEntity().isEmpty())
              .orElse(null);

        Courier.super.endDelivery(level, delivery);
        remove = true;
    }

    // --

    public Optional<Courier> trySpawn(ServerLevel level) {
        if (spawnPos == null) return Optional.empty();
        return trySpawnNearby(level, spawnPos);
    }

    public Optional<Courier> trySpawnNearby(ServerLevel level, BlockPos pos) {
        int y = Math.max(pos.getY(), level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() + 5);
        y = Math.min(y, level.getMaxBuildHeight());

        BlockPos blockPos = new BlockPos(pos.getX(), y, pos.getZ());

        if (!level.isLoaded(blockPos) || !isInSafeSimulationDistance(level, blockPos)) {
            return Optional.empty();
        }

        Vec3 p = Vec3.atCenterOf(blockPos);
        @Nullable Entity entity = createEntity(level);
        if (!(entity instanceof Courier courier)) {
            return Optional.empty();
        }

        entity.moveTo(p.x(), p.y(), p.z(), entity.getYRot(), entity.getXRot());
        level.addFreshEntity(entity);

        level.sendParticles(ParticleTypes.CLOUD, p.x(), p.y(), p.z(), 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, p.x(), p.y(), p.z(),
                SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);

        courier.setDelivery(delivery);
        courier.getDelivery()
              .ifPresent(delivery -> {
                  courier.startDeliveryPhase(level, delivery);
                  courier.onDeliveryChanged(level);
              });

        remove = true;
        spawnPos = null;

        if (Envelope.debug()) LOGGER.info("Transitioning Background Courier to a {}...", courier.getCourierName());
        return Optional.of(courier);
    }

    public @Nullable Entity createEntity(ServerLevel level) {
        if (entity.isEmpty()) {
            @Nullable Pigeon pigeon = Envelope.EntityTypes.PIGEON.get().create(level);
            if (pigeon == null) {
                return null;
            }
            pigeon.setVariant(Pigeon.Variant.getRandom(level.getRandom()));
            return pigeon;
        }

        //noinspection deprecation
        return EntityType.loadEntityRecursive(entity.getUnsafe(), level, Function.identity());
    }
}
