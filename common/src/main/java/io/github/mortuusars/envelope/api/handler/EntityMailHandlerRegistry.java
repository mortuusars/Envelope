package io.github.mortuusars.envelope.api.handler;

import io.github.mortuusars.envelope.world.mail.handler.EntityMailHandler;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Function;

public interface EntityMailHandlerRegistry {
    EntityMailHandlerRegistry INSTANCE = EntityMailHandlerRegistryImpl.INSTANCE;

    void register(ResourceLocation id, Function<RegistryAccess, Optional<EntityMailHandler>> supplier);
}