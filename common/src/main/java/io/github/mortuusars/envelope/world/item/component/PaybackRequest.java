package io.github.mortuusars.envelope.world.item.component;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record PaybackRequest(List<StackIngredient> items) implements TooltipComponent {
    public static final int SLOTS = 6;

    public static final Codec<PaybackRequest> CODEC =
          Codec.list(StackIngredient.CODEC, 1, 6).xmap(PaybackRequest::new, PaybackRequest::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, PaybackRequest> STREAM_CODEC =
          StackIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(6)).map(PaybackRequest::new, PaybackRequest::items);

    public PaybackRequest {
        Preconditions.checkArgument(!items.isEmpty(), "Payback must have at least one requested item.");
    }

    public static Optional<PaybackRequest> create(List<StackIngredient> items) {
        return !items.isEmpty() ? Optional.of(new PaybackRequest(items)) : Optional.empty();
    }

    public static PaybackRequest createOrDefault(List<StackIngredient> items) {
        return !items.isEmpty()
              ? new PaybackRequest(items)
              : createDefault();
    }

    public static PaybackRequest createDefault() {
        return new PaybackRequest(List.of(StackIngredient.createDefault()));
    }

    // --

    public boolean matches(Container container) {
        for (int slot = 0; slot < items().size(); slot++) {
            StackIngredient stackIngredient = items().get(slot);
            if (slot >= container.getContainerSize()) {
                return false;
            }
            ItemStack stack = container.getItem(slot);
            if (!stackIngredient.test(stack)) {
                return false;
            }
        }

        return true;
    }

    public boolean matches(PackageContents packageContents) {
        for (int slot = 0; slot < items().size(); slot++) {
            StackIngredient stackIngredient = items().get(slot);
            if (slot >= packageContents.size()) {
                return false;
            }
            ItemStack stack = packageContents.getItem(slot);
            if (!stackIngredient.test(stack)) {
                return false;
            }
        }

        return true;
    }

    public Optional<StackIngredient> getRequestedItem(int index) {
        return index < items.size() ? Optional.of(items.get(index)) : Optional.empty();
    }

    // --

    public static boolean isValidPaybackItem(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems() && !stack.is(Envelope.Tags.Items.CANNOT_BE_PACKAGED) && !stack.is(Envelope.Tags.Items.CANNOT_BE_USED_AS_PAYBACK);
    }
}
