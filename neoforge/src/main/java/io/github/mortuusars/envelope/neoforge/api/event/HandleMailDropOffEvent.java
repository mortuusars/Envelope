package io.github.mortuusars.envelope.neoforge.api.event;

import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class HandleMailDropOffEvent extends Event implements ICancellableEvent {
    private final MailDropOffContext context;
    private MailDropOffResult result = MailDropOffResult.PASS;

    public HandleMailDropOffEvent(MailDropOffContext context) {
        this.context = context;
    }

    public MailDropOffContext getContext() {
        return context;
    }

    public MailDropOffResult getResult() {
        return result;
    }

    public void setResult(MailDropOffResult result) {
        this.result = result;
        setCanceled(true);
    }
}
