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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;

public class BackgroundCourier implements Courier {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          CustomData.CODEC.optionalFieldOf("entity", CustomData.EMPTY).forGetter(BackgroundCourier::entity),
          Delivery.CODEC.optionalFieldOf("delivery", null).forGetter(BackgroundCourier::delivery),
          BlockPos.CODEC.optionalFieldOf("home_pos", null).forGetter(BackgroundCourier::homePos),
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("undelivered_mail", ItemStack.EMPTY).forGetter(BackgroundCourier::undeliveredMail)
    ).apply(instance, BackgroundCourier::new));

    private static final Logger LOGGER = LogUtils.getLogger();

    protected final CustomData entity;
    protected @Nullable Delivery delivery;
    protected @Nullable BlockPos homePos;
    protected ItemStack undeliveredMail;

    protected BackgroundCourier(CustomData entity, @Nullable Delivery delivery,
                             @Nullable BlockPos homePos, ItemStack undeliveredMail) {
        this.entity = entity;
        this.delivery = delivery;
        this.homePos = homePos;
        this.undeliveredMail = undeliveredMail;
    }

    public BackgroundCourier(CustomData entity, @NotNull Delivery delivery, @NotNull BlockPos homePos) {
        this(entity, delivery, homePos, ItemStack.EMPTY);
    }

    public static BackgroundCourier virtual() {
        return new BackgroundCourier(CustomData.EMPTY, null, null, ItemStack.EMPTY);
    }

    public CustomData entity() {
        return entity;
    }

    public @Nullable Delivery delivery() {
        return delivery;
    }

    public @Nullable BlockPos homePos() {
        return homePos;
    }

    public ItemStack undeliveredMail() {
        return undeliveredMail;
    }

    @Override
    public void setDelivery(@Nullable Delivery delivery) {
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
        return delivery() == null && homePos() == null;
    }

    @Override
    public Optional<BlockPos> getCurrentPos() {
        return delivery != null
              ? getDelivery().flatMap(delivery -> delivery.getPhase().estimateCurrentPos())
              : Optional.ofNullable(homePos);
    }

    // --

    public void tick(ServerLevel level) {
        if (delivery != null) {
            tickDelivery(level, delivery);
        }
    }

    public Optional<RealCourier> trySpawn(ServerLevel level) {
        if (delivery != null && delivery.getPhase().getType().isTraveling()) {
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
