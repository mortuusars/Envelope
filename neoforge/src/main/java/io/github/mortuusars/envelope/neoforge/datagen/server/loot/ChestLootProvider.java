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

public class ChestLootProvider implements LootTableSubProvider {
    protected final HolderLookup.Provider registries;

    public ChestLootProvider(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public void generate(@NotNull BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
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

    // --

    protected LootPoolSingletonContainer.Builder<?> letter(@Nullable Component name, @NotNull Component text) {
        LootPoolSingletonContainer.Builder<?> builder = LootItem.lootTableItem(Envelope.Items.LETTER.get())
              .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.LETTER_CONTENT, new LetterContent(text)));

        if (name != null) {
            builder.apply(SetComponentsFunction.setComponent(DataComponents.ITEM_NAME, name));
        }

        return builder;
    }
}