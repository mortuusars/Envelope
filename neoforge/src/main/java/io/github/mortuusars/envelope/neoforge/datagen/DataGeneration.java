package io.github.mortuusars.envelope.neoforge.datagen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.neoforge.datagen.client.ModelsDatagen;
import io.github.mortuusars.envelope.neoforge.datagen.server.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

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

        DatapackBuiltinEntriesProvider datapackRegistries = new BuiltInDatapackEntries(output, registries);
        generator.addProvider(event.includeServer(), datapackRegistries);
        generator.addProvider(event.includeServer(), new SealImpressionTagsDatagen(output, datapackRegistries.getRegistryProvider(), Envelope.ID, existingFileHelper));
    }

}
