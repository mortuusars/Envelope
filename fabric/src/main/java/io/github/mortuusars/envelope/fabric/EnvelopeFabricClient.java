package io.github.mortuusars.envelope.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.client.gui.screen.*;
import io.github.mortuusars.envelope.client.model.BatBackpackModel;
import io.github.mortuusars.envelope.client.model.CharredPigeonModel;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.renderer.entity.CharredPigeonRenderer;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.BatBackpackLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.CharredPigeonBackpackLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonBackpackLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonHatLayer;
import io.github.mortuusars.envelope.world.item.Sealable;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public class EnvelopeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EnvelopeClient.init();

        ConfigScreenFactoryRegistry.INSTANCE.register(Envelope.ID, ConfigurationScreen::new);

        EntityRendererRegistry.register(Envelope.EntityTypes.PIGEON.get(), PigeonRenderer::new);
        EntityRendererRegistry.register(Envelope.EntityTypes.CHARRED_PIGEON.get(), CharredPigeonRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(PigeonRenderer.MODEL_LAYER, PigeonModel::createLayerDefinition);
        EntityModelLayerRegistry.registerModelLayer(PigeonBackpackLayer.MODEL_LAYER, PigeonModel::createLayerDefinition);
        EntityModelLayerRegistry.registerModelLayer(PigeonHatLayer.MODEL_LAYER, PigeonModel::createLayerDefinition);
        EntityModelLayerRegistry.registerModelLayer(CharredPigeonRenderer.MODEL_LAYER, CharredPigeonModel::createLayerDefinition);
        EntityModelLayerRegistry.registerModelLayer(CharredPigeonBackpackLayer.MODEL_LAYER, CharredPigeonModel::createLayerDefinition);
        EntityModelLayerRegistry.registerModelLayer(BatBackpackLayer.MODEL_LAYER, BatBackpackModel::createLayerDefinition);

        BlockRenderLayerMap.INSTANCE.putBlock(Envelope.Blocks.LETTER.get(), RenderType.cutout());

        ColorProviderRegistry.ITEM.register(Sealable::getSealOverlayColor, Envelope.Items.SEALED_LETTER.get());
        ColorProviderRegistry.ITEM.register(Sealable::getSealOverlayColor, Envelope.Items.SEALED_PACKAGE.get());
        ColorProviderRegistry.BLOCK.register(Sealable::getSealOverlayColor, Envelope.Blocks.SEALED_PACKAGE.get());

        MenuScreens.register(Envelope.MenuTypes.MAILBOX.get(), MailboxScreen::new);
        MenuScreens.register(Envelope.MenuTypes.PACKING.get(), PackingScreen::new);
        MenuScreens.register(Envelope.MenuTypes.PACKAGE.get(), PackageScreen::new);
        MenuScreens.register(Envelope.MenuTypes.PAYBACK_PACKING.get(), PaybackPackingScreen::new);
        MenuScreens.register(Envelope.MenuTypes.PAYBACK_PACKAGE.get(), PaybackPackageScreen::new);
        MenuScreens.register(Envelope.MenuTypes.PAYBACK_TAG.get(), PaybackTagScreen::new);
    }
}
