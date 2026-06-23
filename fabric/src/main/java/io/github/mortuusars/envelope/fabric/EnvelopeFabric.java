package io.github.mortuusars.envelope.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.command.EnvelopeCommand;
import io.github.mortuusars.envelope.event.CommonEvents;
import io.github.mortuusars.envelope.event.ServerEvents;
import io.github.mortuusars.envelope.network.fabric.FabricC2SPackets;
import io.github.mortuusars.envelope.network.fabric.FabricS2CPackets;
import io.github.mortuusars.envelope.world.entity.CharredPigeon;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.PigeonVariant;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.config.ModConfig;

public class EnvelopeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Envelope.init();

        NeoForgeConfigRegistry.INSTANCE.register(Envelope.ID, ModConfig.Type.SERVER, Config.Server.SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(Envelope.ID, ModConfig.Type.CLIENT, Config.Client.SPEC);

        CommonEvents.commonSetup();

        DynamicRegistries.registerSynced(Envelope.Registries.PIGEON_VARIANT, PigeonVariant.DIRECT_CODEC, PigeonVariant.DIRECT_CODEC);
        DynamicRegistries.registerSynced(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, ServiceAddressDefinition.DIRECT_CODEC, ServiceAddressDefinition.DIRECT_CODEC);
        DynamicRegistries.registerSynced(Envelope.Registries.SEAL_MATERIAL, SealMaterial.DIRECT_CODEC, SealMaterial.DIRECT_CODEC);
        DynamicRegistries.registerSynced(Envelope.Registries.SEAL_IMPRESSION, SealImpression.DIRECT_CODEC, SealImpression.DIRECT_CODEC);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(event -> {
            Envelope.Items.PIGEONHOLES.forEach(item -> event.accept(item.get()));
            event.accept(Envelope.Items.PAPER_BOX.get());
            event.accept(Envelope.Items.MAILBOX.get());
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(event -> {
            event.accept(Envelope.Items.LETTER_AND_QUILL.get());
            event.accept(Envelope.Items.LETTER.get());
            event.accept(Envelope.Items.SEALED_LETTER.get());
            event.accept(Envelope.Items.PACKAGE.get());
            event.accept(Envelope.Items.SEALED_PACKAGE.get());
            event.accept(Envelope.Items.ADDRESS_TAG.get());
            event.accept(Envelope.Items.PAYBACK_TAG.get());
            event.accept(Envelope.Items.SEAL_STAMP.get());
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(event -> {
            event.accept(Envelope.Items.PIGEON_SPAWN_EGG.get());
            event.accept(Envelope.Items.CHARRED_PIGEON_SPAWN_EGG.get());
        });

        FabricDefaultAttributeRegistry.register(Envelope.EntityTypes.PIGEON.get(), Pigeon.createAttributes().build());

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlatformImpl.server = server;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PlatformImpl.server = null;
        });

        ServerTickEvents.END_SERVER_TICK.register(ServerEvents::serverTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerEvents.playerLogin(handler.getPlayer());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EnvelopeCommand.register(dispatcher, registryAccess);
        });

        BiomeModifications.addSpawn(biomeSelector -> biomeSelector.hasTag(Envelope.Tags.Biomes.ALLOWS_PIGEON_SPAWNS),
                MobCategory.CREATURE, Envelope.EntityTypes.PIGEON.get(), 2, 3, 6);
        BiomeModifications.addSpawn(biomeSelector -> biomeSelector.hasTag(Envelope.Tags.Biomes.ALLOWS_CHARRED_PIGEON_SPAWNS),
              MobCategory.MONSTER, Envelope.EntityTypes.CHARRED_PIGEON.get(), 1, 1, 1);
        BiomeModifications.create(Envelope.resource("charred_pigeon_spawn_cost"))
              .add(ModificationPhase.ADDITIONS, biomeSelector -> biomeSelector.hasTag(Envelope.Tags.Biomes.ALLOWS_CHARRED_PIGEON_SPAWNS),
                    context -> context.getSpawnSettings().setSpawnCost(Envelope.EntityTypes.CHARRED_PIGEON.get(), 0.7, 0.15));

        SpawnPlacements.register(Envelope.EntityTypes.PIGEON.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING, Pigeon::checkSpawnRules);
        SpawnPlacements.register(Envelope.EntityTypes.CHARRED_PIGEON.get(), SpawnPlacementTypes.ON_GROUND,
              Heightmap.Types.MOTION_BLOCKING, CharredPigeon::checkSpawnRules);

        FlammableBlockRegistry.getDefaultInstance().add(Envelope.Blocks.PAPER_BOX.get(), 50, 15);
        FlammableBlockRegistry.getDefaultInstance().add(Envelope.Blocks.PACKAGE.get(), 50, 15);
        FlammableBlockRegistry.getDefaultInstance().add(Envelope.Blocks.LETTER.get(), 200, 10);
        Envelope.Blocks.PIGEONHOLES.forEach((id, block) -> {
            FlammableBlockRegistry.getDefaultInstance().add(block.get(), 20, 15);
        });

        FabricC2SPackets.register();
        FabricS2CPackets.register();
    }
}
