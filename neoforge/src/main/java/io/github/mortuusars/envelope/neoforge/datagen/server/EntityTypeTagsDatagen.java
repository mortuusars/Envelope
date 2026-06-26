package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class EntityTypeTagsDatagen extends EntityTypeTagsProvider {
    public EntityTypeTagsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> future, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, future, Envelope.ID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
              .add(
                    Envelope.EntityTypes.PIGEON.get(),
                    Envelope.EntityTypes.CHARRED_PIGEON.get()
              );

        tag(Envelope.Tags.EntityTypes.SPAWNS_ARCHIMEDES)
              .addTag(EntityTypeTags.ZOMBIES)
              .addTag(EntityTypeTags.RAIDERS)
              .add(EntityType.WANDERING_TRADER)
              .add(EntityType.PIGLIN)
              .add(EntityType.PIGLIN_BRUTE)
              .remove(EntityType.DROWNED);
    }
}
