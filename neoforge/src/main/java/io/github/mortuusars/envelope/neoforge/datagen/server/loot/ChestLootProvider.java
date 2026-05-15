package io.github.mortuusars.envelope.neoforge.datagen.server.loot;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddresses;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public record ChestLootProvider(HolderLookup.Provider registries) implements LootTableSubProvider {
    @Override
    public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        lostMail(output);
        collapsedMailHub(output);
    }

    private void collapsedMailHub(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(
              Envelope.LootTables.COLLAPSED_MAIL_HUB_STORAGE,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.COLLAPSED_MAIL_HUB_STORAGE_MAIL).setWeight(2))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.COLLAPSED_MAIL_HUB_STORAGE_MATERIALS))
                    .add(NestedLootTable.lootTableReference(Envelope.LootTables.COLLAPSED_MAIL_HUB_STORAGE_STAMPS))
              )
        );

        MutableComponent automatedSupplyService = ServiceAddress.getOrThrow(registries, ServiceAddresses.AUTOMATED_SUPPLY_SERVICE).format().toComponent();

        output.accept(
              Envelope.LootTables.COLLAPSED_MAIL_HUB_STORAGE_MAIL,
              LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                          .setRolls(UniformGenerator.between(1.0F, 3.0F))
                          .add(LootItem.lootTableItem(Items.PAPER).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                          .add(LootItem.lootTableItem(Envelope.Items.PAPER_BOX.get()).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))))
                          .add(LootItem.lootTableItem(Envelope.Items.PACKAGE.get())
                                .apply(SetComponentsFunction.setComponent(DataComponents.ITEM_NAME, Component.translatable("item.envelope.lost_mail")))
                                .apply(SetComponentsFunction.setComponent(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(Envelope.LootTables.LOST_MAIL, 0L)))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))))
                    .withPool(LootPool.lootPool()
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.common_1")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.common_2")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.common_3")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.common_4")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.common_5")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.common_6")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.common_7", automatedSupplyService)))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.nether_1")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.nether_2")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.nether_3")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.nether_4")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.nether_5")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.nether_6")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.nether_7")))
                          .add(letter(Component.translatable("item.envelope.lost_mail"), Component.translatable("letter.envelope.collapsed_mail_hub.nether_8")))
                          .add(EmptyLootItem.emptyItem().setWeight(5))
                    ));

        output.accept(
              Envelope.LootTables.COLLAPSED_MAIL_HUB_STORAGE_MATERIALS,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(1.0F, 4.0F))
                    .add(LootItem.lootTableItem(Items.PAPER).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Envelope.Items.ADDRESS_TAG.get()).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Envelope.Items.PAYBACK_TAG.get()).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.PAPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.FEATHER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.BLACK_DYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Items.BOOK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                    .add(LootItem.lootTableItem(Envelope.Items.LETTER_AND_QUILL.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                    .add(LootItem.lootTableItem(Items.WRITABLE_BOOK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
              )
        );

        output.accept(
              Envelope.LootTables.COLLAPSED_MAIL_HUB_STORAGE_STAMPS,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(1.0F, 3.0F))
                    .add(LootItem.lootTableItem(Items.PAPER).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                    .add(LootItem.lootTableItem(Envelope.Items.SEAL_STAMP.get()).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                    .add(LootItem.lootTableItem(Envelope.Items.SEAL_STAMP.get())
                          .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.SEAL_STAMP_IMPRESSION, SealImpression.getOrThrow(registries, SealImpression.HEART))))
                    .add(LootItem.lootTableItem(Envelope.Items.SEAL_STAMP.get())
                          .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.SEAL_STAMP_IMPRESSION, SealImpression.getOrThrow(registries, SealImpression.CREEPER))))
                    .add(LootItem.lootTableItem(Envelope.Items.SEAL_STAMP.get())
                          .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.SEAL_STAMP_IMPRESSION, SealImpression.getOrThrow(registries, SealImpression.LETTER))))
                    .add(LootItem.lootTableItem(Envelope.Items.SEAL_STAMP.get())
                          .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.SEAL_STAMP_IMPRESSION, SealImpression.getOrThrow(registries, SealImpression.SWORDS))))
                    .add(LootItem.lootTableItem(Envelope.Items.SEAL_STAMP.get())
                          .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.SEAL_STAMP_IMPRESSION, SealImpression.getOrThrow(registries, SealImpression.VILLAGER))))
              )
        );

        output.accept(
              Envelope.LootTables.COLLAPSED_MAIL_HUB_MANUFACTORY,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(2.0F, 8.0F))
                    .add(LootItem.lootTableItem(Items.DIRT).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.COARSE_DIRT).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Items.PUMPKIN_SEEDS).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.PUMPKIN).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Items.CARVED_PUMPKIN).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                    .add(LootItem.lootTableItem(Items.COPPER_BLOCK).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
                    .add(LootItem.lootTableItem(Items.EXPOSED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.WEATHERED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.OXIDIZED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
              )
        );

        output.accept(
              Envelope.LootTables.COLLAPSED_MAIL_HUB_DISPATCHERY,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(1.0F, 3.0F))
                    .add(LootItem.lootTableItem(Items.WRITABLE_BOOK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                    .add(LootItem.lootTableItem(Items.BOOK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.PAPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.FEATHER).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.BLACK_DYE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Envelope.Items.LETTER_AND_QUILL.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
              )
        );

        output.accept(
              Envelope.LootTables.COLLAPSED_MAIL_HUB_CONSTRUCTION,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(4.0F, 8.0F))
                    .add(LootItem.lootTableItem(Items.SCAFFOLDING).apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F))))
                    .add(LootItem.lootTableItem(Items.ACACIA_PLANKS).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.ACACIA_FENCE).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.RED_WOOL).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.COPPER_INGOT).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 12.0F))))
                    .add(LootItem.lootTableItem(Items.COPPER_BLOCK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.EXPOSED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.WEATHERED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.OXIDIZED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.COPPER_BULB).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.EXPOSED_COPPER_BULB).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.WEATHERED_COPPER_BULB).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.OXIDIZED_COPPER_BULB).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.CHISELED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.EXPOSED_CHISELED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.WEATHERED_CHISELED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.OXIDIZED_CHISELED_COPPER).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.COPPER_TRAPDOOR).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.EXPOSED_COPPER_TRAPDOOR).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.WEATHERED_COPPER_TRAPDOOR).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Items.OXIDIZED_COPPER_TRAPDOOR).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                    .add(LootItem.lootTableItem(Envelope.Items.PAPER_BOX.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F))))
              )
        );

        output.accept(
              Envelope.LootTables.COLLAPSED_MAIL_HUB_DOCK_NOTE,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .add(letter(Component.translatable("letter.envelope.collapsed_mail_hub.dock_note.name.note"), Component.translatable("letter.envelope.collapsed_mail_hub.dock_note_1")))
                    .add(letter(Component.translatable("letter.envelope.collapsed_mail_hub.dock_note.name.note"), Component.translatable("letter.envelope.collapsed_mail_hub.dock_note_2")))
                    .add(letter(Component.translatable("letter.envelope.collapsed_mail_hub.dock_note.name.status"), Component.translatable("letter.envelope.collapsed_mail_hub.dock_note_3")))
                    .add(letter(Component.translatable("letter.envelope.collapsed_mail_hub.dock_note.name.status"), Component.translatable("letter.envelope.collapsed_mail_hub.dock_note_4")))
                    .add(letter(Component.translatable("letter.envelope.collapsed_mail_hub.dock_note.name.status"), Component.translatable("letter.envelope.collapsed_mail_hub.dock_note_5")))
                    .add(letter(Component.translatable("letter.envelope.collapsed_mail_hub.dock_note.name.status"), Component.translatable("letter.envelope.collapsed_mail_hub.dock_note_6")))
                    .add(letter(Component.translatable("letter.envelope.collapsed_mail_hub.dock_note.name.status"), Component.translatable("letter.envelope.collapsed_mail_hub.dock_note_7")))
                    .add(letter(Component.translatable("letter.envelope.collapsed_mail_hub.dock_note.name.status"), Component.translatable("letter.envelope.collapsed_mail_hub.dock_note_8")))
              )
        );
    }

    private LootPoolSingletonContainer.Builder<?> letter(@Nullable Component name, @NotNull Component text) {
        LootPoolSingletonContainer.Builder<?> builder = LootItem.lootTableItem(Envelope.Items.LETTER.get())
              .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.LETTER_CONTENT, new LetterContent(text)));

        if (name != null) {
            builder.apply(SetComponentsFunction.setComponent(DataComponents.ITEM_NAME, name));
        }

        return builder;
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
}