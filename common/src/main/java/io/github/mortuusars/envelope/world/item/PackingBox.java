package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.world.item.ItemStack;

public interface PackingBox {
    default int getTimesPacked(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.PACKAGE_TIMES_PACKED, 0);
    }

    default int getRemainingPacks(ItemStack stack) {
        return Config.Server.PACKAGE_PACK_LIMIT.get() - getTimesPacked(stack);
    }

    default boolean canPack(ItemStack stack) {
        return getTimesPacked(stack) < Config.Server.PACKAGE_PACK_LIMIT.get();
    }

    default boolean shouldBeDestroyedWhenEmpty(ItemStack stack) {
        return !canPack(stack);
    }

    default boolean canInsert(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems() && !stack.is(Envelope.Tags.Items.CANNOT_BE_PACKAGED);
    }
}
