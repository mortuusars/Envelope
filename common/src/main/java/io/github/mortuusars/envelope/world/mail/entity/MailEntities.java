package io.github.mortuusars.envelope.world.mail.entity;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MailEntities {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<EntityAddress, MailEntity> entities = new HashMap<>();

    private final MailService service;

    public MailEntities(MailService service) {
        this.service = service;
    }

    public MailService getMailService() {
        return service;
    }

    public Set<EntityAddress> getAllAddresses() {
        return getMailService().getLevel().registryAccess().registryOrThrow(Envelope.Registries.ENTITY_ADDRESS)
              .holders()
              .map(EntityAddress::new)
              .collect(Collectors.toSet());
    }

    public Optional<MailEntity> byAddress(EntityAddress address) {
        return Optional.ofNullable(entities.get(address));
    }

    public void register(MailEntity entity) {
        EntityAddress address = entity.getAddress();
        if (entities.containsKey(address)) {
            LOGGER.warn("Mail entity with address '{}' is already registered. Old one will be overwritten.", address);
        }
        entities.put(address, entity);
    }
}
