package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.ServiceLetterMeaning;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SealedServiceLetterItem extends SealedLetterItem {
    public SealedServiceLetterItem(Supplier<? extends Item> baseItem, Properties properties) {
        super(baseItem, properties);
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        if (stack.get(Envelope.DataComponents.SERVICE_LETTER_MEANING) instanceof ServiceLetterMeaning meaning) {
            return Component.translatable("item.envelope.sealed_service_letter.name", meaning.translate());
        }
        return super.getName(stack);
    }
}
