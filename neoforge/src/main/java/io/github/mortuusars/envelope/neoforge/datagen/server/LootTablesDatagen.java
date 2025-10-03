package io.github.mortuusars.envelope.neoforge.datagen.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LootTablesDatagen extends LootTableProvider {
    private LootTablesDatagen(PackOutput output, Set<ResourceKey<LootTable>> requiredTables, List<SubProviderEntry> subProviders, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, requiredTables, subProviders, registries);
    }

    public static LootTablesDatagen create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTablesDatagen(
                output,
                Collections.emptySet(), // Envelope.LootTables.all()
                List.of(
                        new LootTableProvider.SubProviderEntry(EntityLootDatagen::new, LootContextParamSets.ENTITY)
                ),
                registries
        );
    }
}
