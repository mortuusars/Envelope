package io.github.mortuusars.envelope.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BreakPaperBoxWhenFallingTrigger extends SimpleCriterionTrigger<BreakPaperBoxWhenFallingTrigger.TriggerInstance> {
    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, double fallDistance) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(fallDistance));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<MinMaxBounds.Doubles> distance) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                    MinMaxBounds.Doubles.CODEC.optionalFieldOf("distance").forGetter(TriggerInstance::distance))
              .apply(instance, TriggerInstance::new));

        public boolean matches(double fallDistance) {
            return (this.distance.isEmpty() || this.distance.get().matches(fallDistance));
        }
    }
}
