package io.github.mortuusars.envelope.world.item.crafting;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.PaybackTagItem;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class PaybackTagApplicationRecipe extends CustomRecipe {
    public PaybackTagApplicationRecipe(CraftingBookCategory category) {
        super(category);
    }

    public boolean matches(CraftingInput input, Level level) {
        int targets = 0;
        int tags = 0;

        for (int k = 0; k < input.size(); k++) {
            ItemStack stack = input.getItem(k);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(Envelope.Tags.Items.MAILABLE)) {
                targets++;
            } else {
                if (!(stack.getItem() instanceof PaybackTagItem) || !stack.has(Envelope.DataComponents.PAYBACK_TAG_CONTENTS)) {
                    return false;
                }

                tags++;
            }

            if (tags > 1 || targets > 1) {
                return false;
            }
        }

        return targets == 1 && tags == 1;
    }

    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack target = ItemStack.EMPTY;
        ItemStack tag = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(Envelope.Tags.Items.MAILABLE)) {
                target = stack;
            } else if (stack.getItem() instanceof PaybackTagItem) {
                tag = stack;
            }
        }

        if (target.isEmpty() || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return Mail.setPaybackRequest(target.copyWithCount(1), tag.get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Envelope.RecipeSerializers.PAYBACK_TAG_APPLICATION.get();
    }
}
