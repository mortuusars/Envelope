package io.github.mortuusars.envelope.world.inventory;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Objects;

/**
 * Advanced ingredient to allow matching stacks of specific count and/or components.
 * <br>
 * Similar to {@link net.minecraft.advancements.critereon.ItemPredicate} but more suited for inventories and crafting.
 * <br>
 * <br>
 * It is detached from all vanilla ingredient system, so logic for it has to be manually written.
 */
@SuppressWarnings("deprecation")
public class StackIngredient {
    public static final Codec<StackIngredient> CODEC = RecordCodecBuilder.create(i -> i.group(
          HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false)
                .fieldOf("item")
                .forGetter(StackIngredient::items),
          ExtraCodecs.intRange(1, 99)
                .optionalFieldOf("count", 1)
                .forGetter(StackIngredient::count),
          DataComponentPredicate.CODEC
                .optionalFieldOf("components", DataComponentPredicate.EMPTY)
                .forGetter(StackIngredient::components),
          Codec.BOOL
                .optionalFieldOf("strict", false)
                .forGetter(StackIngredient::isStrict)
    ).apply(i, StackIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StackIngredient> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.holderSet(Registries.ITEM), StackIngredient::items,
          ByteBufCodecs.INT, StackIngredient::count,
          DataComponentPredicate.STREAM_CODEC, StackIngredient::components,
          ByteBufCodecs.BOOL, StackIngredient::isStrict,
          StackIngredient::new
    );

    private final HolderSet<Item> items;
    private final int count;
    private final DataComponentPredicate components;
    private final boolean strict;
    private final ItemStack[] stacks;

    public StackIngredient(HolderSet<Item> items, int count, DataComponentPredicate components, boolean strict) {
        Preconditions.checkArgument(count >= 1 && count <= 99, "Count must be in range 1-99.");
        this.items = items;
        this.count = count;
        this.components = components;
        this.strict = strict;
        this.stacks = items.stream()
              .map(item -> new ItemStack(item, count, components.asPatch()))
              .toArray(ItemStack[]::new);
    }

    public StackIngredient(Item item, int count, DataComponentPredicate components) {
        this(HolderSet.direct(item.builtInRegistryHolder()), count, components, false);
    }

    public StackIngredient(Item item, int count) {
        this(item, count, DataComponentPredicate.EMPTY);
    }

    public StackIngredient(Item item) {
        this(item, 1);
    }

    public StackIngredient(TagKey<Item> tag, int count, DataComponentPredicate components) {
        this(BuiltInRegistries.ITEM.getTag(tag).orElseThrow(), count, components, false);
    }

    public StackIngredient(TagKey<Item> tag, int count) {
        this(tag, count, DataComponentPredicate.EMPTY);
    }

    public StackIngredient(TagKey<Item> tag) {
        this(tag, 1);
    }

    // --

    public static StackIngredient createDefault() {
        return new StackIngredient(HolderSet.direct(Items.EMERALD.builtInRegistryHolder()),
              1, DataComponentPredicate.EMPTY, false);
    }

    // --

    public HolderSet<Item> items() {
        return items;
    }

    public int count() {
        return count;
    }

    public DataComponentPredicate components() {
        return components;
    }

    public boolean isStrict() {
        return strict;
    }

    public ItemStack[] stacks() {
        return stacks;
    }

    // --

    public boolean test(ItemStack stack) {
        return countEquals(stack) && testWithoutCount(stack);
    }

    public boolean testWithoutCount(ItemStack stack) {
        return stack.is(items()) && componentsMatch(stack);
    }

    public boolean countEquals(ItemStack stack) {
        return stack.getCount() == count();
    }

    public boolean componentsMatch(ItemStack stack) {
        return strict
              ? stacks.length != 0 && Objects.equals(stack.getComponents(), stacks[0].getComponents())
              : components().test(stack);
    }

    // --

    public ItemStack getRollingDisplayedStack() {
        if (stacks.length == 0) return ItemStack.EMPTY;
        int index = (int)(Util.getMillis() / 1000) % stacks.length;
        return stacks[index];
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StackIngredient that = (StackIngredient) o;
        return count == that.count && strict == that.strict && Objects.equals(items, that.items) && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, count, components, strict, Arrays.hashCode(stacks));
    }

    @Override
    public String toString() {
        return "StackIngredient{" +
              "items=" + items +
              ", count=" + count +
              ", components=" + components +
              ", strict=" + strict +
              '}';
    }
}