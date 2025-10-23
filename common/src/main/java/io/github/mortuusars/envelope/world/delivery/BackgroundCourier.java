package io.github.mortuusars.envelope.world.delivery;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class BackgroundCourier implements Courier {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompoundTag.CODEC.fieldOf("entity_tag").forGetter(BackgroundCourier::getEntityTag),
            Delivery.CODEC.optionalFieldOf("delivery", null).forGetter(BackgroundCourier::delivery)
    ).apply(instance, BackgroundCourier::new));
    private static final Logger LOGGER = LogUtils.getLogger();

    protected final CompoundTag entityTag;
    protected @Nullable Delivery delivery;
    protected boolean remove = false;

    public BackgroundCourier(CompoundTag entityTag, @NotNull Delivery delivery) {
        this.entityTag = entityTag;
        this.delivery = delivery;
    }

    public CompoundTag getEntityTag() {
        return entityTag;
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

    // --

    @Override
    public void startDeliveryPhase(ServerLevel level, Delivery delivery) {
        Courier.super.startDeliveryPhase(level, delivery);

        switch (delivery.getPhase().getType()) {
            case APPROACHING_TARGET, APPROACHING_HOME -> {
                delivery.getTargetPos()
                      .flatMap(pos -> trySpawnNearby(level, Position.ascendTowards(level, pos,
                            delivery.getOriginPos(), getAscendPosDistance()), true))
                      .ifPresent(pigeon -> {
                          pigeon.startDeliveryPhase(level, delivery);
                          pigeon.onDeliveryChanged(level);
                          remove = true;
                      });
            }
        }
    }

    @Override
    public void advanceDeliveryPhase(ServerLevel level, Delivery delivery) {
        Courier.super.advanceDeliveryPhase(level, delivery);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        Courier.super.endDelivery(level, delivery);
        remove = true;
        LOGGER.error("Spawning couriers after delivery is not implemented yet.");
    }

    public Optional<Pigeon> trySpawnNearby(ServerLevel level, BlockPos pos, boolean effects) {
        BlockPos blockPos = new BlockPos(
                pos.getX(),
                Math.max(pos.getY(), level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() + 5),
                pos.getZ());

        if (!level.isLoaded(blockPos)) {
            return Optional.empty();
        }

        Vec3 p = Vec3.atCenterOf(blockPos);
        @Nullable Entity entity = createEntity(level);
        if (entity instanceof Pigeon pigeon) {
            entity.moveTo(p.x(), p.y(), p.z(), entity.getYRot(), entity.getXRot());
            level.addFreshEntity(entity);

            if (effects) {
                level.sendParticles(ParticleTypes.CLOUD, p.x(), p.y(), p.z(), 16, 0.1, 0.1, 0.1, 0.05);
                level.playSound(null, p.x(), p.y(), p.z(),
                        SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
            }

            pigeon.setDelivery(delivery);

            return Optional.of(pigeon);
        }

        return Optional.empty();
    }

    public @Nullable Entity createEntity(ServerLevel level) {
        if (entityTag.isEmpty()) {
            Pigeon pigeon = Objects.requireNonNull(Envelope.EntityTypes.PIGEON.get().create(level));
            pigeon.setVariant(Pigeon.Variant.getRandom(level.getRandom()));
            return pigeon;
        }

        return EntityType.loadEntityRecursive(entityTag, level, Function.identity());
    }
}
