package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.SealStampItem;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SealStampDyeingRecipe extends CustomRecipe {
    public SealStampDyeingRecipe(CraftingBookCategory category) {
        super(category);
    }

    public boolean matches(CraftingInput input, Level level) {
        @Nullable SealStampItem stamp = null;
        ItemStack stampStack = ItemStack.EMPTY;
        @Nullable DyeItem dye = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof SealStampItem sealStampItem) {
                if (stamp != null) {
                    return false;
                }

                stamp = sealStampItem;
                stampStack = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem) {
                if (dye != null || !SealMaterial.WAX_COLORS.containsKey(dyeItem.getDyeColor())) {
                    return false;
                }

                dye = dyeItem;
            }
        }

        return stamp != null && dye != null
              && stamp.canDyeWith(stampStack, dye.getDyeColor());
    }

    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        @Nullable SealStampItem stamp = null;
        ItemStack stampStack = ItemStack.EMPTY;
        @Nullable DyeItem dye = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof SealStampItem sealStampItem) {
                if (stamp != null) {
                    return ItemStack.EMPTY;
                }

                stamp = sealStampItem;
                stampStack = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem) {
                if (dye != null || !SealMaterial.WAX_COLORS.containsKey(dyeItem.getDyeColor())) {
                    return ItemStack.EMPTY;
                }

                dye = dyeItem;
            }
        }

        if (stamp == null || dye == null || !stamp.canDyeWith(stampStack, dye.getDyeColor())) {
            return ItemStack.EMPTY;
        }

        ItemStack finalStampStack = stampStack;
        @Nullable DyeItem finalDye = dye;
        return SealMaterial.get(registries, SealMaterial.fromDyeColor(dye.getDyeColor()))
              .map(material -> {
                  ItemStack result = finalStampStack.transmuteCopy(Envelope.Items.DYED_SEAL_STAMPS.get(finalDye.getDyeColor()).get());
                  result.set(Envelope.DataComponents.SEAL_STAMP_MATERIAL, new EitherHolder<>(material));
                  return result;
              })
              .orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Envelope.RecipeSerializers.SEAL_STAMP_DYEING.get();
    }
}
