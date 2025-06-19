package io.github.mortuusars.envelope.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.client.gui.screen.MailboxScreen;
import io.github.mortuusars.envelope.network.fabric.FabricS2CPacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public class EnvelopeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EnvelopeClient.init();
        ConfigScreenFactoryRegistry.INSTANCE.register(Envelope.ID, ConfigurationScreen::new);
        MenuScreens.register(Envelope.MenuTypes.MAILBOX.get(), MailboxScreen::new);
        FabricS2CPacketHandler.register();
    }
}
