package io.github.mortuusars.envelope.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.advancements.predicate.DeliveryPredicate;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MailDeliveredTrigger extends SimpleCriterionTrigger<MailDeliveredTrigger.TriggerInstance> {
    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player,
                        Delivery delivery) {
        this.trigger(player, triggerInstance ->
              triggerInstance.matches(player, delivery));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<DeliveryPredicate> delivery) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    DeliveryPredicate.CODEC.optionalFieldOf("delivery").forGetter(TriggerInstance::delivery))
              .apply(instance, TriggerInstance::new));

        public boolean matches(ServerPlayer player,
                               Delivery delivery) {
            return (this.delivery.isEmpty() || this.delivery.get().matches(player.serverLevel(), delivery));
        }
    }
}
