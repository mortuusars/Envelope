package io.github.mortuusars.envelope.api;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffHandler;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDropOffHandlerRegistry {
    private static final Map<ResourceKey<ServiceAddressDefinition>, List<MailDropOffHandler>> handlers = new HashMap<>();

    public static void register(ResourceKey<ServiceAddressDefinition> definition, MailDropOffHandler handler) {
        List<MailDropOffHandler> list = handlers.computeIfAbsent(definition, key -> new ArrayList<>());
        list.add(handler);
        handlers.put(definition, list);
    }

    public static void register(ResourceLocation definition, MailDropOffHandler handler) {
        register(ResourceKey.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, definition), handler);
    }

    // --

    public static List<MailDropOffHandler> getHandlers(ServiceAddress address) {
        return address.getDefinitionHolder().unwrapKey()
              .map(key -> handlers.getOrDefault(key, List.of()))
              .orElse(List.of());
    }
}
