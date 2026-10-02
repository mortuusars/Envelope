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

import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Envelope.ID)
public class DataGeneration {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> registries = new BuiltInDatapackEntries(output, event.getLookupProvider()).getRegistryProvider();
        DatapackBuiltinEntriesProvider datapackEntries = new BuiltInDatapackEntries(output, registries);
        CompletableFuture<HolderLookup.Provider> datapackRegistries = datapackEntries.getRegistryProvider();

        // -- Server

        generator.addProvider(event.includeServer(), new RecipesDatagen(output, datapackRegistries));
        TagsDatagen.BlockTagsDatagen blockTags = new TagsDatagen.BlockTagsDatagen(output, registries, helper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new TagsDatagen.ItemTags(output, registries, blockTags.contentsGetter(), helper));
        generator.addProvider(event.includeServer(), new TagsDatagen.DamageTypeTags(output, registries, helper));
        generator.addProvider(event.includeServer(), new TagsDatagen.EntityTypeTagsDatagen(output, registries, helper));
        generator.addProvider(event.includeServer(), LootTablesDatagen.create(output, registries));

        generator.addProvider(event.includeServer(), datapackEntries);
        generator.addProvider(event.includeServer(), new TagsDatagen.SealImpressionTagsDatagen(output, datapackRegistries, helper));
        generator.addProvider(event.includeServer(), new TagsDatagen.ServiceAddressTagsDatagen(output, datapackRegistries, helper));

        generator.addProvider(event.includeServer(), new AdvancementsDatagen(output, datapackRegistries, helper, List.of(
              new AdvancementsDatagen.Generator()
        )));


        // -- Client

        generator.addProvider(event.includeClient(), new ModelsDatagen(output, helper));
    }
}
