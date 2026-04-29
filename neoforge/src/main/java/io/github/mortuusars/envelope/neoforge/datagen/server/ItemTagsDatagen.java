package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ItemTagsDatagen extends ItemTagsProvider {
    public ItemTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Envelope.ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        copy(Envelope.Tags.Blocks.PIGEONHOLES, Envelope.Tags.Items.PIGEONHOLES);

        tag(Envelope.Tags.Items.PIGEON_FOOD)
              .addOptionalTag(Tags.Items.SEEDS);

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
              .addTag(ItemTags.SHOVELS);

        tag(Envelope.Tags.Items.CANNOT_BE_PACKAGED)
              .addTag(Envelope.Tags.Items.PACKAGES)
              .add(Envelope.Items.PAPER_BOX.get())
              .add(Envelope.Items.PAYBACK_BOX.get())
              .add(Items.BUNDLE);

        tag(Envelope.Tags.Items.LETTERS)
              .add(Envelope.Items.LETTER.get())
              .add(Envelope.Items.SEALED_LETTER.get());

        tag(Envelope.Tags.Items.PACKAGES)
              .add(Envelope.Items.PACKAGE.get())
              .add(Envelope.Items.SEALED_PACKAGE.get());

        tag(Envelope.Tags.Items.MAILABLE)
              .add(Envelope.Items.LETTER.get())
              .add(Envelope.Items.SEALED_LETTER.get())
              .add(Envelope.Items.PACKAGE.get())
              .add(Envelope.Items.SEALED_PACKAGE.get())
              .add(Envelope.Items.PAYBACK_BOX.get())
              .add(Envelope.Items.PAYBACK_PACKAGE.get())
              .addOptionalTag(ResourceLocation.parse("create:packages"));
    }
}
