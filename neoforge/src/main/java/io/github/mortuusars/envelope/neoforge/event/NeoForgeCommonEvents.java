package io.github.mortuusars.envelope.neoforge.event;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.command.EnvelopeCommand;
import io.github.mortuusars.envelope.event.CommonEvents;
import io.github.mortuusars.envelope.event.ServerEvents;
import io.github.mortuusars.envelope.world.entity.CharredPigeon;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.PigeonVariant;
import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = Envelope.ID)
public class NeoForgeCommonEvents {
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CommonEvents::commonSetup);
    }

    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            Envelope.Items.PIGEONHOLES.forEach(item -> event.accept(item.get()));
            event.accept(Envelope.Items.PAPER_BOX.get());
            event.accept(Envelope.Items.MAILBOX.get());
        }
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(Envelope.Items.LETTER_AND_QUILL.get());
            event.accept(Envelope.Items.LETTER.get());
            event.accept(Envelope.Items.SEALED_LETTER.get());
            event.accept(Envelope.Items.PACKAGE.get());
            event.accept(Envelope.Items.SEALED_PACKAGE.get());
            event.accept(Envelope.Items.ADDRESS_TAG.get());
            event.accept(Envelope.Items.PAYBACK_TAG.get());
            event.accept(Envelope.Items.SEAL_STAMP.get());
        }
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(Envelope.Items.PIGEON_SPAWN_EGG.get());
            event.accept(Envelope.Items.CHARRED_PIGEON_SPAWN_EGG.get());
        }
    }

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(Envelope.EntityTypes.PIGEON.get(), Pigeon.createAttributes().build());
        event.put(Envelope.EntityTypes.CHARRED_PIGEON.get(), CharredPigeon.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(Envelope.EntityTypes.PIGEON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING,
              Pigeon::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(Envelope.EntityTypes.CHARRED_PIGEON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING,
              CharredPigeon::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void addDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Envelope.Registries.PIGEON_VARIANT, PigeonVariant.DIRECT_CODEC, PigeonVariant.DIRECT_CODEC);
        event.dataPackRegistry(Envelope.Registries.SERVICE_ADDRESS_DEFINITION, ServiceAddressDefinition.DIRECT_CODEC, ServiceAddressDefinition.DIRECT_CODEC);
        event.dataPackRegistry(Envelope.Registries.SEAL_MATERIAL, SealMaterial.DIRECT_CODEC, SealMaterial.DIRECT_CODEC);
        event.dataPackRegistry(Envelope.Registries.SEAL_SYMBOL, SealSymbol.DIRECT_CODEC, SealSymbol.DIRECT_CODEC);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        EnvelopeCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        ServerEvents.serverStarted(event.getServer());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post tick) {
        ServerEvents.serverTick(tick.getServer());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        CommonEvents.levelTick(event.getLevel());
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerEvents.playerLogin(player);
        }
    }

    @SubscribeEvent
    public static void entityLeaveLevel(EntityLeaveLevelEvent event) {
        CommonEvents.entityLeaveLevel(event.getLevel(), event.getEntity());
    }

    @SubscribeEvent
    public static void entityDeath(LivingDeathEvent event) {
        CommonEvents.livingDeath(event.getEntity(), event.getSource());
    }
}