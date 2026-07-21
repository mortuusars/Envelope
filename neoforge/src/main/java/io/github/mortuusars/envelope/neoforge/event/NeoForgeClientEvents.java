package io.github.mortuusars.envelope.neoforge.event;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.client.gui.screen.*;
import io.github.mortuusars.envelope.client.model.CharredPigeonModel;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.renderer.entity.CharredPigeonRenderer;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.CharredPigeonBackpackLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonBackpackLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonHatLayer;
import io.github.mortuusars.envelope.world.item.Sealable;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Envelope.ID, value = Dist.CLIENT)
public class NeoForgeClientEvents {
    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EnvelopeClient.init();
            ItemBlockRenderTypes.setRenderLayer(Envelope.Blocks.LETTER.get(), RenderType.cutout());
        });
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
        event.registerEntityRenderer(Envelope.EntityTypes.CHARRED_PIGEON.get(), CharredPigeonRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PigeonRenderer.MODEL_LAYER, PigeonModel::createLayerDefinition);
        event.registerLayerDefinition(PigeonBackpackLayer.MODEL_LAYER, PigeonModel::createLayerDefinition);
        event.registerLayerDefinition(PigeonHatLayer.MODEL_LAYER, PigeonModel::createLayerDefinition);
        event.registerLayerDefinition(CharredPigeonRenderer.MODEL_LAYER, CharredPigeonModel::createLayerDefinition);
        event.registerLayerDefinition(CharredPigeonBackpackLayer.MODEL_LAYER, CharredPigeonModel::createLayerDefinition);
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