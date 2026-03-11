package io.github.mortuusars.envelope.world.item.crafting.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class MailCraftingRecipe implements MailingRecipe {
    private final EntityAddress address;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final float experience;

    public MailCraftingRecipe(EntityAddress address, NonNullList<Ingredient> ingredients, ItemStack result, float experience) {
        this.address = address;
        this.ingredients = ingredients;
        this.result = result;
        this.experience = experience;
    }

    @Override
    public EntityAddress getAddress() {
        return address;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public ItemStack getResult() {
        return result;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    public float getExperience() {
        return experience;
    }

    @Override
    public CraftingResult craft(PackageContents input, Address sender, ServerLevel level) {
        return new CraftingResult(
              consumeInput(input),
              createOutput(input, sender, level),
              calculateExperiencePoints(getExperience()));
    }

    public int calculateExperiencePoints(float experience) {
        int points = Mth.floor(experience);
        float remainder = Mth.frac(experience);
        if (remainder != 0.0F && Math.random() < remainder) {
            points++;
        }
        return points;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Envelope.RecipeSerializers.MAIL_CRAFTING.get();
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
              "address=" + address +
              ", ingredients=" + ingredients +
              ", result=" + result +
              (experience > 0 ? ", experience=" + experience : "") +
              '}';
    }

//    public record Serializer() implements RecipeSerializer<MailCraftingRecipe> {
//        public static final MapCodec<MailCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
//              RegistryFixedCodec.create(Envelope.Registries.MAIL_ENTITY)
//                    .xmap(EntityAddress::new, EntityAddress::getEntityHolder)
//                    .fieldOf("entity")
//                    .forGetter(MailCraftingRecipe::getAddress),
//              Ingredient.CODEC_NONEMPTY
//                    .listOf()
//                    .fieldOf("ingredients")
//                    .flatXmap(
//                          list -> {
//                              Ingredient[] ingredients = list.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
//                              if (ingredients.length == 0) {
//                                  return DataResult.error(() -> "No ingredients for mail crafting recipe");
//                              } else {
//                                  return ingredients.length > PackageContents.SLOTS
//                                        ? DataResult.error(() -> "Too many ingredients for mail crafting recipe")
//                                        : DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
//                              }
//                          },
//                          DataResult::success
//                    )
//                    .forGetter(MailCraftingRecipe::getIngredients),
//              ItemStack.STRICT_CODEC
//                    .fieldOf("result")
//                    .forGetter(MailCraftingRecipe::getResult),
//              Codec.FLOAT
//                    .optionalFieldOf("experience", 0.0f)
//                    .forGetter(MailCraftingRecipe::getExperience)
//        ).apply(i, MailCraftingRecipe::new));
//
//        public static final StreamCodec<RegistryFriendlyByteBuf, MailCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
//              EntityAddress.STREAM_CODEC, MailCraftingRecipe::getAddress,
//              Ingredient.CONTENTS_STREAM_CODEC
//                    .apply(ByteBufCodecs.list(PackageContents.SLOTS))
//                    .map(Serializer::toNonNullList, Function.identity()), MailCraftingRecipe::getIngredients,
//              ItemStack.STREAM_CODEC, MailCraftingRecipe::getResult,
//              ByteBufCodecs.FLOAT, MailCraftingRecipe::getExperience,
//              MailCraftingRecipe::new
//        );
//
//        private static NonNullList<Ingredient> toNonNullList(List<Ingredient> ingredients) {
//            NonNullList<Ingredient> list = NonNullList.createWithCapacity(ingredients.size());
//            list.addAll(ingredients);
//            return list;
//        }
//
//        @Override
//        public @NotNull MapCodec<MailCraftingRecipe> codec() {
//            return CODEC;
//        }
//
//        @Override
//        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MailCraftingRecipe> streamCodec() {
//            return STREAM_CODEC;
//        }
//    }
}
