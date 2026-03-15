package io.github.mortuusars.envelope.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Function;

public interface EnvelopeCodecs {
    Codec<Integer> HEX_COLOR = Codec.STRING.comapFlatMap(
          hex -> {
              try {
                  int color = (int) Long.parseLong(hex.replace("#", ""), 16);
                  return DataResult.success(color);
              } catch (NumberFormatException e) {
                  return DataResult.error(() -> "Invalid color value: " + hex);
              }
          },
          color -> "#" + String.format("%08X", color));

    Codec<Float> NON_NEGATIVE_FLOAT = Codec.FLOAT
          .validate(value -> value.compareTo(0.0f) >= 0.0f && value.compareTo(1.0f) <= 0
                ? DataResult.success(value)
                : DataResult.error(() -> "Value must be within 0.0 to 1.0 range."));

    static Codec<NonNullList<Ingredient>> recipeIngredients(int size, RecipeType<?> type) {
        return Ingredient.CODEC_NONEMPTY
              .listOf()
              .flatXmap(
                    list -> {
                        Ingredient[] ingredients = list.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
                        if (ingredients.length == 0) {
                            return DataResult.error(() -> "No ingredients for " + type.toString() + " recipe");
                        } else {
                            return ingredients.length > size
                                  ? DataResult.error(() -> "Too many ingredients for " + type.toString() + " recipe")
                                  : DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
                        }
                    },
                    DataResult::success
              );
    }
}
