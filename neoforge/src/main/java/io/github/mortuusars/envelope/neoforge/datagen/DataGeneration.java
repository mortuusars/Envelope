package io.github.mortuusars.envelope.neoforge.datagen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.neoforge.datagen.client.ModelsDatagen;
import io.github.mortuusars.envelope.neoforge.datagen.server.*;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Envelope.ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGeneration {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ModelsDatagen(output, existingFileHelper));

        generator.addProvider(event.includeServer(), new RecipesDatagen(output, registries));
        BlockTagsDatagen blockTags = new BlockTagsDatagen(output, registries, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ItemTagsDatagen(output, registries, blockTags.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(), LootTablesDatagen.create(output, registries));

        DatapackBuiltinEntriesProvider datapackRegistries = new EnvelopeDatapackRegistries(output, registries);
        generator.addProvider(event.includeServer(), datapackRegistries);
        generator.addProvider(event.includeServer(), new SealImpressionDatagen(output, datapackRegistries.getRegistryProvider(), Envelope.ID, existingFileHelper));
    }

    public static class EnvelopeDatapackRegistries extends DatapackBuiltinEntriesProvider {
        public static final RegistrySetBuilder REGISTRIES = new RegistrySetBuilder()
              .add(Envelope.Registries.SEAL_MATERIAL, SealMaterial::bootstrap)
              .add(Envelope.Registries.SEAL_IMPRESSION, SealImpression::bootstrap);

        public EnvelopeDatapackRegistries(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
            super(output, provider, REGISTRIES, Set.of(Envelope.ID));
        }
    }
}
