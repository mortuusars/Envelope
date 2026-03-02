package io.github.mortuusars.envelope.api;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffHandler;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityMailDropOffHandlerRegistry {
    private static final Map<ResourceKey<MailEntity>, List<MailDropOffHandler>> handlers = new HashMap<>();

    public static void register(ResourceKey<MailEntity> entity, MailDropOffHandler handler) {
        List<MailDropOffHandler> list = handlers.computeIfAbsent(entity, key -> new ArrayList<>());
        list.add(handler);
        handlers.put(entity, list);
    }

    public static void register(ResourceLocation entity, MailDropOffHandler handler) {
        register(ResourceKey.create(Envelope.Registries.MAIL_ENTITY, entity), handler);
    }

    // --

    public static List<MailDropOffHandler> getHandlers(EntityAddress address) {
        return address.getEntityHolder().unwrapKey()
              .map(key -> handlers.getOrDefault(key, List.of()))
              .orElse(List.of());
    }
}
