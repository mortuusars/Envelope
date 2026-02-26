package io.github.mortuusars.envelope.api.handler;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.mail.handler.EntityMailHandler;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

public class EntityMailHandlerRegistryImpl implements EntityMailHandlerRegistry {
    public static final EntityMailHandlerRegistryImpl INSTANCE = new EntityMailHandlerRegistryImpl();

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<ResourceLocation, Function<RegistryAccess, Optional<EntityMailHandler>>> suppliers = new HashMap<>();

    @Override
    public void register(ResourceLocation id, Function<RegistryAccess, Optional<EntityMailHandler>> supplier) {
        Preconditions.checkState(!suppliers.containsKey(id), "EntityMailHandler with id '%s' is already registered", id);
        suppliers.put(id, supplier);
    }

    public Map<ResourceLocation, EntityMailHandler> createHandlers(RegistryAccess access) {
        Map<ResourceLocation, EntityMailHandler> map = new HashMap<>();
        for (Map.Entry<ResourceLocation, Function<RegistryAccess, Optional<EntityMailHandler>>> entry : suppliers.entrySet()) {
            try {
                entry.getValue().apply(access).ifPresent(handler -> {
                    map.put(entry.getKey(), handler);
                });
            } catch (Exception e) {
                LOGGER.error("Failed to create entity mail handler '{}': ", entry.getKey(), e);
            }
        }
        return map;
    }
}
