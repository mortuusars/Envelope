package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.LetterAndQuillItem;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.component.Mail;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LetterCloningRecipe extends CustomRecipe {
	public LetterCloningRecipe(CraftingBookCategory category) {
		super(category);
	}

	public boolean matches(CraftingInput input, Level level) {
		int copies = 0;
		ItemStack sourceStack = ItemStack.EMPTY;

		for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
				continue;
            }

            if (stack.getItem() instanceof LetterItem) {
                if (!sourceStack.isEmpty()) {
                    return false;
                }
                sourceStack = stack;
            } else {
                if (!(stack.getItem() instanceof LetterAndQuillItem)) {
                    return false;
                }

                copies++;
            }
        }

		return !sourceStack.isEmpty() && copies > 0;
	}

	public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		int copies = 0;
		ItemStack sourceStack = ItemStack.EMPTY;

		for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
				continue;
            }

			if (stack.getItem() instanceof LetterItem) {
                if (!sourceStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                sourceStack = stack;
            } else {
				if (!(stack.getItem() instanceof LetterAndQuillItem)) {
                    return ItemStack.EMPTY;
                }

                copies++;
            }
        }

		if (!sourceStack.isEmpty() && copies > 0) {
			ItemStack result = sourceStack.copyWithCount(copies);
			result.remove(Envelope.DataComponents.LETTER_TATTERED);
			Mail.removePreviousDeliveryData(result);
			result.remove(Envelope.DataComponents.MAIL_RECIPIENT);
			result.remove(Envelope.DataComponents.MAIL_REQUESTED_PAYBACK);
			return result;
		}

        return ItemStack.EMPTY;
    }

	public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> items = NonNullList.withSize(input.size(), ItemStack.EMPTY);

		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = input.getItem(i);
			@Nullable Item remainingItem = stack.getItem().getCraftingRemainingItem();
			if (remainingItem != null) {
				items.set(i, new ItemStack(remainingItem));
			} else if (stack.getItem() instanceof LetterItem) {
				items.set(i, stack.copyWithCount(1));
				break;
			}
		}

		return items;
	}

	@Override
	public @NotNull RecipeSerializer<?> getSerializer() {
		return Envelope.RecipeSerializers.LETTER_CLONING.get();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 3 && height >= 3;
	}
}
