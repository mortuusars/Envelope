package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.world.inventory.ContainerUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.List;

public record PaybackTagContents(List<ItemStack> items) implements TooltipComponent {
    public static final int SLOTS = RequestedPayback.SLOTS;

    public static final Codec<PaybackTagContents> CODEC = ItemStack.OPTIONAL_CODEC.listOf(0, SLOTS)
          .xmap(PaybackTagContents::new, PaybackTagContents::getItemsForReading);

    public static final StreamCodec<RegistryFriendlyByteBuf, PaybackTagContents> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
          .apply(ByteBufCodecs.list(SLOTS))
          .map(PaybackTagContents::new, PaybackTagContents::getItemsForReading);

    public static final PaybackTagContents EMPTY = new PaybackTagContents(Collections.emptyList());
    public static final PaybackTagContents DEFAULT = new PaybackTagContents(List.of(new ItemStack(Items.EMERALD)));

    public static PaybackTagContents create(Container container) {
        Container compactedContainer = ContainerUtils.compact(container, SLOTS);
        List<ItemStack> items = ContainerUtils.toList(compactedContainer, SLOTS).stream().filter(i -> !i.isEmpty()).toList();
        return new PaybackTagContents(items);
    }

    // --

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return this.equals(EMPTY) || items.isEmpty() || items.stream().allMatch(ItemStack::isEmpty);
    }

    public List<ItemStack> getItemsForReading() {
        return items;
    }

    public ItemStack getItemForReading(int index) {
        return index < size() ? items.get(index) : ItemStack.EMPTY;
    }

    @SuppressWarnings("deprecation")
    public boolean equals(Object another) {
        return this == another ||
              (another instanceof PaybackTagContents contents
                    && ItemStack.listMatches(this.items, contents.items));
    }

    @SuppressWarnings("deprecation")
    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }
}
