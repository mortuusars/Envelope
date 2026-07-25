package io.github.mortuusars.envelope.neoforge.datagen.server.loot;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddresses;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.BiConsumer;

public class PackageLootProvider implements LootTableSubProvider {
    protected final HolderLookup.Provider registries;

    public PackageLootProvider(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        lostMail(output);
        charredMail(output);
    }

    private void lostMail(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(
              Envelope.LootTables.LOST_MAIL,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.LOST_MAIL_JUNK).setWeight(3))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.LOST_MAIL_PLANTS).setWeight(2))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.LOST_MAIL_BLOCKS).setWeight(2))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.LOST_MAIL_METALS))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.LOST_MAIL_TOOLS))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.LOST_MAIL_WEAPONS))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.LOST_MAIL_VALUABLES))
              )
        );

        output.accept(
              Envelope.LootTables.LOST_MAIL_JUNK,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(2.0F, 10.0F))
                    .add(LootItem.lootTableItem(Items.STICK).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(6.0F, 16.0F))))
                    .add(LootItem.lootTableItem(Items.COAL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 10.0F))))
                    .add(LootItem.lootTableItem(Items.PAPER).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.HONEYCOMB).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.CLAY_BALL).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 10.0F))))
                    .add(LootItem.lootTableItem(Items.YELLOW_DYE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.FLOWER_POT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.WHITE_WOOL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Blocks.BLACK_WOOL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Blocks.GRAY_WOOL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Blocks.BROWN_WOOL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Blocks.LIGHT_GRAY_WOOL).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.FEATHER).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.FLINT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.WHEAT_SEEDS).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.LEATHER).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.COMPASS).setWeight(5))
              )
        );

        output.accept(
              Envelope.LootTables.LOST_MAIL_PLANTS,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(2.0F, 8.0F))
                    .add(LootItem.lootTableItem(Items.FERN).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.LARGE_FERN).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.SHORT_GRASS).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.TALL_GRASS).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.SUNFLOWER).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.DANDELION).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.BLUE_ORCHID).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.SWEET_BERRIES).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Blocks.OAK_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.SPRUCE_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.BIRCH_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.ACACIA_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.CHERRY_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.DARK_OAK_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.JUNGLE_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.BAMBOO_SAPLING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.MANGROVE_PROPAGULE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Blocks.SPORE_BLOSSOM).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
              )
        );

        output.accept(
              Envelope.LootTables.LOST_MAIL_BLOCKS,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(10.0F, 32.0F))
                    .add(LootItem.lootTableItem(Items.STONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.DIORITE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.ANDESITE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.GRANITE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.DRIPSTONE_BLOCK).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.CALCITE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.TERRACOTTA).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.TUFF).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.SANDSTONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(5.0F, 12.0F))))
              )
        );

        output.accept(
              Envelope.LootTables.LOST_MAIL_METALS,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(2.0F, 10.0F))
                    .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.IRON_BLOCK).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.COPPER_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.COPPER_BLOCK).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
                    .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Items.GOLD_BLOCK).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.CHAIN).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
              )
        );

        output.accept(
              Envelope.LootTables.LOST_MAIL_TOOLS,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(2.0F, 8.0F))
                    .add(LootItem.lootTableItem(Items.STONE_PICKAXE).setWeight(12))
                    .add(LootItem.lootTableItem(Items.STONE_SHOVEL).setWeight(12))
                    .add(LootItem.lootTableItem(Items.STONE_HOE).setWeight(12))
                    .add(LootItem.lootTableItem(Items.STONE_AXE).setWeight(12))
                    .add(LootItem.lootTableItem(Items.IRON_PICKAXE).setWeight(10))
                    .add(LootItem.lootTableItem(Items.IRON_SHOVEL).setWeight(10))
                    .add(LootItem.lootTableItem(Items.IRON_HOE).setWeight(10))
                    .add(LootItem.lootTableItem(Items.IRON_AXE).setWeight(10))
                    .add(LootItem.lootTableItem(Items.DIAMOND_PICKAXE).setWeight(2))
                    .add(LootItem.lootTableItem(Items.DIAMOND_SHOVEL).setWeight(2))
                    .add(LootItem.lootTableItem(Items.DIAMOND_HOE).setWeight(2))
                    .add(LootItem.lootTableItem(Items.DIAMOND_AXE).setWeight(2))
                    .add(LootItem.lootTableItem(Items.ANVIL).setWeight(2))
                    .add(LootItem.lootTableItem(Items.GRINDSTONE).setWeight(2))
                    .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F))))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
              )
        );

        output.accept(
              Envelope.LootTables.LOST_MAIL_WEAPONS,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(2.0F, 8.0F))
                    .add(LootItem.lootTableItem(Items.STONE_SWORD).setWeight(20))
                    .add(LootItem.lootTableItem(Items.IRON_SWORD).setWeight(12))
                    .add(LootItem.lootTableItem(Items.DIAMOND_SWORD).setWeight(6))
                    .add(LootItem.lootTableItem(Items.BOW).setWeight(20))
                    .add(LootItem.lootTableItem(Items.CROSSBOW).setWeight(12))
                    .add(LootItem.lootTableItem(Items.ARROW).setWeight(20).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 16.0F))))
                    .add(LootItem.lootTableItem(Items.GRINDSTONE).setWeight(6))
                    .add(LootItem.lootTableItem(Items.IRON_HORSE_ARMOR).setWeight(20))
                    .add(LootItem.lootTableItem(Items.GOLDEN_HORSE_ARMOR).setWeight(12))
                    .add(LootItem.lootTableItem(Items.DIAMOND_HORSE_ARMOR).setWeight(6))
                    .add(LootItem.lootTableItem(Items.MACE).setWeight(1))
                    .add(LootItem.lootTableItem(Items.TRIDENT).setWeight(1))
              )
        );

        output.accept(
              Envelope.LootTables.LOST_MAIL_VALUABLES,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(2.0F, 8.0F))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(50).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 16.0F))))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(50).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 16.0F))))
                    .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Items.LAPIS_LAZULI).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.BOOK).setWeight(25).apply(EnchantWithLevelsFunction.enchantWithLevels(registries, ConstantValue.exactly(30.0F))))
                    .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(25).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.PRISMARINE_CRYSTALS).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.EMERALD_BLOCK).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
              )
        );
    }

    private void charredMail(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        LootTable barteringTable = LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(1, 2))
                    .add(NestedLootTable.lootTableReference(BuiltInLootTables.PIGLIN_BARTERING)))
              .build();

        output.accept(
              Envelope.LootTables.NETHER_LOST_MAIL,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(NestedLootTable.inlineLootTable(barteringTable).setWeight(3))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.LOST_MAIL)))
        );
    }
}