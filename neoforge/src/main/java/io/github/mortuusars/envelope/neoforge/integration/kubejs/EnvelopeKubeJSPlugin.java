package io.github.mortuusars.envelope.neoforge.integration.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.neoforge.api.event.HandleMailDropOffEvent;
import io.github.mortuusars.envelope.neoforge.integration.kubejs.event.EnvelopeJSEvents;
import io.github.mortuusars.envelope.neoforge.integration.kubejs.event.HandleMailDropOffEventJS;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

public class EnvelopeKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(EnvelopeJSEvents.GROUP);
    }

    @Override
    public void init() {
        subscribeToNeoForgeEvents();
    }

    private void subscribeToNeoForgeEvents() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, this::handleMailDropOff);
    }

    private void handleMailDropOff(HandleMailDropOffEvent event) {
        HandleMailDropOffEventJS eventJS = new HandleMailDropOffEventJS(event.getContext());
        EventResult postResult = EnvelopeJSEvents.HANDLE_MAIL_DROP_OFF.post(ScriptType.SERVER, eventJS);

        if (postResult.override() && postResult.value() instanceof MailDropOffResult result) {
            event.setResult(result);
        }
    }
}