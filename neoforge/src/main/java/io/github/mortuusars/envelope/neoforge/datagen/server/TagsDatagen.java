package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class TagsDatagen {
    public static class BlockTagsDatagen extends BlockTagsProvider {
        public BlockTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper helper) {
            super(output, registries, Envelope.ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.@NotNull Provider provider) {
            tag(Envelope.Tags.Blocks.PIGEONS_SPAWNABLE_ON)
                  .add(Blocks.GRASS_BLOCK)
                  .add(Blocks.DIRT)
                  .add(Blocks.PODZOL)
                  .add(Blocks.COARSE_DIRT)
                  .add(Blocks.AIR)
                  .addTag(BlockTags.LEAVES)
                  .addTag(BlockTags.LOGS)
                  .addTag(BlockTags.BASE_STONE_OVERWORLD);

            tag(Envelope.Tags.Blocks.PIGEONS_PERCHABLE_ON)
                  .addTag(BlockTags.LEAVES)
                  .addTag(BlockTags.LOGS)
                  .addTag(BlockTags.WOODEN_DOORS)
                  .addTag(BlockTags.WOODEN_FENCES)
                  .addTag(BlockTags.WOODEN_SLABS)
                  .addTag(BlockTags.WOODEN_STAIRS)
                  .addTag(BlockTags.WOODEN_TRAPDOORS)
                  .addTag(BlockTags.WALL_HANGING_SIGNS)
                  .addTag(BlockTags.PLANKS);

            tag(Envelope.Tags.Blocks.PIGEONHOLES)
                  .add(Envelope.Blocks.PIGEONHOLES.values().stream().map(Supplier::get).toArray(Block[]::new));

            tag(Envelope.Tags.Blocks.PIGEONHOLES_THAT_BURN)
                  .add(
                        Envelope.Blocks.OAK_PIGEONHOLE.get(),
                        Envelope.Blocks.SPRUCE_PIGEONHOLE.get(),
                        Envelope.Blocks.BIRCH_PIGEONHOLE.get(),
                        Envelope.Blocks.JUNGLE_PIGEONHOLE.get(),
                        Envelope.Blocks.ACACIA_PIGEONHOLE.get(),
                        Envelope.Blocks.DARK_OAK_PIGEONHOLE.get(),
                        Envelope.Blocks.MANGROVE_PIGEONHOLE.get(),
                        Envelope.Blocks.CHERRY_PIGEONHOLE.get(),
                        Envelope.Blocks.BAMBOO_PIGEONHOLE.get()
                  );

            tag(Envelope.Tags.Blocks.BURNING)
                  .addTag(BlockTags.FIRE)
                  .addTag(BlockTags.CAMPFIRES);

            tag(BlockTags.MINEABLE_WITH_AXE)
                  .addTag(Envelope.Tags.Blocks.PIGEONHOLES)
                  .add(Envelope.Blocks.MAILBOX.get());

            tag(BlockTags.DOES_NOT_BLOCK_HOPPERS)
                  .addTag(Envelope.Tags.Blocks.PIGEONHOLES);

            tag(Envelope.Tags.Blocks.DOVECOTE_PLAINS_REPLACEABLE)
                  .addTag(Envelope.Tags.Blocks.PIGEONHOLES)
                  .addTag(BlockTags.WOODEN_STAIRS)
                  .addTag(BlockTags.WOODEN_SLABS)
                  .addTag(BlockTags.WOODEN_FENCES)
                  .addTag(BlockTags.WOODEN_TRAPDOORS);

            tag(Envelope.Tags.Blocks.DOVECOTE_SAVANNA_REPLACEABLE)
                  .addTag(BlockTags.WOODEN_STAIRS)
                  .addTag(BlockTags.WOODEN_FENCES);

            tag(Envelope.Tags.Blocks.DOVECOTE_TAIGA_REPLACEABLE)
                  .addTag(Envelope.Tags.Blocks.PIGEONHOLES)
                  .addTag(BlockTags.WOODEN_STAIRS)
                  .addTag(BlockTags.WOODEN_SLABS);
        }
    }

    public static class ItemTags extends ItemTagsProvider {
        public ItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                        CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper) {
            super(output, registries, blockTags, Envelope.ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.@NotNull Provider provider) {
            copy(Envelope.Tags.Blocks.PIGEONHOLES, Envelope.Tags.Items.PIGEONHOLES);

            tag(Envelope.Tags.Items.PIGEON_FOOD)
                  .addOptionalTag(Tags.Items.SEEDS);
            tag(Envelope.Tags.Items.BAT_FOOD)
                  .add(Items.SPIDER_EYE);
            tag(Envelope.Tags.Items.COURIER_FOOD)
                  .addOptionalTag(Envelope.Tags.Items.PIGEON_FOOD)
                  .addOptionalTag(Envelope.Tags.Items.BAT_FOOD);

            tag(Envelope.Tags.Items.VILLAGER_FEEDING_PIGEON_FOOD_COMMON)
                  .add(Items.WHEAT_SEEDS);
            tag(Envelope.Tags.Items.VILLAGER_FEEDING_PIGEON_FOOD_UNCOMMON)
                  .add(Items.BEETROOT_SEEDS);
            tag(Envelope.Tags.Items.VILLAGER_FEEDING_PIGEON_FOOD_RARE)
                  .add(Items.MELON_SEEDS)
                  .add(Items.PUMPKIN_SEEDS)
                  .add(Items.TORCHFLOWER_SEEDS)
                  .addOptional(ResourceLocation.parse("farmersdelight:cabbage_seeds"))
                  .addOptional(ResourceLocation.parse("farmersdelight:tomato_seeds"))
                  .addOptional(ResourceLocation.parse("farmersdelight:rice"))
                  .addOptional(ResourceLocation.parse("supplementaries:flax_seeds"));

            tag(Envelope.Tags.Items.WASTE_SCOOPABLE)
                  .addTag(net.minecraft.tags.ItemTags.SHOVELS);

            tag(Envelope.Tags.Items.CANNOT_BE_PACKAGED)
                  .addTag(Envelope.Tags.Items.PACKAGES)
                  .add(Envelope.Items.PAPER_BOX.get())
                  .add(Envelope.Items.PAYBACK_BOX.get())
                  .add(Items.BUNDLE);

            tag(Envelope.Tags.Items.LETTERS)
                  .add(Envelope.Items.LETTER.get())
                  .add(Envelope.Items.SEALED_LETTER.get())
                  .add(Envelope.Items.TATTERED_LETTER.get())
                  .add(Envelope.Items.SEALED_TATTERED_LETTER.get())
                  .add(Envelope.Items.SERVICE_LETTER.get())
                  .add(Envelope.Items.SEALED_SERVICE_LETTER.get());

            tag(Envelope.Tags.Items.PACKAGES)
                  .add(Envelope.Items.PACKAGE.get())
                  .add(Envelope.Items.SEALED_PACKAGE.get());

            tag(Envelope.Tags.Items.REGULAR_SEAL_STAMPS)
                  .add(Envelope.Items.SEAL_STAMP.get())
                  .add(Envelope.Items.DYED_SEAL_STAMPS.values().stream().map(Supplier::get).toArray(Item[]::new));

            tag(Envelope.Tags.Items.SEAL_STAMPS)
                  .addTag(Envelope.Tags.Items.REGULAR_SEAL_STAMPS)
                  .add(Envelope.Items.SOULBOUND_SEAL_STAMP.get());

            tag(Envelope.Tags.Items.MAILABLE)
                  .add(Envelope.Items.LETTER.get())
                  .add(Envelope.Items.SEALED_LETTER.get())
                  .add(Envelope.Items.TATTERED_LETTER.get())
                  .add(Envelope.Items.SEALED_TATTERED_LETTER.get())
                  .add(Envelope.Items.SERVICE_LETTER.get())
                  .add(Envelope.Items.SEALED_SERVICE_LETTER.get())
                  .add(Envelope.Items.PACKAGE.get())
                  .add(Envelope.Items.SEALED_PACKAGE.get())
                  .add(Envelope.Items.PAYBACK_BOX.get())
                  .add(Envelope.Items.PAYBACK_PACKAGE.get())
                  .addOptionalTag(ResourceLocation.parse("create:packages"));
        }
    }

    public static class EntityTypeTagsDatagen extends EntityTypeTagsProvider {
        public EntityTypeTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper helper) {
            super(output, registries, Envelope.ID, helper);
        }

        @Override
        protected void addTags(@NotNull HolderLookup.Provider provider) {
            tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
                  .add(
                        Envelope.EntityTypes.PIGEON.get(),
                        Envelope.EntityTypes.CHARRED_PIGEON.get(),
                        Envelope.EntityTypes.COURIER_BAT.get()
                  );

            tag(Envelope.Tags.EntityTypes.PIGEONHOLE_INHABITORS)
                  .add(Envelope.EntityTypes.PIGEON.get());

            tag(Envelope.Tags.EntityTypes.SPAWNS_ARCHIMEDES)
                  .addTag(EntityTypeTags.ZOMBIES)
                  .addTag(EntityTypeTags.RAIDERS)
                  .add(EntityType.WANDERING_TRADER)
                  .add(EntityType.PIGLIN)
                  .add(EntityType.PIGLIN_BRUTE)
                  .remove(EntityType.DROWNED);
        }
    }

    public static class DamageTypeTags extends DamageTypeTagsProvider {
        public DamageTypeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper helper) {
            super(output, registries, Envelope.ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.@NotNull Provider provider) {
            tag(Envelope.Tags.DamageTypes.BYPASSES_COURIER_DELIVERY_EVASION)
                  .add(DamageTypes.FELL_OUT_OF_WORLD)
                  .add(DamageTypes.GENERIC_KILL);

            tag(Envelope.Tags.DamageTypes.SPAWNS_ARCHIMEDES)
                  .add(DamageTypes.PLAYER_EXPLOSION);
        }
    }

    public static class SealImpressionTagsDatagen extends TagsProvider<SealSymbol> {
        public SealImpressionTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
            super(output, Envelope.Registries.SEAL_SYMBOL, registries, Envelope.ID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.@NotNull Provider provider) {
            tag(Envelope.Tags.SealImpressions.SPECIAL)
                  .addAll(SealSymbol.EMBLEMS.values().stream().toList());

            tag(Envelope.Tags.SealImpressions.TOOLS)
                  .add(SealSymbol.SWORD)
                  .add(SealSymbol.PICKAXE)
                  .add(SealSymbol.AXE)
                  .add(SealSymbol.SHOVEL)
                  .add(SealSymbol.HOE);
        }
    }

    public static class ServiceAddressTagsDatagen extends TagsProvider<ServiceAddressDefinition> {
        public ServiceAddressTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper helper) {
            super(output, Envelope.Registries.SERVICE_ADDRESS_DEFINITION, registries, Envelope.ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.@NotNull Provider provider) {
            tag(Envelope.Tags.ServiceAddresses.HIDDEN)
                  .add(ServiceAddress.EQUINE_ASSURANCE_BUREAU);
        }
    }
}
