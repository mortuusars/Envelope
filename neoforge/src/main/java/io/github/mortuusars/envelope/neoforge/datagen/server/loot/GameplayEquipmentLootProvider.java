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
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Arrays;
import java.util.function.BiConsumer;

public record GameplayEquipmentLootProvider(HolderLookup.Provider registries) implements LootTableSubProvider {
    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(
              Envelope.LootTables.CHARRED_PIGEON_MAIL,
              LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(recipePackage(ServiceAddresses.MAIL_SERVICE,
                          new ItemStack(Envelope.Items.ADDRESS_TAG.get(), 3),
                          new ItemStack(Items.RED_DYE, 3)))
                    .add(recipePackage(ServiceAddresses.AUTOMATED_SUPPLY_SERVICE,
                          new ItemStack(Items.ANDESITE, 4),
                          new ItemStack(Items.FLINT, 4),
                          new ItemStack(Items.FLINT, 4),
                          new ItemStack(Items.FLINT, 4),
                          new ItemStack(Items.FLINT, 4),
                          new ItemStack(Items.FLINT, 4)))
                    .add(recipePackage(ServiceAddresses.AUTOMATED_SUPPLY_SERVICE,
                          new ItemStack(Items.DIORITE, 4),
                          new ItemStack(Items.BONE_MEAL, 4),
                          new ItemStack(Items.BONE_MEAL, 4),
                          new ItemStack(Items.BONE_MEAL, 4),
                          new ItemStack(Items.BONE_MEAL, 4),
                          new ItemStack(Items.BONE_MEAL, 4)))
                    .add(recipePackage(ServiceAddresses.AUTOMATED_SUPPLY_SERVICE,
                          new ItemStack(Items.INK_SAC, 4),
                          new ItemStack(Items.GLOWSTONE_DUST, 4),
                          new ItemStack(Items.GLOWSTONE_DUST, 4),
                          new ItemStack(Items.GLOWSTONE_DUST, 4),
                          new ItemStack(Items.GLOWSTONE_DUST, 4),
                          new ItemStack(Items.GLOWSTONE_DUST, 4)))
                    .add(recipePackage(ServiceAddresses.EQUINE_ASSURANCE_BUREAU,
                          new ItemStack(Items.GOLD_BLOCK, 1)))
                    .add(LootItem.lootTableItem(Envelope.Items.PACKAGE.get())
                          .apply(SetNameFunction.setName(Component.translatable("item.envelope.lost_mail"), SetNameFunction.Target.ITEM_NAME))
                          .apply(SetComponentsFunction.setComponent(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(Envelope.LootTables.NETHER_LOST_MAIL, 0L)))))
        );
    }

    private LootPoolSingletonContainer.Builder<?> recipePackage(ResourceKey<ServiceAddressDefinition> address, ItemStack... ingredients) {
        return LootItem.lootTableItem(Envelope.Items.PACKAGE.get())
              .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.MAIL_RECIPIENT, ServiceAddress.getOrThrow(registries, address)))
              .apply(SetComponentsFunction.setComponent(Envelope.DataComponents.PACKAGE_CONTENTS, new PackageContents(Arrays.stream(ingredients).toList())));
    }
}