package io.github.mortuusars.envelope.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PackageContents implements RecipeInput, TooltipComponent {
    public static final int SLOTS = 6;

    public static final Codec<PackageContents> CODEC = ItemStack.OPTIONAL_CODEC.listOf(0, SLOTS)
            .xmap(PackageContents::new, PackageContents::getItemsTrimmed);
    public static final StreamCodec<RegistryFriendlyByteBuf, PackageContents> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
            .apply(ByteBufCodecs.list(SLOTS))
            .map(PackageContents::new, PackageContents::getItemsTrimmed);

    public static final PackageContents EMPTY = new PackageContents(Collections.emptyList());

    private final List<ItemStack> items;

    public PackageContents(List<ItemStack> items) {
        this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(items.size(), SLOTS); i++) {
            this.items.set(i, items.get(i));
        }
    }

    public PackageContents(Container container) {
        this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(container.getContainerSize(), SLOTS); i++) {
            this.items.set(i, container.getItem(i));
        }
    }

    public static PackageContents of(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.PACKAGE_CONTENTS, EMPTY);
    }

    // --

    public boolean isEmpty() {
        return this == EMPTY || getItems().isEmpty() || getItems().stream().allMatch(ItemStack::isEmpty);
    }

    public int size() {
        return items.size();
    }

    /**
     * Items should not be modified. Use {@link PackageContents#copyItems()} if modification is needed.
     */
    public List<ItemStack> getItems() {
        return items;
    }

    /**
     * Items without trailing empty stacks.
     * <br>
     * Items should not be modified. Use {@link PackageContents#copyItems()} if modification is needed.
     */
    public List<ItemStack> getItemsTrimmed() {
        List<ItemStack> list = new ArrayList<>(items);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (!list.get(i).isEmpty()) {
                break;
            }
            list.remove(i);
        }
        return list;
    }

    /**
     * Item should not be modified. Copy the stack if modification is needed.
     */
    public @NotNull ItemStack getItem(int index) {
        return index < size() ? items.get(index) : ItemStack.EMPTY;
    }

    /**
     * Returned item can be modified safely.
     */
    public @NotNull ItemStack copyItem(int index) {
        return getItem(index).copy();
    }

    /**
     * Returned items can be modified safely.
     */
    public List<ItemStack> copyItems() {
        return Lists.transform(this.items, ItemStack::copy);
    }

    // --

    @SuppressWarnings("deprecation")
    public boolean equals(Object another) {
        return this == another ||
              (another instanceof PackageContents packageContents
                    && ItemStack.listMatches(this.items, packageContents.items));
    }

    @SuppressWarnings("deprecation")
    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }

    public String toString() {
        return "PackageContents" + this.items;
    }

    // --

    public static boolean canHold(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems() && !stack.is(Envelope.Tags.Items.CANNOT_BE_PACKAGED);
    }
}
