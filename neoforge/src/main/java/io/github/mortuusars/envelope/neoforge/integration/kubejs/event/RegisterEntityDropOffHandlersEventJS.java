package io.github.mortuusars.envelope.neoforge.integration.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import io.github.mortuusars.envelope.api.EntityMailDropOffHandlerRegistry;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffHandler;
import net.minecraft.resources.ResourceLocation;

public class RegisterEntityDropOffHandlersEventJS implements KubeEvent {
    public void register(ResourceLocation addressId, MailDropOffHandler handler) {
        EntityMailDropOffHandlerRegistry.register(addressId, handler);
    }
}
