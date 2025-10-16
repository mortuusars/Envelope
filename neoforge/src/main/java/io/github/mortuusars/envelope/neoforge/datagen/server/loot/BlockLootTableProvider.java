package io.github.mortuusars.envelope.neoforge.datagen.server.loot;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.function.Supplier;

public class BlockLootTableProvider extends BlockLootSubProvider {
    public BlockLootTableProvider(HolderLookup.Provider registries) {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    /**
     * This method is for the datagen to check that all known entries have a loot table associated with them.
     */
    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return Envelope.Blocks.PIGEONHOLES.values().stream()
              .map(Supplier::get)
              .map(Block.class::cast)
              .toList();
    }

    @Override
    public void generate() {
        Envelope.Blocks.PIGEONHOLES.forEach((id, block) -> add(block.get(), this::createPigeonholeDrop));
    }

    public LootTable.Builder createPigeonholeDrop(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
              .setRolls(ConstantValue.exactly(1.0F))
              .add(LootItem.lootTableItem(block)
                    .when(hasSilkTouch())
                    .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                          .include(Envelope.DataComponents.PIGEONS))
                    .apply(CopyBlockState.copyState(block).copy(PigeonholeBlock.WASTE_LEVEL))
                    .otherwise(LootItem.lootTableItem(block))
              )
        );
    }
}
