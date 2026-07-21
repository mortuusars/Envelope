package io.github.mortuusars.envelope.world.mail.service;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.AddressLocation;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.*;
import java.util.stream.Collectors;

public class ServiceAddresses {
    public static final ResourceKey<ServiceAddressDefinition> MAIL_SERVICE =
          ResourceKey.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, Envelope.resource("mail_service"));
    public static final ResourceKey<ServiceAddressDefinition> AUTOMATED_SUPPLY_SERVICE =
          ResourceKey.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, Envelope.resource("automated_supply_service"));
    public static final ResourceKey<ServiceAddressDefinition> EQUINE_ASSURANCE_BUREAU =
          ResourceKey.create(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, Envelope.resource("equine_assurance_bureau"));

    private final MailService service;

    public ServiceAddresses(MailService service) {
        this.service = service;
    }

    public MailService getMailService() {
        return service;
    }

    public Set<ServiceAddress> getAllAddresses() {
        return getMailService().getLevel().registryAccess().registryOrThrow(Envelope.Registries.SERVICE_ADDRESS_DEFINITION)
              .holders()
              .map(ServiceAddress::new)
              .collect(Collectors.toSet());
    }

    public void tick() {
    }

    // --

    public static void bootstrap(BootstrapContext<ServiceAddressDefinition> context) {
        context.register(MAIL_SERVICE, new ServiceAddressDefinition(
              Component.translatable("address.envelope.mail_service"),
              EnvelopeSymbols.ADDRESS_MAIL_SERVICE,
              new AddressLocation.Relative(0)));
        context.register(AUTOMATED_SUPPLY_SERVICE, new ServiceAddressDefinition(
              Component.translatable("address.envelope.automated_supply_service"),
              EnvelopeSymbols.ADDRESS_SERVICE,
              new AddressLocation.Relative(1000)));
        context.register(EQUINE_ASSURANCE_BUREAU, new ServiceAddressDefinition(
              Component.translatable("address.envelope.equine_assurance_bureau"),
              EnvelopeSymbols.ADDRESS_SERVICE,
              new AddressLocation.Relative(2000)));
    }
}
