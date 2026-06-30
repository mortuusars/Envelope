package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.advancements.critereon.BreakPaperBoxWhenFallingTrigger;
import io.github.mortuusars.envelope.advancements.critereon.MailDeliveredTrigger;
import io.github.mortuusars.envelope.advancements.predicate.DeliveryPredicate;
import io.github.mortuusars.envelope.advancements.predicate.ItemOccludingBlockPredicate;
import io.github.mortuusars.envelope.advancements.predicate.ItemPackagePredicate;
import net.minecraft.Util;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
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
            AdvancementHolder placePigeonhole = Advancement.Builder.advancement()
                  .parent(ResourceLocation.parse("minecraft:husbandry/root"))
                  .display(
                        new ItemStack(Envelope.Items.OAK_PIGEONHOLE.get()),
                        Component.translatable("advancement.envelope.just_dont_smoke_it.title"),
                        Component.translatable("advancement.envelope.just_dont_smoke_it.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                  )
                  .requirements(AdvancementRequirements.Strategy.OR)
                  .addCriterion("place_pigeonhole", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(
                        LocationCheck.checkLocation(LocationPredicate.Builder.location()
                              .setBlock(BlockPredicate.Builder.block().of(Envelope.Tags.Blocks.PIGEONHOLES)))))
                  .save(consumer, Envelope.resource("husbandry/just_dont_smoke_it"), existingFileHelper);

            Advancement.Builder.advancement()
                  .parent(placePigeonhole)
                  .display(
                        new ItemStack(Items.CAMPFIRE),
                        Component.translatable("advancement.envelope.and_you_did_it_anyway.title"),
                        Component.translatable("advancement.envelope.and_you_did_it_anyway.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        true
                  )
                  .requirements(AdvancementRequirements.Strategy.OR)
                  .addCriterion("smoke_pigeonhole", Envelope.CriteriaTriggers.SMOKE_PIGEONHOLE.get()
                        .createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty())))
                  .save(consumer, Envelope.resource("husbandry/and_you_did_it_anyway"), existingFileHelper);

            Advancement.Builder.advancement()
                  .parent(placePigeonhole)
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
                  .save(consumer, Envelope.resource("husbandry/dirt_to_diamonds"), existingFileHelper);

            Advancement.Builder.advancement()
                  .parent(placePigeonhole)
                  .display(
                        new ItemStack(Items.TNT),
                        Component.translatable("advancement.envelope.its_filthy_in_there.title"),
                        Component.translatable("advancement.envelope.its_filthy_in_there.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        true
                  )
                  .requirements(AdvancementRequirements.Strategy.OR)
                  .addCriterion("spawn_archimedes", Envelope.CriteriaTriggers.SPAWN_ARCHIMEDES.get()
                        .createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty())))
                  .save(consumer, Envelope.resource("husbandry/its_filthy_in_there"), existingFileHelper);

            // --

            AdvancementHolder placeMailbox = Advancement.Builder.advancement()
                  .parent(ResourceLocation.parse("minecraft:adventure/root"))
                  .display(
                        new ItemStack(Envelope.Items.MAILBOX.get()),
                        Component.translatable("advancement.envelope.overworld_wide_web.title"),
                        Component.translatable("advancement.envelope.overworld_wide_web.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                  )
                  .requirements(AdvancementRequirements.Strategy.OR)
                  .addCriterion("place_mailbox", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(
                        LocationCheck.checkLocation(LocationPredicate.Builder.location()
                              .setBlock(BlockPredicate.Builder.block().of(Envelope.Blocks.MAILBOX.get())))))
                  .save(consumer, Envelope.resource("adventure/overworld_wide_web"), existingFileHelper);

            ItemPackagePredicate fullPackagePredicate = new ItemPackagePredicate(Optional.of(new CollectionPredicate<>(
                  Optional.of(new CollectionContentsPredicate.Multiple<>(NonNullList.withSize(6, ItemPredicate.Builder.item()
                        .withCount(MinMaxBounds.Ints.atLeast(64))
                        .withSubPredicate(Envelope.ItemSubPredicates.OCCLUDING_BLOCK.get(), ItemOccludingBlockPredicate.INSTANCE).build()))),
                  Optional.empty(), Optional.empty()
            )));

            Advancement.Builder.advancement()
                  .parent(placeMailbox)
                  .display(
                        new ItemStack(Envelope.Items.SEALED_PACKAGE.get()),
                        Component.translatable("advancement.envelope.what_is_my_purpose.title"),
                        Component.translatable("advancement.envelope.what_is_my_purpose.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        true
                  )
                  .requirements(AdvancementRequirements.Strategy.OR)
                  .addCriterion("deliver_package_full_of_blocks", Envelope.CriteriaTriggers.MAIL_DELIVERED.get()
                        .createCriterion(new MailDeliveredTrigger.TriggerInstance(Optional.empty(), Optional.of(new DeliveryPredicate(
                              Optional.of(ItemPredicate.Builder.item()
                                    .of(Envelope.Tags.Items.PACKAGES)
                                    .withSubPredicate(Envelope.ItemSubPredicates.PACKAGE_CONTENTS.get(), fullPackagePredicate)
                                    .build()), Optional.empty())))))
                  .save(consumer, Envelope.resource("adventure/what_is_my_purpose"), existingFileHelper);

            Advancement.Builder.advancement()
                  .parent(placeMailbox)
                  .display(
                        Util.make(new ItemStack(Envelope.Items.LETTER.get()), stack ->
                              stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE)),
                        Component.translatable("advancement.envelope.foxtile_environment.title"),
                        Component.translatable("advancement.envelope.foxtile_environment.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                  )
                  .requirements(AdvancementRequirements.Strategy.OR)
                  .addCriterion("get_tattered_letter", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item()
                        .of(Envelope.Items.LETTER.get(), Envelope.Items.SEALED_LETTER.get())
                        .hasComponents(DataComponentPredicate.builder().expect(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE).build())))
                  .save(consumer, Envelope.resource("adventure/foxtile_environment"), existingFileHelper);

            // --

            Advancement.Builder.advancement()
                  .parent(ResourceLocation.parse("minecraft:adventure/root"))
                  .display(
                        new ItemStack(Envelope.Items.PAPER_BOX.get()),
                        Component.translatable("advancement.envelope.soft_landing.title"),
                        Component.translatable("advancement.envelope.soft_landing.description"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                  )
                  .requirements(AdvancementRequirements.Strategy.OR)
                  .addCriterion("fall_on_paper_box_from_big_height", Envelope.CriteriaTriggers.BREAK_PAPER_BOX_WHEN_FALLING_TRIGGER.get()
                        .createCriterion(new BreakPaperBoxWhenFallingTrigger.TriggerInstance(
                              Optional.empty(), Optional.of(MinMaxBounds.Doubles.atLeast(100)))))
                  .save(consumer, Envelope.resource("adventure/soft_landing"), existingFileHelper);

            // Advancement for discovering collapsed mail hub is written manually due to Envelope's structures not being available for datagen.
        }
    }
}
