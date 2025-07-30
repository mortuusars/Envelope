package io.github.mortuusars.envelope.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.client.gui.screen.MailboxAddressScreen;
import io.github.mortuusars.envelope.client.gui.screen.MailboxScreen;
import io.github.mortuusars.envelope.client.model.PigeonFancyHatModel;
import io.github.mortuusars.envelope.client.model.PigeonLegBandModel;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.model.geom.EnvelopeModelLayers;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.network.fabric.FabricS2CPacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public class EnvelopeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EnvelopeClient.init();

        ConfigScreenFactoryRegistry.INSTANCE.register(Envelope.ID, ConfigurationScreen::new);

        EntityRendererRegistry.register(Envelope.EntityTypes.PIGEON.get(), PigeonRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(EnvelopeModelLayers.PIGEON, PigeonModel::createLayerDefinition);
        EntityModelLayerRegistry.registerModelLayer(EnvelopeModelLayers.PIGEON_LEG_BAND, PigeonLegBandModel::createLayerDefinition);
        EntityModelLayerRegistry.registerModelLayer(EnvelopeModelLayers.PIGEON_FANCY_HAT, PigeonFancyHatModel::createLayerDefinition);

        MenuScreens.register(Envelope.MenuTypes.MAILBOX.get(), MailboxScreen::new);
        MenuScreens.register(Envelope.MenuTypes.MAILBOX_ADDRESS.get(), MailboxAddressScreen::new);

        FabricS2CPacketHandler.register();
    }
}
