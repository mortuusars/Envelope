package io.github.mortuusars.envelope.neoforge.datagen.server;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import io.github.mortuusars.envelope.world.item.crafting.*;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailLetterBroadcastingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailPaybackRequestCancelingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipeBuilder;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.service.cloud_depository.CloudDepository;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import io.github.mortuusars.envelope.world.mail.address.type.ServiceAddress;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.EitherHolder;
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
        buildMailRecipes(output);
    }

    // --

    private void buildCraftingRecipes(@NotNull RecipeOutput output) {
        pigeonhole(output, Envelope.Items.OAK_PIGEONHOLE.get(), Items.OAK_PLANKS);
        pigeonhole(output, Envelope.Items.SPRUCE_PIGEONHOLE.get(), Items.SPRUCE_PLANKS);
        pigeonhole(output, Envelope.Items.BIRCH_PIGEONHOLE.get(), Items.BIRCH_PLANKS);
        pigeonhole(output, Envelope.Items.JUNGLE_PIGEONHOLE.get(), Items.JUNGLE_PLANKS);
        pigeonhole(output, Envelope.Items.ACACIA_PIGEONHOLE.get(), Items.ACACIA_PLANKS);
        pigeonhole(output, Envelope.Items.DARK_OAK_PIGEONHOLE.get(), Items.DARK_OAK_PLANKS);
        pigeonhole(output, Envelope.Items.MANGROVE_PIGEONHOLE.get(), Items.MANGROVE_PLANKS);
        pigeonhole(output, Envelope.Items.CHERRY_PIGEONHOLE.get(), Items.CHERRY_PLANKS);
        pigeonhole(output, Envelope.Items.BAMBOO_PIGEONHOLE.get(), Items.BAMBOO_PLANKS);
        pigeonhole(output, Envelope.Items.CRIMSON_PIGEONHOLE.get(), Items.CRIMSON_PLANKS);
        pigeonhole(output, Envelope.Items.WARPED_PIGEONHOLE.get(), Items.WARPED_PLANKS);

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
              .define('P', ItemTags.PLANKS)
              .define('I', Tags.Items.INGOTS_IRON)
              .define('H', Items.HONEYCOMB)
              .pattern(" P ")
              .pattern(" I ")
              .pattern(" H ")
              .unlockedBy("has_mailable", has(Envelope.Tags.Items.MAILABLE))
              .save(output);

        SpecialRecipeBuilder.special(LetterCloningRecipe::new).save(output, Envelope.resource("letter_cloning"));
        SpecialRecipeBuilder.special(AddressTagApplicationRecipe::new).save(output, Envelope.resource("address_tag_application"));
        SpecialRecipeBuilder.special(PaybackTagApplicationRecipe::new).save(output, Envelope.resource("payback_tag_application"));
        SpecialRecipeBuilder.special(SealStampDyeingRecipe::new).save(output, Envelope.resource("seal_stamp_dyeing"));

        buildLetterPresettingRecipes(output);
    }

    private void buildLetterPresettingRecipes(@NotNull RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Mail.of(CloudDepository.createWithdrawalRequestLetter()).get())
              .requires(Envelope.Items.LETTER_AND_QUILL.get())
              .requires(Items.COPPER_INGOT)
              .requires(Items.COPPER_INGOT)
              .requires(Items.COPPER_INGOT)
              .group("letter_presets")
              .unlockedBy("has_letter", has(Envelope.Items.LETTER_AND_QUILL.get()))
              .save(output, Envelope.resource("letter_presetting/cloud_depository/withdrawal_request"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Mail.of(CloudDepository.createStatusRequestLetter()).get())
              .requires(Envelope.Items.LETTER_AND_QUILL.get())
              .requires(Items.COPPER_INGOT)
              .group("letter_presets")
              .unlockedBy("has_letter", has(Envelope.Items.LETTER_AND_QUILL.get()))
              .save(output, Envelope.resource("letter_presetting/cloud_depository/status_request"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Mail.of(CloudDepository.createExpansionRequestLetter()).get())
              .requires(Envelope.Items.LETTER_AND_QUILL.get())
              .requires(Items.COPPER_INGOT)
              .requires(Items.DIAMOND)
              .group("letter_presets")
              .unlockedBy("has_letter", has(Envelope.Items.LETTER_AND_QUILL.get()))
              .save(output, Envelope.resource("letter_presetting/cloud_depository/expansion_request"));
    }

    // --

    private void buildMailRecipes(@NotNull RecipeOutput output) {
        mailService(output);
        automatedSupplyService(output);
        equineAssuranceBureau(output);
    }

    private void mailService(@NotNull RecipeOutput output) {
        ServiceAddress address = address(ServiceAddress.MAIL_SERVICE);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Envelope.Items.ADDRESS_TAG.get()))
              .requires(Ingredient.of(Tags.Items.DYES_RED))
              .forResult(Envelope.Items.PAYBACK_TAG.get())
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.DIAMOND))
              .forResult(Mail.createPackage(Envelope.LootTables.LOST_MAIL)
                    .itemName(Component.translatable("item.envelope.lost_mail"))
                    .get())
              .experience(1.5f)
              .save(output, "lost_mail");

        output.accept(
              Envelope.resource(MailRecipeBuilder.getDefaultPath(address, "letter_broadcasting")),
              new MailLetterBroadcastingRecipe(
                    address,
                    NonNullList.of(Ingredient.EMPTY, Ingredient.of(Envelope.Tags.Items.LETTERS), Ingredient.of(Items.DIAMOND))
              ),
              null);

        output.accept(
              Envelope.resource(MailRecipeBuilder.getDefaultPath(address, "payback_request_canceling")),
              new MailPaybackRequestCancelingRecipe(
                    address,
                    NonNullList.of(Ingredient.EMPTY, Ingredient.of(Envelope.Items.PAYBACK_TAG.get()))
              ),
              null);

        sealStamp(output, address, Ingredient.of(Items.GOLDEN_APPLE), SealSymbol.APPLE);
        sealStamp(output, address, Ingredient.of(ItemTags.SWORDS), SealSymbol.SWORD);
        sealStamp(output, address, Ingredient.of(ItemTags.PICKAXES), SealSymbol.PICKAXE);
        sealStamp(output, address, Ingredient.of(ItemTags.SHOVELS), SealSymbol.SHOVEL);
        sealStamp(output, address, Ingredient.of(ItemTags.AXES), SealSymbol.AXE);
        sealStamp(output, address, Ingredient.of(ItemTags.HOES), SealSymbol.HOE);
        sealStamp(output, address, Ingredient.of(Items.GRASS_BLOCK), SealSymbol.CUBE);
        sealStamp(output, address, Ingredient.of(Items.BOOK), SealSymbol.BOOK);
        sealStamp(output, address, Ingredient.of(Envelope.Items.LETTER_AND_QUILL.get()), SealSymbol.LETTER_AND_QUILL);
        sealStamp(output, address, Ingredient.of(Items.SKELETON_SKULL), SealSymbol.SKULL_AND_BONES);
    }

    private void automatedSupplyService(@NotNull RecipeOutput output) {
        ServiceAddress address = address(ServiceAddress.AUTOMATED_SUPPLY_SERVICE);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.ROTTEN_FLESH), 6)
              .forResult(Items.LEATHER, 1)
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Tags.Items.INGOTS_IRON))
              .requires(Ingredient.of(Items.LEATHER), 5)
              .forResult(Items.SADDLE)
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.ANDESITE), 1)
              .requires(Ingredient.of(Items.FLINT), 5)
              .forResult(Items.TUFF, 2)
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.GRANITE), 1)
              .requires(Ingredient.of(Items.CLAY_BALL), 5)
              .forResult(Items.DRIPSTONE_BLOCK, 2)
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.DIORITE), 1)
              .requires(Ingredient.of(Items.BONE_MEAL), 5)
              .forResult(Items.CALCITE, 2)
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.IRON_NUGGET))
              .requires(Ingredient.of(Items.PHANTOM_MEMBRANE))
              .forResult(Items.NAME_TAG)
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.INK_SAC))
              .requires(Ingredient.of(Items.GLOWSTONE_DUST), 5)
              .forResult(Items.GLOW_INK_SAC)
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.PAPER))
              .requires(Ingredient.of(Items.BLACK_DYE))
              .requires(Ingredient.of(Items.FEATHER))
              .forResult(Envelope.Items.LETTER_AND_QUILL.get())
              .save(output);

        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.BOOK))
              .requires(Ingredient.of(Items.BLACK_DYE))
              .requires(Ingredient.of(Items.FEATHER))
              .forResult(Items.WRITABLE_BOOK)
              .save(output);
    }

    private void equineAssuranceBureau(@NotNull RecipeOutput output) {
        ServiceAddress address = address(ServiceAddress.EQUINE_ASSURANCE_BUREAU);
        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Items.GOLD_BLOCK))
              .forResult(Items.GOLDEN_HORSE_ARMOR)
              .save(output);
    }

    // --

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

    protected void sealStamp(RecipeOutput output, ServiceAddress address, Ingredient ingredient, ResourceKey<SealSymbol> impression) {
        MailRecipeBuilder.crafting(address)
              .requires(Ingredient.of(Envelope.Items.SEAL_STAMP.get()))
              .requires(ingredient)
              .forResult(Util.make(() -> {
                  ItemStack stamp = new ItemStack(Envelope.Items.SEAL_STAMP.get());
                  HolderLookup.RegistryLookup<SealSymbol> lookup = Objects.requireNonNull(registries)
                        .lookupOrThrow(Envelope.Registries.SEAL_SYMBOL);
                  stamp.set(Envelope.DataComponents.SEAL_STAMP_DIE, new EitherHolder<>(lookup.getOrThrow(impression)));
                  return stamp;
              }))
              .experience(1.5f)
              .save(output, "seal_stamp_" + impression.location().getPath());
    }

    protected ServiceAddress address(ResourceKey<ServiceAddressDefinition> key) {
        return new ServiceAddress(Objects.requireNonNull(registries).lookupOrThrow(Envelope.Registries.SERVICE_ADDRESS_DEFINITION)
              .get(key)
              .orElseThrow());
    }
}
