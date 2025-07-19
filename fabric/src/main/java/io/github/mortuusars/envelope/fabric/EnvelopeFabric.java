package io.github.mortuusars.envelope.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.command.MailCommand;
import io.github.mortuusars.envelope.event.ServerEvents;
import io.github.mortuusars.envelope.network.fabric.FabricC2SPackets;
import io.github.mortuusars.envelope.network.fabric.FabricS2CPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.config.ModConfig;

public class EnvelopeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Envelope.init();

        NeoForgeConfigRegistry.INSTANCE.register(Envelope.ID, ModConfig.Type.SERVER, Config.Server.SPEC);
//        NeoForgeConfigRegistry.INSTANCE.register(Envelope.ID, ModConfig.Type.COMMON, Config.Common.SPEC);
//        NeoForgeConfigRegistry.INSTANCE.register(Envelope.ID, ModConfig.Type.CLIENT, Config.Client.SPEC);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(content -> {
            content.accept(Envelope.Items.MAILBOX.get());
            content.accept(Envelope.Items.CARDBOARD_BOX.get());
            content.accept(Envelope.Items.PACKAGE.get());
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(Envelope.Items.LETTER.get());
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlatformHelperImpl.server = server;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PlatformHelperImpl.server = null;
        });

        ServerTickEvents.END_SERVER_TICK.register(ServerEvents::serverTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerEvents.playerLogin(handler.getPlayer());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MailCommand.register(dispatcher);
        });

        FabricC2SPackets.register();
        FabricS2CPackets.register();
    }
}
