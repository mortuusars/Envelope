package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.crafting.*;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailLetterBroadcastingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipeBuilder;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.entity.MailEntities;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RecipesDatagen extends RecipeProvider {
    private @Nullable HolderLookup.Provider registries;

    public RecipesDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected @NotNull CompletableFuture<?> run(@NotNull CachedOutput output, HolderLookup.@NotNull Provider registries) {
        this.registries = registries;
        return super.run(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        buildCraftingRecipes(output);
        buildMailRecipes(output, Objects.requireNonNull(registries, "Registries are not available."));
    }

    private void buildCraftingRecipes(@NotNull RecipeOutput output) {
        pigeonhole(output, Envelope.Items.OAK_PIGEONHOLE.get(), Items.OAK_PLANKS);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, Envelope.Items.MAILBOX.get(), 1)
              .define('P', ItemTags.PLANKS)
              .define('S', ItemTags.WOODEN_SLABS)
              .define('T', Envelope.Items.ADDRESS_TAG.get())
              .pattern("SSS")
              .pattern("PTP")
              .pattern("PPP")
              .unlockedBy("has_planks", has(ItemTags.PLANKS))
              .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.LETTER_AND_QUILL.get())
              .requires(Items.PAPER)
              .requires(Items.INK_SAC)
              .requires(Items.FEATHER)
              .unlockedBy("has_paper", has(Items.PAPER))
              .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, Envelope.Items.PAPER_BOX.get(), 3)
              .define('P', Items.PAPER)
              .define('H', Items.HONEYCOMB)
              .pattern(" P ")
              .pattern("PHP")
              .pattern(" P ")
              .unlockedBy("has_paper", has(Items.PAPER))
              .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.ADDRESS_TAG.get())
              .requires(Items.PAPER)
              .requires(ItemTags.SIGNS)
              .unlockedBy("has_paper", has(Items.PAPER))
              .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Envelope.Items.SEAL_STAMP.get(), 1)
              .define('S', ItemTags.WOODEN_SLABS)
              .define('I', Tags.Items.INGOTS_IRON)
              .pattern(" S ")
              .pattern(" S ")
              .pattern(" I ")
              .unlockedBy("has_mailable", has(Envelope.Tags.Items.MAILABLE))
              .save(output);

        SpecialRecipeBuilder.special(LetterCloningRecipe::new).save(output, Envelope.resource("letter_cloning"));
        SpecialRecipeBuilder.special(AddressTagApplicationRecipe::new).save(output, Envelope.resource("address_tag_application"));
        SpecialRecipeBuilder.special(PaybackTagApplicationRecipe::new).save(output, Envelope.resource("payback_tag_application"));
    }

    protected void pigeonhole(RecipeOutput output, ItemLike result, ItemLike planks) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, result)
              .define('P', planks)
              .define('H', Items.HAY_BLOCK)
              .pattern("PPP")
              .pattern("PHP")
              .pattern("PPP")
              .group("pigeonhole")
              .unlockedBy("has_hay", has(Items.HAY_BLOCK))
              .save(output);
    }

    private void buildMailRecipes(@NotNull RecipeOutput output, HolderLookup.Provider registries) {
        EntityAddress mailService = address(MailEntities.MAIL_SERVICE);

        MailRecipeBuilder.crafting(mailService)
              .requires(Ingredient.of(Envelope.Items.ADDRESS_TAG.get()))
              .requires(Ingredient.of(Tags.Items.DYES_RED))
              .forResult(Envelope.Items.PAYBACK_TAG.get())
              .save(output);

        MailRecipeBuilder.crafting(mailService)
              .requires(Ingredient.of(Items.DIAMOND))
              .forResult(Mail.createPackage(Envelope.LootTables.LOST_MAIL)
                    .set(DataComponents.ITEM_NAME, Component.translatable("item.envelope.lost_mail"))
                    .get())
              .experience(1.5f)
              .save(output, "lost_mail");

        output.accept(
              Envelope.resource(MailRecipeBuilder.getDefaultPath(mailService, "letter_broadcasting")),
              new MailLetterBroadcastingRecipe(
                    mailService,
                    NonNullList.of(Ingredient.EMPTY, Ingredient.of(Envelope.Tags.Items.LETTERS), Ingredient.of(Items.DIAMOND)),
                    Mail.of(new ItemStack(Envelope.Items.LETTER.get()))
                          .set(DataComponents.ITEM_NAME, Component.translatable("letter.envelope.broadcast_report.name"))
                          .get(),
                    0f
              ),
              null);

        sealStamp(output, mailService, Ingredient.of(Items.GOLDEN_APPLE), SealImpression.APPLE);
        sealStamp(output, mailService, Ingredient.of(ItemTags.SWORDS), SealImpression.SWORD);
        sealStamp(output, mailService, Ingredient.of(ItemTags.PICKAXES), SealImpression.PICKAXE);
        sealStamp(output, mailService, Ingredient.of(ItemTags.SHOVELS), SealImpression.SHOVEL);
        sealStamp(output, mailService, Ingredient.of(ItemTags.AXES), SealImpression.AXE);
        sealStamp(output, mailService, Ingredient.of(ItemTags.HOES), SealImpression.HOE);
        sealStamp(output, mailService, Ingredient.of(Items.BOOK), SealImpression.BOOK);
        sealStamp(output, mailService, Ingredient.of(Items.SKELETON_SKULL), SealImpression.SKELETON);

        // --

        EntityAddress automatedSupplyService = address(MailEntities.AUTOMATED_SUPPLY_SERVICE);

        MailRecipeBuilder.crafting(automatedSupplyService)
              .requires(Ingredient.of(Items.ROTTEN_FLESH), 6)
              .forResult(Items.LEATHER, 1)
              .save(output);

        MailRecipeBuilder.crafting(automatedSupplyService)
              .requires(Ingredient.of(Tags.Items.INGOTS_IRON))
              .requires(Ingredient.of(Items.LEATHER), 5)
              .forResult(Items.SADDLE)
              .save(output);

        MailRecipeBuilder.crafting(automatedSupplyService)
              .requires(Ingredient.of(Items.DIORITE), 1)
              .requires(Ingredient.of(Items.BONE_MEAL), 5)
              .forResult(Items.CALCITE)
              .save(output);

        MailRecipeBuilder.crafting(automatedSupplyService)
              .requires(Ingredient.of(Items.IRON_NUGGET))
              .requires(Ingredient.of(Items.PHANTOM_MEMBRANE))
              .forResult(Items.NAME_TAG)
              .save(output);

        MailRecipeBuilder.crafting(automatedSupplyService)
              .requires(Ingredient.of(Items.INK_SAC))
              .requires(Ingredient.of(Items.GLOWSTONE_DUST), 5)
              .forResult(Items.GLOW_INK_SAC)
              .save(output);

        MailRecipeBuilder.crafting(automatedSupplyService)
              .requires(Ingredient.of(Items.PAPER))
              .requires(Ingredient.of(Items.BLACK_DYE))
              .requires(Ingredient.of(Items.SLIME_BALL))
              .requires(Ingredient.of(Items.FEATHER))
              .forResult(Envelope.Items.LETTER_AND_QUILL.get())
              .save(output);

        MailRecipeBuilder.crafting(automatedSupplyService)
              .requires(Ingredient.of(Items.BOOK))
              .requires(Ingredient.of(Items.BLACK_DYE))
              .requires(Ingredient.of(Items.SLIME_BALL))
              .requires(Ingredient.of(Items.FEATHER))
              .forResult(Items.WRITABLE_BOOK)
              .save(output);
    }

    protected void sealStamp(RecipeOutput output, EntityAddress address, Ingredient ingredient, ResourceKey<SealImpression> impression) {
        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Envelope.Items.SEAL_STAMP.get()))
              .requires(ingredient)
              .forResult(Util.make(() -> {
                  ItemStack stamp = new ItemStack(Envelope.Items.SEAL_STAMP.get());
                  HolderLookup.RegistryLookup<SealImpression> lookup = Objects.requireNonNull(registries)
                        .lookupOrThrow(Envelope.Registries.SEAL_IMPRESSION);
                  stamp.set(Envelope.DataComponents.SEAL_STAMP_IMPRESSION, lookup.getOrThrow(impression));
                  return stamp;
              }))
              .experience(1.5f)
              .save(output, "seal_stamp_" + impression.location().getPath());
    }

    protected EntityAddress address(ResourceKey<MailEntity> key) {
        return new EntityAddress(Objects.requireNonNull(registries).lookupOrThrow(Envelope.Registries.MAIL_ENTITY)
              .get(key)
              .orElseThrow());
    }
}
