package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
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
                .addTag(Tags.Items.SEEDS);

        tag(Envelope.Tags.Items.WASTE_SCOOPABLE)
                .addTag(ItemTags.SHOVELS);

        tag(Envelope.Tags.Items.CANNOT_BE_PACKAGED)
                .add(Envelope.Items.PACKAGE.get())
                .add(Items.BUNDLE);

        tag(Envelope.Tags.Items.LETTERS)
                .add(Envelope.Items.LETTER.get())
                .add(Envelope.Items.SEALED_LETTER.get())
                .add(Envelope.Items.OPENED_SEALED_LETTER.get())
                .add(Envelope.Items.TATTERED_LETTER.get());
        tag(Envelope.Tags.Items.PACKAGES)
                .add(Envelope.Items.PACKAGE.get());

        tag(Envelope.Tags.Items.MAILABLE)
              .add(Envelope.Items.LETTER.get())
              .add(Envelope.Items.SEALED_LETTER.get())
              .add(Envelope.Items.OPENED_SEALED_LETTER.get())
              .add(Envelope.Items.TATTERED_LETTER.get())
              .add(Envelope.Items.PACKAGE.get());

        tag(Envelope.Tags.Items.CUTTERS)
              .addTag(Tags.Items.TOOLS_SHEAR);
    }
}
