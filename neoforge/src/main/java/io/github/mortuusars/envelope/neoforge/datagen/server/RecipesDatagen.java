package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.crafting.MailRecipeBuilder;
import io.github.mortuusars.envelope.world.mail.entity.MailEntities;
import io.github.mortuusars.envelope.world.mail.entity.MailEntity;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
              .requires(new StackIngredient(Envelope.Items.ADDRESS_TAG.get()))
              .requires(new StackIngredient(Tags.Items.DYES_RED))
              .forResult(Envelope.Items.PAYBACK_TAG.get())
              .save(output);

        sealStamp(output, mailService, new StackIngredient(Items.GOLDEN_APPLE), SealImpression.APPLE);
        sealStamp(output, mailService, new StackIngredient(ItemTags.SWORDS), SealImpression.SWORD);
        sealStamp(output, mailService, new StackIngredient(ItemTags.PICKAXES), SealImpression.PICKAXE);
        sealStamp(output, mailService, new StackIngredient(ItemTags.SHOVELS), SealImpression.SHOVEL);
        sealStamp(output, mailService, new StackIngredient(ItemTags.AXES), SealImpression.AXE);
        sealStamp(output, mailService, new StackIngredient(ItemTags.HOES), SealImpression.HOE);
        sealStamp(output, mailService, new StackIngredient(Items.BOOK), SealImpression.BOOK);
        sealStamp(output, mailService, new StackIngredient(Items.SKELETON_SKULL), SealImpression.SKELETON);

        // --

        EntityAddress tradeOffice = address(MailEntities.AUTOMATED_SUPPLY_SERVICE);

        MailRecipeBuilder.crafting(tradeOffice)
              .requires(new StackIngredient(Items.ROTTEN_FLESH), 6)
              .forResult(Items.LEATHER, 1)
              .save(output);

        MailRecipeBuilder.crafting(tradeOffice)
              .requires(new StackIngredient(Items.LEATHER), 3)
              .requires(new StackIngredient(Tags.Items.INGOTS_IRON))
              .forResult(Items.SADDLE)
              .save(output);
    }

    protected void sealStamp(RecipeOutput output, EntityAddress address, StackIngredient ingredient, ResourceKey<SealImpression> impression) {
        MailRecipeBuilder.crafting(address)
              .requires(new StackIngredient(Envelope.Items.SEAL_STAMP.get()))
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
