package io.github.mortuusars.envelope.world.item.component;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record RequestedPayback(List<RequestedItem> items) implements TooltipComponent {
    public static final int SLOTS = 6;
    public static final RequestedPayback DEFAULT = new RequestedPayback(List.of(RequestedItem.DEFAULT));

    public static final Codec<RequestedPayback> CODEC =
          Codec.list(RequestedItem.CODEC, 1, 6).xmap(RequestedPayback::new, RequestedPayback::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestedPayback> STREAM_CODEC =
          RequestedItem.STREAM_CODEC.apply(ByteBufCodecs.list(6)).map(RequestedPayback::new, RequestedPayback::items);

    public RequestedPayback {
        Preconditions.checkArgument(!items.isEmpty(), "Payback must have at least one requested item.");
    }

    public static Optional<RequestedPayback> create(List<RequestedItem> items) {
        return !items.isEmpty() ? Optional.of(new RequestedPayback(items)) : Optional.empty();
    }

    public static RequestedPayback createOrDefault(List<RequestedItem> items) {
        return !items.isEmpty() ? new RequestedPayback(items) : DEFAULT;
    }

    // --

    public boolean matches(Container container) {
        for (int slot = 0; slot < items().size(); slot++) {
            RequestedItem requestedItem = items().get(slot);
            if (slot >= container.getContainerSize()) {
                return false;
            }
            ItemStack stack = container.getItem(slot);
            if (!requestedItem.matches(stack)) {
                return false;
            }
        }

        return true;
    }

    public boolean matches(PackageContents packageContents) {
        for (int slot = 0; slot < items().size(); slot++) {
            RequestedItem requestedItem = items().get(slot);
            if (slot >= packageContents.size()) {
                return false;
            }
            ItemStack stack = packageContents.getItemForReading(slot);
            if (!requestedItem.matches(stack)) {
                return false;
            }
        }

        return true;
    }

    public Optional<RequestedItem> getRequestedItem(int index) {
        return index < items.size() ? Optional.of(items.get(index)) : Optional.empty();
    }

    // --

    public static boolean isValidPaybackItem(ItemStack stack) {
        return Envelope.Items.PACKING_BOX.get().canInsert(stack) && !stack.is(Envelope.Tags.Items.CANNOT_BE_USED_AS_PAYBACK);
    }
}
