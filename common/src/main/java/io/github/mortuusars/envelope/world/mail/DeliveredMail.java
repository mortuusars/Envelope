package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.api.mail.Mail;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DeliveredMail {
    protected final Map<UUID, Set<Mail>> mail = new HashMap<>();

    public boolean deliver(UUID recipientUuid, Mail mail) {
        return this.mail.computeIfAbsent(recipientUuid, uuid -> new HashSet<>()).add(mail);
    }

    public boolean takeOut(UUID recipientUuid, Mail mail) {
        @Nullable Set<Mail> set = this.mail.get(recipientUuid);
        return set != null && set.remove(mail);
    }

    public Set<Mail> getAll(UUID recipientUuid) {
        Set<Mail> set = this.mail.getOrDefault(recipientUuid, Collections.emptySet());
        return Collections.unmodifiableSet(set);
    }
}
