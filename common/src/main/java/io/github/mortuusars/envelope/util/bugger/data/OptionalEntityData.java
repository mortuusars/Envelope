package io.github.mortuusars.envelope.util.bugger.data;

import com.mojang.serialization.Codec;
import io.github.mortuusars.mortaar.client.Minecrft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.function.BiConsumer;

public class OptionalEntityData<T> extends PairData<Integer, Optional<T>> {
    protected BiConsumer<Entity, Optional<T>> handler = (entity, data) -> {
    };

    public OptionalEntityData(ResourceLocation id, Codec<T> codec) {
        super(id, Codec.INT.fieldOf("id"), codec.optionalFieldOf("data"));
    }

    public OptionalEntityData<T> handle(BiConsumer<Entity, Optional<T>> handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public void handle(Integer id, Optional<T> data) {
        if (Minecrft.level().getEntity(id) instanceof Entity entity) {
            handler.accept(entity, data);
        }
    }
}
