package io.github.mortuusars.envelope.util.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;

public record ComponentProvider(WeightedRandomList<WeightedComponent> list) {
    public static final Codec<ComponentProvider> CODEC = WeightedRandomList.codec(WeightedComponent.FULL_CODEC)
          .xmap(ComponentProvider::new, ComponentProvider::list);

    public static final ComponentProvider EMPTY = new ComponentProvider(WeightedRandomList.create());

    public static ComponentProvider of(Component component) {
        return new ComponentProvider(WeightedRandomList.create(new WeightedComponent(component)));
    }

    public Component get(RandomSource randomSource) {
        return list.getRandom(randomSource).map(WeightedComponent::getComponent).orElse(CommonComponents.EMPTY);
    }
}
