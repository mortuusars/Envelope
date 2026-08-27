package io.github.mortuusars.envelope.util.bugger.data;

import com.mojang.serialization.Codec;
import io.github.mortuusars.mortaar.client.Minecrft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.BiConsumer;

public class EntityData<T> extends PairData<Integer, T> {
    protected BiConsumer<Entity, T> handler = (entity, data) -> {
    };

    public EntityData(ResourceLocation id, Codec<T> codec) {
        super(id, Codec.INT.fieldOf("id"), codec.fieldOf("data"));
    }

    public EntityData<T> handle(BiConsumer<Entity, T> handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public void handle(Integer id, T data) {
        if (Minecrft.level().getEntity(id) instanceof Entity entity) {
            handler.accept(entity, data);
        }
    }
}
