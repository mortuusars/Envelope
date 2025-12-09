package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record Payback(List<RequestedItem> items) {
    public static final int SLOTS = 6;

    public static final Codec<Payback> CODEC =
          Codec.list(RequestedItem.CODEC, 1, 6).xmap(Payback::new, Payback::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, Payback> STREAM_CODEC =
          RequestedItem.STREAM_CODEC.apply(ByteBufCodecs.list(6)).map(Payback::new, Payback::items);


//    public static Payback of(Container container) {
//        SimpleContainer collapsedContainer = new SimpleContainer(SLOTS);
//        for (int slot = 0; slot < Math.min(SLOTS, container.getContainerSize()); slot++) {
//            ItemStack stack = container.getItem(slot);
//            collapsedContainer.addItem(stack);
//        }
//        container = collapsedContainer;
//
//        List<RequestedItem> items = new ArrayList<>();
//
//        for (int slot = 0; slot < Math.min(SLOTS, container.getContainerSize()); slot++) {
//            ItemStack stack = container.getItem(slot);
//            if (!stack.isEmpty()) {
//                RequestedItem requestedItem = new RequestedItem(stack.getItem(), stack.getCount(),
//                      DataComponentPredicate.allOf(stack.getComponents()));
//                items.add(requestedItem);
//            }
//        }
//
//        return new Payback(items);
//    }

    public boolean matches(Container container) {
        if (container.getContainerSize() != items().size()) {
            return false;
        }

        for (int slot = 0; slot < items().size(); slot++) {
            RequestedItem requestedItem = items().get(slot);
            ItemStack stack = container.getItem(slot);
            if (!requestedItem.matches(stack)) {
                return false;
            }
        }

        return true;
    }
}
