package io.github.mortuusars.envelope.neoforge;

import com.mojang.serialization.MapCodec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.neoforge.loot.ConfigurableAddTableLootModifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(Envelope.ID)
public class EnvelopeNeoForge {
    public EnvelopeNeoForge(ModContainer container) {
        Envelope.init();

        container.registerConfig(ModConfig.Type.SERVER, Config.Server.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, Config.Common.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.Client.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            EnvelopeNeoForgeClient.init(container);
        }
    }

    public static class LootModifiers {
        private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
              DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Envelope.ID);

        public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<ConfigurableAddTableLootModifier>> ADD_TABLE =
              LOOT_MODIFIERS.register("add_table", () -> ConfigurableAddTableLootModifier.CODEC);
    }
}