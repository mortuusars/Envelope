package io.github.mortuusars.envelope.world.mail.service;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;

import java.util.*;
import java.util.stream.Collectors;

public class ServiceAddresses {
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
              .filter(ServiceAddress::isEnabled)
              .map(ServiceAddress::new)
              .collect(Collectors.toSet());
    }
}
