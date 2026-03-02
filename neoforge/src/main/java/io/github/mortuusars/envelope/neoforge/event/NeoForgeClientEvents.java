package io.github.mortuusars.envelope.neoforge.event;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.client.gui.screen.*;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonBackpackLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonHatLayer;
import io.github.mortuusars.envelope.world.item.Sealable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Envelope.ID, value = Dist.CLIENT)
public class NeoForgeClientEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EnvelopeClient::init);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(Envelope.MenuTypes.MAILBOX.get(), MailboxScreen::new);

        event.register(Envelope.MenuTypes.PACKING.get(), PackingScreen::new);
        event.register(Envelope.MenuTypes.PACKAGE.get(), PackageScreen::new);

        event.register(Envelope.MenuTypes.PAYBACK_PACKING.get(), PaybackPackingScreen::new);
        event.register(Envelope.MenuTypes.PAYBACK_PACKAGE.get(), PaybackPackageScreen::new);

        event.register(Envelope.MenuTypes.PAYBACK_TAG.get(), PaybackTagScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Envelope.EntityTypes.PIGEON.get(), PigeonRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PigeonRenderer.PIGEON_LAYER, PigeonModel::createLayerDefinition);
        event.registerLayerDefinition(PigeonBackpackLayer.PIGEON_BACKPACK, PigeonModel::createLayerDefinition);
        event.registerLayerDefinition(PigeonHatLayer.PIGEON_HAT, PigeonModel::createLayerDefinition);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(Sealable::getSealOverlayColor, Envelope.Items.SEALED_LETTER.get());
        event.register(Sealable::getSealOverlayColor, Envelope.Items.SEALED_PACKAGE.get());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(Sealable::getSealOverlayColor, Envelope.Blocks.SEALED_PACKAGE.get());
    }
}