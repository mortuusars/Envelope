package io.github.mortuusars.envelope.world.delivery;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;

public class BackgroundCourier implements Courier {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          CustomData.CODEC.optionalFieldOf("entity", CustomData.EMPTY).forGetter(BackgroundCourier::getEntity),
          Delivery.CODEC.optionalFieldOf("delivery").forGetter(BackgroundCourier::getDelivery),
          BlockPos.CODEC.optionalFieldOf("home_pos").forGetter(BackgroundCourier::getHomePos),
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("undelivered_mail", ItemStack.EMPTY).forGetter(BackgroundCourier::getUndeliveredMail)
    ).apply(instance, BackgroundCourier::new));

    private static final Logger LOGGER = LogUtils.getLogger();

    protected final CustomData entity;
    protected Optional<Delivery> delivery;
    protected Optional<BlockPos> homePos;
    protected ItemStack undeliveredMail;

    protected BackgroundCourier(CustomData entity, Optional<Delivery> delivery,
                             Optional<BlockPos> homePos, ItemStack undeliveredMail) {
        this.entity = entity;
        this.delivery = delivery;
        this.homePos = homePos;
        this.undeliveredMail = undeliveredMail;
    }

    public BackgroundCourier(CustomData entity, Optional<Delivery> delivery, Optional<BlockPos> homePos) {
        this(entity, delivery, homePos, ItemStack.EMPTY);
    }

    public static BackgroundCourier virtual() {
        return new BackgroundCourier(CustomData.EMPTY, null, null, ItemStack.EMPTY);
    }

    public CustomData getEntity() {
        return entity;
    }

    public Optional<Delivery> getDelivery() {
        return delivery;
    }

    public Optional<BlockPos> getHomePos() {
        return homePos;
    }

    public ItemStack getUndeliveredMail() {
        return undeliveredMail;
    }

    @Override
    public void setDelivery(Optional<Delivery> delivery) {
        this.delivery = delivery;
    }

    @Override
    public Component getName() {
        return Component.literal("Background Courier");
    }

    @Override
    public void handleUndeliveredMail(ServerLevel level, ItemStack mail) {
        undeliveredMail = mail;
    }

    public boolean canBeRemoved() {
        return getDelivery().isEmpty() && getHomePos().isEmpty();
    }

    @Override
    public Optional<BlockPos> getCurrentPos() {
        return isDelivering()
              ? getDelivery().flatMap(delivery -> delivery.getPhase().estimateCurrentPos())
              : getHomePos();
    }

    // --

    public void tick(ServerLevel level) {
        getDelivery().ifPresent(delivery -> tickDelivery(level, delivery));
    }

    public Optional<RealCourier> trySpawn(ServerLevel level) {
        if (getDelivery().isPresent() && getDelivery().get().getPhase().getType().isTraveling()) {
            return Optional.empty();
        }
        return getCurrentPos().flatMap(pos -> trySpawnNearby(level, pos));
    }

    public Optional<RealCourier> trySpawnNearby(ServerLevel level, BlockPos pos) {
        int y = Math.max(pos.getY(), level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() + 2);
        y = Math.min(y, level.getMaxBuildHeight());

        BlockPos blockPos = new BlockPos(pos.getX(), y, pos.getZ());

        if (!level.isLoaded(blockPos) || !isInSafeSimulationDistance(level, blockPos)) {
            return Optional.empty();
        }

        Vec3 p = Vec3.atCenterOf(blockPos);
        @Nullable Entity entity = createEntity(level);
        if (!(entity instanceof RealCourier courier)) {
            return Optional.empty();
        }

        entity.moveTo(p.x(), p.y(), p.z(), entity.getYRot(), entity.getXRot());
        level.addFreshEntity(entity);

        courier.onCourierSpawned(level);

        courier.setDelivery(delivery);
        courier.getDelivery()
              .ifPresent(delivery -> {
                  courier.startDeliveryPhase(level, delivery);
                  courier.onDeliveryChanged(level);
              });

        if (Envelope.debug()) LOGGER.info("Transitioning Background Courier to a {}...", courier.getName().getString());
        return Optional.of(courier);
    }

    public @Nullable Entity createEntity(ServerLevel level) {
        if (entity.isEmpty()) {
            @Nullable Pigeon pigeon = Envelope.EntityTypes.PIGEON.get().create(level);
            if (pigeon == null) {
                return null;
            }
            pigeon.setVariant(Pigeon.Variant.getRandom(level.getRandom()));
            pigeon.setService(true);
            return pigeon;
        }

        //noinspection deprecation
        return EntityType.loadEntityRecursive(entity.getUnsafe(), level, Function.identity());
    }
}
