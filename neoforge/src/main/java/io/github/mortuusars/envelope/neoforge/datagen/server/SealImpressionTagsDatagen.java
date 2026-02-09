package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class SealImpressionTagsDatagen extends TagsProvider<SealImpression> {
    public SealImpressionTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Envelope.Registries.SEAL_IMPRESSION, completableFuture, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(Envelope.Tags.SealImpressions.SPECIAL)
              .addAll(SealImpression.SYMBOLS.values().stream().toList());

        tag(Envelope.Tags.SealImpressions.TOOLS)
              .add(SealImpression.SWORD)
              .add(SealImpression.PICKAXE)
              .add(SealImpression.AXE)
              .add(SealImpression.SHOVEL)
              .add(SealImpression.HOE);
    }
}