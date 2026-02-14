package io.github.mortuusars.envelope.world.mail.entity;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.Set;
import java.util.stream.Collectors;

public class MailEntities {
    public static final ResourceKey<MailEntity> MAIL_SERVICE =
          ResourceKey.create(Envelope.Registries.MAIL_ENTITY, Envelope.resource("mail_service"));
    public static final ResourceKey<MailEntity> TRADE_OFFICE =
          ResourceKey.create(Envelope.Registries.MAIL_ENTITY, Envelope.resource("trade_office"));

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

    public static void bootstrap(BootstrapContext<MailEntity> context) {
        context.register(MAIL_SERVICE, new MailEntity(
              Component.translatable("address.envelope.mail_service"),
              new AddressLocation.Relative(1000)));
        context.register(TRADE_OFFICE, new MailEntity(
              Component.translatable("address.envelope.trade_office"),
              new AddressLocation.Relative(2000)));
    }
}
