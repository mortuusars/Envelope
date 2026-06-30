package io.github.mortuusars.envelope.advancements.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public record DeliveryPredicate(
      Optional<ItemPredicate> mail,
      Optional<MinMaxBounds.Ints> distance) {
    public static final Codec<DeliveryPredicate> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemPredicate.CODEC.optionalFieldOf("mail").forGetter(DeliveryPredicate::mail),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("distance").forGetter(DeliveryPredicate::distance))
          .apply(i, DeliveryPredicate::new));

    public boolean matches(ServerLevel level, Delivery delivery) {
        return (mail.isEmpty() || mail.get().test(delivery.getMail()))
              && (distance.isEmpty() || distance.get().matches(delivery.getRoute().getDistance(level)));
    }
}
