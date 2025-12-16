package io.github.mortuusars.envelope.world.inventory;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
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
import net.minecraft.world.level.ItemLike;

import java.util.List;

public record RequestedItem(Either<TagKey<Item>, Holder<Item>> item, int count, DataComponentPredicate components) {
    public static final Codec<RequestedItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.xor(TagKey.hashedCodec(Registries.ITEM), ItemStack.ITEM_NON_AIR_CODEC)
                      .fieldOf("item")
                      .forGetter(RequestedItem::item),
                ExtraCodecs.intRange(1, 99)
                      .fieldOf("count").orElse(1)
                      .forGetter(RequestedItem::count),
                DataComponentPredicate.CODEC
                      .optionalFieldOf("components", DataComponentPredicate.EMPTY)
                      .forGetter(RequestedItem::components))
          .apply(i, RequestedItem::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestedItem> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.either(
                ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id), TagKey::location),
                ByteBufCodecs.holderRegistry(Registries.ITEM)), RequestedItem::item,
          ByteBufCodecs.INT, RequestedItem::count,
          DataComponentPredicate.STREAM_CODEC, RequestedItem::components,
          RequestedItem::new
    );

    public static final RequestedItem DEFAULT = new RequestedItem(Items.EMERALD);

    public RequestedItem {
        Preconditions.checkArgument(count >= 1 && count <= 99, "Count must be in range 1-99.");
    }

    public RequestedItem(TagKey<Item> tag, int count, DataComponentPredicate components) {
        this(Either.left(tag), count, components);
    }

    public RequestedItem(TagKey<Item> tag, int count) {
        this(tag, count, DataComponentPredicate.EMPTY);
    }

    public RequestedItem(TagKey<Item> tag) {
        this(tag, 1);
    }

    // Item#builtInRegistryHolder is most likely deprecated because mojang wants us to get it through ItemStack#getItemHolder.
    // Same deprecations are present in other places, like Block for example.
    @SuppressWarnings("deprecation")
    public RequestedItem(ItemLike item, int count, DataComponentPredicate components) {
        this(Either.right(item.asItem().builtInRegistryHolder()), count, components);
    }

    public RequestedItem(ItemLike item, int count) {
        this(item, count, DataComponentPredicate.EMPTY);
    }

    public RequestedItem(ItemLike item) {
        this(item, 1);
    }

    // --

    public List<Item> items() {
        return this.item().map(
              tag -> BuiltInRegistries.ITEM.getTag(tag)
                    .map(named -> named.stream()
                          .map(Holder::value)
                          .toList())
                    .orElse(List.of(Items.BARRIER)),
              itemHolder -> List.of(itemHolder.value()));
    }

    // --

    public boolean matches(ItemStack stack) {
        return typeMatches(stack)
              && countEquals(stack)
              && componentsMatch(stack);
    }

    public boolean typeMatches(ItemStack stack) {
        return item.map(stack::is, stack::is);
    }

    public boolean countMatches(ItemStack stack) {
        return stack.getCount() >= Math.min(count(), stack.getMaxStackSize());
    }

    public boolean countEquals(ItemStack stack) {
        return stack.getCount() == Math.min(count(), stack.getMaxStackSize());
    }

    public boolean componentsMatch(ItemStack stack) {
        return components().test(stack);
    }
}