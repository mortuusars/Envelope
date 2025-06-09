package io.github.mortuusars.envelope.neoforge.event;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.EnvelopeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class NeoForgeClientEvents {
    @EventBusSubscriber(modid = Envelope.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(EnvelopeClient::init);
        }
    }

    // @EventBusSubscriber(modid = Envelope.ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class GameBus {
    }
}