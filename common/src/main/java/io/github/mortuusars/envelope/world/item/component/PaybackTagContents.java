package io.github.mortuusars.envelope.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PaybackTagContents implements TooltipComponent {
    public static final int SLOTS = Payback.SLOTS;

    public static final Codec<PaybackTagContents> CODEC = ItemStack.OPTIONAL_CODEC.listOf(0, SLOTS)
          .xmap(PaybackTagContents::new, PaybackTagContents::getItemsForReading);
    public static final StreamCodec<RegistryFriendlyByteBuf, PaybackTagContents> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
          .apply(ByteBufCodecs.list(SLOTS))
          .map(PaybackTagContents::new, PaybackTagContents::getItemsForReading);

    public static final PaybackTagContents EMPTY = new PaybackTagContents(Collections.emptyList());

    private final List<ItemStack> items;

    public PaybackTagContents(List<ItemStack> items) {
        this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(items.size(), SLOTS); i++) {
            this.items.set(i, items.get(i));
        }
    }

    public static PaybackTagContents of(Container container) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < Math.min(container.getContainerSize(), SLOTS); i++) {
            items.add(container.getItem(i));
        }
        return new PaybackTagContents(items);
    }

    public static PaybackTagContents of(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, EMPTY);
    }

    // --

    public int size() {
        return items.size();
    }

    public List<ItemStack> getItemsForReading() {
        return items;
    }

    public ItemStack getItemForReading(int index) {
        return index < size() ? items.get(index) : ItemStack.EMPTY;
    }

    public List<ItemStack> copyItems() {
        return Lists.transform(this.items, ItemStack::copy);
    }

    public Payback toPayback() {
        //TODO: collapse stacks?
        return new Payback(getItemsForReading().stream()
              .limit(SLOTS)
              .map(item -> new RequestedItem(item.getItem(), item.getCount(), DataComponentPredicate.EMPTY))
              .toList());
    }

    // --

    @SuppressWarnings("deprecation")
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        return object instanceof PaybackTagContents packageContents && ItemStack.listMatches(this.items, packageContents.items);
    }

    @SuppressWarnings("deprecation")
    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }

    public String toString() {
        return "PaybackTagContents" + this.items;
    }

    public boolean isEmpty() {
        return this.equals(EMPTY) || getItemsForReading().isEmpty() || getItemsForReading().stream().allMatch(ItemStack::isEmpty);
    }
}
