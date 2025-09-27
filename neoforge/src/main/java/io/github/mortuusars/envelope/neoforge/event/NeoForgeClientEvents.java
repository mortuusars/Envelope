package io.github.mortuusars.envelope.neoforge.event;

import com.mojang.datafixers.util.Either;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.client.gui.screen.PigeonholeAddressScreen;
import io.github.mortuusars.envelope.client.gui.screen.PigeonholeScreen;
import io.github.mortuusars.envelope.client.model.PigeonFancyHatModel;
import io.github.mortuusars.envelope.client.model.PigeonLegBandModel;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.model.geom.EnvelopeModelLayers;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.mail.Address;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.Optional;

public class NeoForgeClientEvents {
    @EventBusSubscriber(modid = Envelope.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(EnvelopeClient::init);
        }

        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(Envelope.MenuTypes.PIGEONHOLE.get(), PigeonholeScreen::new);
            event.register(Envelope.MenuTypes.PIGEONHOLE_ADDRESS.get(), PigeonholeAddressScreen::new);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(Envelope.EntityTypes.PIGEON.get(), PigeonRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(EnvelopeModelLayers.PIGEON, PigeonModel::createLayerDefinition);
            event.registerLayerDefinition(EnvelopeModelLayers.PIGEON_LEG_BAND, PigeonLegBandModel::createLayerDefinition);
            event.registerLayerDefinition(EnvelopeModelLayers.PIGEON_FANCY_HAT, PigeonFancyHatModel::createLayerDefinition);
        }
    }

    // @EventBusSubscriber(modid = Envelope.ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class GameBus {

    }
}