package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface Sealable {
    ItemStack seal(Level level, ItemStack stack, Seal seal);

    default boolean canSeal(Level level, ItemStack stack) {
        return !stack.has(Envelope.DataComponents.SEAL);
    }
}
