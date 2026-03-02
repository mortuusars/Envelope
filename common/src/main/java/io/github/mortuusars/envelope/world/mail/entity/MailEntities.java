package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.Envelope;
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
    public static final ResourceKey<MailEntity> AUTOMATED_SUPPLY_SERVICE = createKey(Envelope.resource("automated_supply_service"));

    private final MailService service;

    public MailEntities(MailService service) {
        this.service = service;
    }

    public MailService getMailService() {
        return service;
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
        context.register(AUTOMATED_SUPPLY_SERVICE, new MailEntity(
              Component.translatable("address.envelope.automated_supply_service"),
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
