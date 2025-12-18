package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class RecipesDatagen extends RecipeProvider {
    public RecipesDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        pigeonhole(output, Envelope.Items.OAK_PIGEONHOLE.get(), Items.OAK_PLANKS);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.LETTER_AND_QUILL.get())
              .requires(Items.PAPER)
              .requires(Items.FEATHER)
              .requires(Items.INK_SAC)
              .unlockedBy("has_paper", has(Items.PAPER))
              .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.PAPER_BOX.get())
              .requires(Items.PAPER)
              .requires(Items.PAPER)
              .requires(Items.PAPER)
              .requires(Items.PAPER)
              .unlockedBy("has_paper", has(Items.PAPER))
              .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.PACKING_BOX.get())
              .requires(Envelope.Items.PAPER_BOX.get())
              .requires(Items.HONEYCOMB)
              .unlockedBy("has_paper_box", has(Envelope.Items.PAPER_BOX.get()))
              .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.ADDRESS_TAG.get())
              .requires(Items.PAPER)
              .requires(ItemTags.SIGNS)
              .unlockedBy("has_paper", has(Items.PAPER))
              .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.PAYBACK_TAG.get())
              .requires(Items.PAPER)
              .requires(ItemTags.SIGNS)
              .requires(Tags.Items.DYES_RED)
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
}
