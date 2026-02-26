package io.github.mortuusars.envelope.world.mail.entity;

import com.google.common.collect.*;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.handler.EntityMailHandler;
import io.github.mortuusars.envelope.api.handler.EntityMailHandlerRegistryImpl;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MailEntities {
    public static final ResourceKey<MailEntity> MAIL_SERVICE = createKey(Envelope.resource("mail_service"));
    public static final ResourceKey<MailEntity> TRADE_OFFICE = createKey(Envelope.resource("trade_office"));

    private final MailService service;
    private final BiMap<ResourceLocation, EntityMailHandler> handlers;
    private final Map<EntityAddress, List<EntityMailHandler>> handlersByAddress;

    public MailEntities(MailService service) {
        this.service = service;
        this.handlers = HashBiMap.create(EntityMailHandlerRegistryImpl.INSTANCE.createHandlers(service.getLevel().registryAccess()));
        this.handlersByAddress = new HashMap<>();
        for (Map.Entry<ResourceLocation, EntityMailHandler> entry : handlers.entrySet()) {
            handlersByAddress.computeIfAbsent(entry.getValue().getAddress(), address -> new ArrayList<>())
                  .add(entry.getValue());
        }
    }

    public MailService getMailService() {
        return service;
    }

    public List<EntityMailHandler> getHandlers(EntityAddress address) {
        return handlersByAddress.getOrDefault(address, List.of());
    }

    public Optional<ResourceLocation> getHandlerId(EntityMailHandler handler) {
        return Optional.ofNullable(handlers.inverse().get(handler));
    }

    public Set<EntityAddress> getAllAddresses() {
        return getMailService().getLevel().registryAccess().registryOrThrow(Envelope.Registries.MAIL_ENTITY)
              .holders()
              .map(EntityAddress::new)
              .collect(Collectors.toSet());
    }

    // --

    public static ResourceKey<MailEntity> createKey(ResourceLocation location) {
        return ResourceKey.create(Envelope.Registries.MAIL_ENTITY, location);
    }

    public static void bootstrap(BootstrapContext<MailEntity> context) {
        context.register(MAIL_SERVICE, new MailEntity(
              Component.translatable("address.envelope.mail_service"),
              new AddressLocation.Relative(1000)));
        context.register(TRADE_OFFICE, new MailEntity(
              Component.translatable("address.envelope.trade_office"),
              new AddressLocation.Relative(2000)));
    }

    public @NotNull Holder<MailEntity> get(ResourceKey<MailEntity> key) {
        return getMailService().getLevel().registryAccess().registryOrThrow(Envelope.Registries.MAIL_ENTITY)
              .getHolderOrThrow(key);
    }

    public @NotNull EntityAddress getAddress(ResourceKey<MailEntity> key) {
        return new EntityAddress(get(key));
    }
}
