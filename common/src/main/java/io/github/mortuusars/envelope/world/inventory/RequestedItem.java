package io.github.mortuusars.envelope.world.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class RequestedItem {
    private static final Codec<RequestedItem> BASE_CODEC = RecordCodecBuilder.create(i -> i.group(
                TagKey.hashedCodec(Registries.ITEM).optionalFieldOf("tag").forGetter(RequestedItem::getTag),
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(RequestedItem::getItem),
                ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(RequestedItem::getCount),
                DataComponentPredicate.CODEC.optionalFieldOf("components", DataComponentPredicate.EMPTY).forGetter(RequestedItem::getComponents))
          .apply(i, RequestedItem::new));
    public static final Codec<RequestedItem> CODEC = BASE_CODEC.validate(RequestedItem::validate);

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestedItem> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id), TagKey::location)), RequestedItem::getTag,
          ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.ITEM).map(Holder::value, Holder::direct)), RequestedItem::getItem,
          ByteBufCodecs.INT, RequestedItem::getCount,
          DataComponentPredicate.STREAM_CODEC, RequestedItem::getComponents,
          RequestedItem::new
    );

    private final Optional<TagKey<Item>> tag;
    private final Optional<Item> item;
    private final int count;
    private final DataComponentPredicate components;

    private RequestedItem(Optional<TagKey<Item>> tag, Optional<Item> item, int count, DataComponentPredicate components) {
        this.tag = tag;
        this.item = item;
        this.count = count;
        this.components = components;
    }

    public RequestedItem(TagKey<Item> tag, int count, DataComponentPredicate components) {
        this(Optional.ofNullable(tag), Optional.empty(), count, components);
    }

    public RequestedItem(Item item, int count, DataComponentPredicate components) {
        this(Optional.empty(), Optional.ofNullable(item), count, components);
    }

    private DataResult<RequestedItem> validate() {
        if (tag.isEmpty() && item.isEmpty()) return DataResult.error(() -> "Both tag and item cannot be empty.");
        if (tag.isPresent() && item.isPresent()) return DataResult.error(() -> "Both tag and item cannot be present.");
        return DataResult.success(this);
    }

    // --

    public Optional<TagKey<Item>> getTag() {
        return tag;
    }

    public Optional<Item> getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public DataComponentPredicate getComponents() {
        return components;
    }

    // --

    public boolean matches(ItemStack stack) {
        return typeMatches(stack)
              && countMatches(stack)
              && componentsMatch(stack);
    }

    public boolean typeMatches(ItemStack stack) {
        return getTag().map(stack::is).orElse(false)
              || getItem().map(stack::is).orElse(false);
    }

    public boolean countMatches(ItemStack stack) {
        return stack.getCount() >= Math.min(getCount(), stack.getMaxStackSize());
    }

    public boolean countEquals(ItemStack stack) {
        return stack.getCount() == Math.min(getCount(), stack.getMaxStackSize());
    }

    public boolean componentsMatch(ItemStack stack) {
        return getComponents().test(stack);
    }

//    public List<ItemStack> getStacks() {
//        return tagOrItem.map(tag -> Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(tag).stream().map(item -> {
//            ItemStack stack = new ItemStack(item, getCount());
//            stack.setTag(getTag());
//            return stack;
//        }).toList(), item -> {
//            ItemStack stack = new ItemStack(item, getCount());
//            stack.setTag(getTag());
//            return List.of(stack);
//        });
//    }
//
//    @SuppressWarnings("Convert2MethodRef")
//    public boolean matches(ItemStack stack) {
//        return getTagOrItem().map(item -> stack.is(item), tag -> stack.is(tag)) && tagMatches(stack);
//    }
//
//    @SuppressWarnings("unused")
//    public boolean matchesWithCount(ItemStack stack) {
//        return matches(stack) && stack.getCount() >= getCount();
//    }
//
//    public boolean tagMatches(ItemStack stack) {
//        switch (tagCompareBehavior) {
//            case IGNORE -> {
//                return true;
//            }
//            case WEAK -> {
//                return NbtUtils.compareNbt(getTag(), stack.getTag(), true);
//            }
//            case STRONG -> {
//                CompoundTag tag = getTag();
//                CompoundTag stackTag = stack.getTag();
//                if (tag == null || tag.isEmpty())
//                    return stackTag == null || stackTag.isEmpty();
//
//                return tag.equals(stackTag);
//            }
//        }
//
//        return false;
//    }
//
//    public boolean isEmpty() {
//        return this.equals(EMPTY) || getTagOrItem().map(tag -> false, item -> item instanceof AirItem);
//    }

    // --

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestedItem that = (RequestedItem) o;
        return count == that.count && Objects.equals(tag, that.tag) && Objects.equals(item, that.item) && Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, item, count, components);
    }

    @Override
    public String toString() {
        return tag.map(tag -> "RequestedItem{" +
                    "tag=" + tag +
                    ", count=" + count +
                    ", components=" + components +
                    '}')
              .orElseGet(() -> "RequestedItem{" +
                    "item=" + item.orElse(Items.AIR) +
                    ", count=" + count +
                    ", components=" + components +
                    '}');
    }
}

