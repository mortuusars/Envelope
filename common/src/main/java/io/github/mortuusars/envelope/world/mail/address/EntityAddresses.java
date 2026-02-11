package io.github.mortuusars.envelope.world.mail.address;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

public class EntityAddresses {
    public static final ResourceKey<EntityAddressDefinition> MAIL_SERVICE =
          ResourceKey.create(Envelope.Registries.ENTITY_ADDRESS, Envelope.resource("mail_service"));

    public static void bootstrap(BootstrapContext<EntityAddressDefinition> context) {
        context.register(MAIL_SERVICE,
              new EntityAddressDefinition("Mail Service", Component.translatable("address.envelope.mail_service")));
    }
}
