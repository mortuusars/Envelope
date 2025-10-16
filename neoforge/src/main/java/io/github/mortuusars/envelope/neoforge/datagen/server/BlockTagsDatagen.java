package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BlockTagsDatagen extends BlockTagsProvider {
    public BlockTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Envelope.ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(Envelope.Tags.Blocks.PIGEON_SPAWNABLE_ON)
                .add(Blocks.GRASS_BLOCK)
                .add(Blocks.AIR)
                .addTag(BlockTags.LEAVES)
                .addTag(BlockTags.LOGS)
                .addTag(BlockTags.BASE_STONE_OVERWORLD);

        tag(Envelope.Tags.Blocks.PIGEONHOLES)
                .add(Envelope.Blocks.PIGEONHOLES.values().stream().map(Supplier::get).toArray(Block[]::new));

        tag(BlockTags.MINEABLE_WITH_AXE)
              .addTag(Envelope.Tags.Blocks.PIGEONHOLES);
    }
}
