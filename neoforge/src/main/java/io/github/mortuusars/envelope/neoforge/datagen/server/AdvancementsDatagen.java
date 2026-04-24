package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AdvancementsDatagen extends AdvancementProvider {
    public AdvancementsDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper, List<AdvancementGenerator> subProviders) {
        super(output, registries, existingFileHelper, subProviders);
    }

    public static class Generator implements AdvancementProvider.AdvancementGenerator {
        @SuppressWarnings("removal")
        @Override
        public void generate(HolderLookup.@NotNull Provider registries,
                             @NotNull Consumer<AdvancementHolder> consumer,
                             @NotNull ExistingFileHelper existingFileHelper) {
            Advancement.Builder.advancement()
                  .parent(ResourceLocation.parse("minecraft:husbandry/root")) //TODO: change parent to pigeonhole-related advancement
                  .display(
                        new ItemStack(Items.DIRT),
                        Component.translatable("advancement.envelope.dirt_to_diamonds.title"),
                        Component.translatable("advancement.envelope.dirt_to_diamonds.description"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        true
                  )
                  .requirements(AdvancementRequirements.Strategy.OR)
                  .addCriterion("scoop_diamond", Envelope.CriteriaTriggers.SCOOP_DIAMOND.get()
                        .createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty())))
                  .save(consumer, Envelope.resource("dirt_to_diamonds"), existingFileHelper);
        }
    }
}
