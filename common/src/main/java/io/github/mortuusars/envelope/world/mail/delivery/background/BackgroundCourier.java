package io.github.mortuusars.envelope.world.mail.delivery.background;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.CourierBat;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableItem;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.delivery.*;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableEntityData;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;

public class BackgroundCourier implements Courier {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(i -> i.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(BackgroundCourier::getSpawnableEntityData),
          CourierProperties.CODEC.optionalFieldOf("properties", CourierProperties.DEFAULT).forGetter(BackgroundCourier::getCourierProperties),
          CourierOrigin.CODEC.optionalFieldOf("origin", CourierOrigin.service()).forGetter(BackgroundCourier::getCourierOrigin),
          Delivery.CODEC.fieldOf("delivery").forGetter(BackgroundCourier::getDelivery)
    ).apply(i, BackgroundCourier::new));
    public static final Logger LOGGER = LogUtils.getLogger();

    private final SpawnableEntityData entityData;
    private final CourierProperties properties;
    private final CourierOrigin origin;
    private final Delivery delivery;

    private boolean removed;

    public BackgroundCourier(SpawnableEntityData entityData, CourierProperties properties, CourierOrigin origin, Delivery delivery) {
        this.entityData = entityData;
        this.properties = properties;
        this.origin = origin;
        this.delivery = delivery;
    }

    public SpawnableEntityData getSpawnableEntityData() {
        return entityData;
    }

    @Override
    public CourierProperties getCourierProperties() {
        return properties;
    }

    @Override
    public @NotNull CourierOrigin getCourierOrigin() {
        return origin;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    @Override
    public Optional<Delivery> getCurrentDelivery() {
        return Optional.of(delivery);
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved() {
        this.removed = true;
    }

    // -- Delivery

    public void tick(ServerLevel level) {
        if (!isRemoved()) {
            tickDelivery(level, getDelivery());
        }
    }

    @Override
    public boolean dispatchDelivery(ServerLevel level, Delivery delivery) {
        boolean handled = Courier.super.dispatchDelivery(level, delivery);
        changeToAppropriateCourierIfNeeded(level, delivery);
        return handled;
    }

    @Override
    public boolean dispatchReturn(ServerLevel level, Delivery delivery) {
        boolean handled = Courier.super.dispatchReturn(level, delivery);
        changeToAppropriateCourierIfNeeded(level, delivery);
        return handled;
    }

    public boolean changeToAppropriateCourierIfNeeded(ServerLevel level, Delivery delivery) {
        if (!getCourierOrigin().isService() || delivery.getPhase() == DeliveryPhase.FINISHED) {
            return false;
        }

        return getSpawnableEntityData().getEntityType()
              .flatMap(type -> {
                  if (type.equals(Envelope.EntityTypes.COURIER_BAT.get()) && (!level.isNight() || !Config.Server.BAT_EMPLOYED_AT_MAIL_SERVICE.get())) {
                      return Optional.of(Pigeon.createService(level));
                  }
                  if (type.equals(Envelope.EntityTypes.PIGEON.get()) && level.isNight() && Config.Server.BAT_EMPLOYED_AT_MAIL_SERVICE.get()) {
                      return Optional.of(CourierBat.createService(level));
                  }
                  return Optional.empty();
              })
              .map(courier -> {
                  level.getEnvelopeMailService().getBackgroundDelivery().addCourier(new BackgroundCourier(
                        courier.toSpawnableCourierData(),
                        courier.getCourierProperties(),
                        CourierOrigin.service(),
                        delivery));
                  setRemoved();
                  return true;
              })
              .orElse(false);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        setRemoved();

        if (getCourierOrigin().isRegular()) {
            BackgroundDelivery background = MailService.of(level).getBackgroundDelivery();
            background.addFinishedCourier(new FinishedBackgroundCourier(getSpawnableEntityData(), getCourierOrigin().getPos(), level.getGameTime()));
            if (!delivery.getMail().isEmpty()) {
                background.addDroppedMail(new SpawnableItem(delivery.getMail(), getCourierOrigin().getPos()));
            }
        } else if (!delivery.getMail().isEmpty()) {
            delivery.getRoute().getSenderPos().ifPresentOrElse(
                  senderPos -> {
                      LOGGER.warn("Dropping undelivered mail on the ground. Delivery: {}.", delivery);
                      MailService.of(level).getBackgroundDelivery().addDroppedMail(new SpawnableItem(delivery.getMail(), senderPos));
                  },
                  () ->
                        LOGGER.warn("Voiding undelivered mail. Delivery: {}.", delivery));
        }
    }
}
