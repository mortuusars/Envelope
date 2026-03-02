package io.github.mortuusars.envelope.neoforge.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;

public interface EnvelopeJSEvents {
    EventGroup GROUP = EventGroup.of("EnvelopeEvents");

    EventHandler REGISTER_ENTITY_DROP_OFF_HANDLERS = GROUP.startup("registerEntityDropOffHandlers",
          () -> RegisterEntityDropOffHandlersEventJS.class);

    EventHandler HANDLE_MAIL_DROP_OFF = GROUP.server("handleMailDropOff",
          () -> HandleMailDropOffEventJS.class);

    static void registerCustomEntityDropOffHandlers() {
        REGISTER_ENTITY_DROP_OFF_HANDLERS.post(ScriptType.STARTUP, new RegisterEntityDropOffHandlersEventJS());
    }
}
