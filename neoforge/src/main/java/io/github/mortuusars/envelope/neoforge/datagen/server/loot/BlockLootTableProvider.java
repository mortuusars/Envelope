package io.github.mortuusars.envelope.neoforge.datagen.server.loot;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PaperBoxBlock;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        List<Block> blocks = new ArrayList<>(
              Envelope.Blocks.PIGEONHOLES.values().stream().map(Supplier::get).toList());
        blocks.add(Envelope.Blocks.PAPER_BOX.get());
        return blocks;
    }

    @Override
    public void generate() {
        Envelope.Blocks.PIGEONHOLES.forEach((id, block) -> add(block.get(), this::createPigeonholeDrop));
        add(Envelope.Blocks.PAPER_BOX.get(), this::createPaperBoxDrops);
    }

    public LootTable.Builder createPaperBoxDrops(Block paperBoxBlock) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
              .setRolls(ConstantValue.exactly(1.0F))
              .add(applyExplosionDecay(paperBoxBlock, LootItem.lootTableItem(paperBoxBlock)
                          .apply(List.of(2, 3, 4), count -> SetItemCountFunction.setCount(ConstantValue.exactly(count))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(paperBoxBlock)
                                      .setProperties(StatePropertiesPredicate.Builder.properties()
                                            .hasProperty(PaperBoxBlock.BOXES, count)))
                          )
                    )
              )
        );
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
