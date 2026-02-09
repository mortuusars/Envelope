package io.github.mortuusars.envelope.world.mail.address;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;

public class EntityAddresses {
    public static void bootstrap(BootstrapContext<EntityAddressDefinition> context) {
        context.register(Address.MAIL_SERVICE.getKey(),
              new EntityAddressDefinition("Mail Service", Component.translatable("address.envelope.mail_service")));
    }
}
