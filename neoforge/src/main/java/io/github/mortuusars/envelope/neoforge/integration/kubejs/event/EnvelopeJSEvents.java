package io.github.mortuusars.envelope.neoforge.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.type.TypeInfo;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;

public interface EnvelopeJSEvents {
    EventGroup GROUP = EventGroup.of("EnvelopeEvents");

    EventHandler REGISTER_SERVICE_DROP_OFF_HANDLERS = GROUP.startup("registerServiceDropOffHandlers",
          () -> RegisterServiceDropOffHandlersEventJS.class);

    EventHandler HANDLE_MAIL_DROP_OFF = GROUP.server("handleMailDropOff",
          () -> HandleMailDropOffEventJS.class).hasResult(TypeInfo.of(MailDropOffResult.class));

    static void registerServiceDropOffHandlers() {
        REGISTER_SERVICE_DROP_OFF_HANDLERS.post(ScriptType.STARTUP, new RegisterServiceDropOffHandlersEventJS());
    }
}
