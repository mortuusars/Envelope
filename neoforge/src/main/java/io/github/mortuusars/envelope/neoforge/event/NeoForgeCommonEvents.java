package io.github.mortuusars.envelope.neoforge.event;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.command.EnvelopeCommand;
import io.github.mortuusars.envelope.event.CommonEvents;
import io.github.mortuusars.envelope.event.ServerEvents;
import io.github.mortuusars.envelope.neoforge.RegisterImpl;
import io.github.mortuusars.envelope.network.neoforge.PacketsImpl;
import io.github.mortuusars.envelope.network.packet.C2SPackets;
import io.github.mortuusars.envelope.network.packet.CommonPackets;
import io.github.mortuusars.envelope.network.packet.Packet;
import io.github.mortuusars.envelope.network.packet.S2CPackets;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;

public class NeoForgeCommonEvents {
    @EventBusSubscriber(modid = Envelope.ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                CommonEvents.commonSetup();
                for (Map.Entry<ResourceLocation, StatFormatter> entry : RegisterImpl.STATS.entrySet()) {
                    Stats.CUSTOM.get(entry.getKey(), entry.getValue());
                }
            });
        }

        @SuppressWarnings("unchecked")
        @SubscribeEvent
        public static void registerPackets(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            // This monstrosity is to avoid having to define packets for forge and fabric separately.
            for (CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload> definition : S2CPackets.getDefinitions()) {
                registrar.playToClient((CustomPacketPayload.Type<Packet>) definition.type(),
                        (StreamCodec<FriendlyByteBuf, Packet>) definition.codec(), PacketsImpl::handle);
            }

            for (CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload> definition : C2SPackets.getDefinitions()) {
                registrar.playToServer((CustomPacketPayload.Type<Packet>) definition.type(),
                        (StreamCodec<FriendlyByteBuf, Packet>) definition.codec(), PacketsImpl::handle);
            }

            for (CustomPacketPayload.TypeAndCodec<? extends FriendlyByteBuf, ? extends CustomPacketPayload> definition : CommonPackets.getDefinitions()) {
                registrar.playBidirectional((CustomPacketPayload.Type<Packet>) definition.type(),
                        (StreamCodec<FriendlyByteBuf, Packet>) definition.codec(), PacketsImpl::handle);
            }
        }

        @SubscribeEvent
        public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
                Envelope.Items.PIGEONHOLES.forEach(item -> event.accept(item.get()));
                event.accept(Envelope.Items.PAPER_BOX.get());
                event.accept(Envelope.Items.PACKAGE.get());
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.accept(Envelope.Items.LETTER_AND_QUILL.get());
                event.accept(Envelope.Items.LETTER.get());
                event.accept(Envelope.Items.SEALED_LETTER.get());
                event.accept(Envelope.Items.OPENED_SEALED_LETTER.get());
                event.accept(Envelope.Items.TATTERED_LETTER.get());
                event.accept(Envelope.Items.ADDRESS_TAG.get());
                event.accept(Envelope.Items.SEAL_STAMP.get());
            }
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                event.accept(Envelope.Items.PIGEON_SPAWN_EGG.get());
            }
        }

        @SubscribeEvent
        public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
            event.put(Envelope.EntityTypes.PIGEON.get(), Pigeon.createAttributes().build());
        }

        @SubscribeEvent
        public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
            event.register(Envelope.EntityTypes.PIGEON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING,
                    Pigeon::checkPigeonSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    @EventBusSubscriber(modid = Envelope.ID, bus = EventBusSubscriber.Bus.GAME)
    public static class GameBus {
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
    }
}