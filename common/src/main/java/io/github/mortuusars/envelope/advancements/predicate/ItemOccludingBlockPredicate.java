package io.github.mortuusars.envelope.advancements.predicate;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class ItemOccludingBlockPredicate implements ItemSubPredicate {
    public static final ItemOccludingBlockPredicate INSTANCE = new ItemOccludingBlockPredicate();
    public static final Codec<ItemOccludingBlockPredicate> CODEC = Codec.unit(INSTANCE);
    public static final Type<ItemOccludingBlockPredicate> TYPE = new Type<>(ItemOccludingBlockPredicate.CODEC);

    private ItemOccludingBlockPredicate() {}

    @Override
    public boolean matches(ItemStack stack) {
        // This is a crude way to check for at least somewhat heavy blocks. This is probably the best we can do without having access to level and position.
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().canOcclude();
    }
}
