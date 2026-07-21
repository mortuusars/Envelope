package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.neoforge.datagen.server.loot.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LootTablesDatagen extends LootTableProvider {
    private LootTablesDatagen(PackOutput output, Set<ResourceKey<LootTable>> requiredTables, List<SubProviderEntry> subProviders,
                              CompletableFuture<HolderLookup.Provider> registries) {
        super(output, requiredTables, subProviders, registries);
    }

    @Override
    protected void validate(@NotNull WritableRegistry<LootTable> writableregistry, @NotNull ValidationContext validationcontext,
                            ProblemReporter.@NotNull Collector problemreporter$collector) {
        // Disabled the validation so we can reference vanilla loot tables in ours
        // super.validate(writableregistry, validationcontext, problemreporter$collector);
    }

    public static LootTablesDatagen create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new LootTablesDatagen(
                output,
                Collections.emptySet(),
                List.of(
                        new LootTableProvider.SubProviderEntry(BlockLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(EntityLootProvider::new, LootContextParamSets.ENTITY),
                        new LootTableProvider.SubProviderEntry(ChestLootProvider::new, LootContextParamSets.CHEST),
                        new LootTableProvider.SubProviderEntry(PackageLootProvider::new, LootContextParamSets.CHEST),
                        new LootTableProvider.SubProviderEntry(GameplayBlockLootProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(GameplayEquipmentLootProvider::new, LootContextParamSets.EQUIPMENT)
                ),
                registries
        );
    }
}
