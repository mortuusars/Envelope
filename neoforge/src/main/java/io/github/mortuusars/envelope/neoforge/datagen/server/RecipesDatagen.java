package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class RecipesDatagen extends RecipeProvider {
    public RecipesDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        pigeonhole(output, Envelope.Items.OAK_PIGEONHOLE.get(), Items.OAK_PLANKS);
        pigeonhole(output, Envelope.Items.SPRUCE_PIGEONHOLE.get(), Items.SPRUCE_PLANKS);
        pigeonhole(output, Envelope.Items.BIRCH_PIGEONHOLE.get(), Items.BIRCH_PLANKS);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.LETTER.get())
                .requires(Items.PAPER)
                .requires(Items.FEATHER)
                .requires(Items.INK_SAC)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.CARDBOARD_BOX.get())
                .requires(Items.PAPER)
                .requires(Items.PAPER)
                .requires(Items.PAPER)
                .requires(Items.PAPER)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Envelope.Items.PACKAGE.get())
                .requires(Envelope.Items.CARDBOARD_BOX.get())
                .requires(Items.HONEYCOMB)
                .unlockedBy("has_cardboard_box", has(Envelope.Items.CARDBOARD_BOX.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Envelope.Items.ADDRESS_TAG.get(), 2)
                .define('P', Items.PAPER)
                .define('S', ItemTags.SIGNS)
                .pattern("   ")
                .pattern("PSP")
                .pattern("   ")
                .unlockedBy("has_pigeonhole", has(Envelope.Tags.Items.PIGEONHOLES))
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
