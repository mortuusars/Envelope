package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.neoforge.datagen.server.loot.BlockLootProvider;
import io.github.mortuusars.envelope.neoforge.datagen.server.loot.ChestLootProvider;
import io.github.mortuusars.envelope.neoforge.datagen.server.loot.EntityLootProvider;
import io.github.mortuusars.envelope.neoforge.datagen.server.loot.GameplayLootProvider;
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
                Collections.emptySet(),
                List.of(
                        new LootTableProvider.SubProviderEntry(BlockLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(EntityLootProvider::new, LootContextParamSets.ENTITY),
                        new LootTableProvider.SubProviderEntry(ChestLootProvider::new, LootContextParamSets.CHEST),
                        new LootTableProvider.SubProviderEntry(GameplayLootProvider::new, LootContextParamSets.BLOCK)
                ),
                registries
        );
    }
}
