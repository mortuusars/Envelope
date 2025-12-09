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

public record RequestedItem(Either<TagKey<Item>, Item> item, int count, DataComponentPredicate components) {
    public static final Codec<RequestedItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.xor(TagKey.hashedCodec(Registries.ITEM), BuiltInRegistries.ITEM.byNameCodec())
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
                ByteBufCodecs.holderRegistry(Registries.ITEM).map(Holder::value, Holder::direct)), RequestedItem::item,
          ByteBufCodecs.INT, RequestedItem::count,
          DataComponentPredicate.STREAM_CODEC, RequestedItem::components,
          RequestedItem::new
    );

    public RequestedItem {
        Preconditions.checkArgument(count >= 1 && count <= 99, "Count must be in range 1-99.");
    }

    public RequestedItem(TagKey<Item> tag, int count, DataComponentPredicate components) {
        this(Either.left(tag), count, components);
    }

    public RequestedItem(Item item, int count, DataComponentPredicate components) {
        this(Either.right(item), count, components);
    }

    // --

    public boolean matches(ItemStack stack) {
        return typeMatches(stack)
              && countMatches(stack)
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