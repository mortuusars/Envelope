package io.github.mortuusars.envelope.util.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class WeightedComponent implements WeightedEntry {
    public static final Codec<WeightedComponent> FULL_CODEC = RecordCodecBuilder.create(i -> i.group(
          ComponentSerialization.CODEC.optionalFieldOf("component", Component.empty()).forGetter(WeightedComponent::getComponent),
          Weight.CODEC.optionalFieldOf("weight", Weight.of(1)).forGetter(WeightedComponent::getWeight)
    ).apply(i, WeightedComponent::new));

    public static final Codec<WeightedComponent> CODEC = Codec.either(ComponentSerialization.CODEC, FULL_CODEC)
          .xmap(regularOrWeighted -> regularOrWeighted.map(WeightedComponent::new, Function.identity()),
                WeightedComponent::asEither);

    private final Component component;
    private final Weight weight;

    public WeightedComponent(Component component, Weight weight) {
        this.component = component;
        this.weight = weight;
    }

    public WeightedComponent(Component component) {
        this.component = component;
        this.weight = Weight.of(1);
    }

    public Component getComponent() {
        return component;
    }

    @Override
    public @NotNull Weight getWeight() {
        return weight;
    }

    public Either<Component, WeightedComponent> asEither() {
        if (getWeight().asInt() <= 1) {
            return Either.left(getComponent());
        } else {
            return Either.right(this);
        }
    }
}