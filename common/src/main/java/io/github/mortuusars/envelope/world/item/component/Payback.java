package io.github.mortuusars.envelope.world.item.component;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Payback {
    public static final int SLOTS = 6;
    public static final Payback DEFAULT = new Payback(List.of(RequestedItem.DEFAULT));

    public static final Codec<Payback> CODEC =
          Codec.list(RequestedItem.CODEC, 1, 6).xmap(Payback::new, Payback::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, Payback> STREAM_CODEC =
          RequestedItem.STREAM_CODEC.apply(ByteBufCodecs.list(6)).map(Payback::new, Payback::items);
    private final List<RequestedItem> items;

    private Payback(List<RequestedItem> items) {
        Preconditions.checkArgument(!items.isEmpty(), "Payback items must have at least one requested item.");
        this.items = items;
    }

    public static Optional<Payback> create(List<RequestedItem> items) {
        return !items.isEmpty() ? Optional.of(new Payback(items)) : Optional.empty();
    }

    public static Payback createOrDefault(List<RequestedItem> items) {
        return !items.isEmpty() ? new Payback(items) : DEFAULT;
    }

    // --

    public boolean matches(Container container) {
        for (int slot = 0; slot < items().size(); slot++) {
            RequestedItem requestedItem = items().get(slot);
            ItemStack stack = container.getItem(slot);
            if (!requestedItem.matches(stack)) {
                return false;
            }
        }

        return true;
    }

    public List<RequestedItem> items() {
        return items;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Payback) obj;
        return Objects.equals(this.items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        return "Payback[" +
              "items=" + items + ']';
    }

}
