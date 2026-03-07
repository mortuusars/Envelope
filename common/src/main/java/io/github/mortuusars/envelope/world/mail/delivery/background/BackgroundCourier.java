package io.github.mortuusars.envelope.world.mail.delivery.background;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableItem;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.delivery.Courier;
import io.github.mortuusars.envelope.world.mail.delivery.CourierOrigin;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableEntityData;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.Optional;

public class BackgroundCourier implements Courier {
    public static final Codec<BackgroundCourier> CODEC = RecordCodecBuilder.create(i -> i.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(BackgroundCourier::getSpawnableEntityData),
          CourierOrigin.CODEC.optionalFieldOf("origin", CourierOrigin.service()).forGetter(BackgroundCourier::getOrigin),
          Delivery.CODEC.fieldOf("delivery").forGetter(BackgroundCourier::getDelivery)
    ).apply(i, BackgroundCourier::new));
    public static final Logger LOGGER = LogUtils.getLogger();

    private final SpawnableEntityData entityData;
    private final Delivery delivery;
    private final CourierOrigin origin;
    private boolean removed;

    public BackgroundCourier(SpawnableEntityData entityData, CourierOrigin origin, Delivery delivery) {
        this.entityData = entityData;
        this.delivery = delivery;
        this.origin = origin;
    }

    public SpawnableEntityData getSpawnableEntityData() {
        return entityData;
    }

    @Override
    public CourierOrigin getOrigin() {
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
    public void endDelivery(ServerLevel level, Delivery delivery) {
        setRemoved();

        if (getOrigin().isRegular()) {
            BackgroundDelivery background = MailService.of(level).getBackgroundDelivery();
            background.addFinishedCourier(new FinishedBackgroundCourier(getSpawnableEntityData(), getOrigin().getPos(), level.getGameTime()));
            if (!delivery.getMail().isEmpty()) {
                background.addDroppedMail(new SpawnableItem(delivery.getMail(), getOrigin().getPos()));
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
