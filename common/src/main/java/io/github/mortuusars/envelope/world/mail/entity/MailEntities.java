package io.github.mortuusars.envelope.world.mail.entity;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MailEntities {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<EntityAddress, MailEntity> entities = new HashMap<>();

    public Optional<MailEntity> byAddress(EntityAddress address) {
        return Optional.ofNullable(entities.get(address));
    }

    public Set<EntityAddress> getAllAddresses() {
        return entities.keySet();
    }

    public void register(MailEntity entity) {
        EntityAddress address = entity.getAddress();
        if (entities.containsKey(address)) {
            LOGGER.warn("Mail entity with address '{}' is already registered. Old one will be overwritten.", address);
        }
        entities.put(address, entity);
    }
}
