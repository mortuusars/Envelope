package io.github.mortuusars.envelope.fabric;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.fabric.FabricC2SPackets;
import io.github.mortuusars.envelope.network.fabric.FabricS2CPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class EnvelopeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Envelope.init();

//        NeoForgeConfigRegistry.INSTANCE.register(Envelope.ID, ModConfig.Type.SERVER, Config.Server.SPEC);
//        NeoForgeConfigRegistry.INSTANCE.register(Envelope.ID, ModConfig.Type.COMMON, Config.Common.SPEC);
//        NeoForgeConfigRegistry.INSTANCE.register(Envelope.ID, ModConfig.Type.CLIENT, Config.Client.SPEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlatformHelperImpl.server = server;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PlatformHelperImpl.server = null;
        });

        FabricC2SPackets.register();
        FabricS2CPackets.register();
    }
}
