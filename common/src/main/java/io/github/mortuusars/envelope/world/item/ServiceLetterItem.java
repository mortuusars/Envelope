package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.ServiceLetterMeaning;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ServiceLetterItem extends LetterItem {
    public ServiceLetterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (stack.get(Envelope.DataComponents.SERVICE_LETTER_MEANING) instanceof ServiceLetterMeaning meaning) {
            return meaning.translate();
        }
        return super.getName(stack);
    }

    public ItemLike getSealedItem() {
        return Envelope.Items.SEALED_SERVICE_LETTER.get();
    }
}
