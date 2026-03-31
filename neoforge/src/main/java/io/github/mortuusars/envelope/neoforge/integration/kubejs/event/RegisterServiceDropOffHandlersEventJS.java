package io.github.mortuusars.envelope.neoforge.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import io.github.mortuusars.envelope.api.ServiceDropOffHandlerRegistry;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffHandler;
import net.minecraft.resources.ResourceLocation;

public class RegisterServiceDropOffHandlersEventJS implements KubeEvent {
    public void register(ResourceLocation addressId, MailDropOffHandler handler) {
        ServiceDropOffHandlerRegistry.register(addressId, handler);
    }
}
