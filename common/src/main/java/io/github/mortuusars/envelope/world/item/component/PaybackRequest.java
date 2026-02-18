package io.github.mortuusars.envelope.world.item.component;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

public record PaybackRequest(List<StackIngredient> items, PaybackDuration duration) implements TooltipComponent {
    public static final int SLOTS = 6;

    private static final Codec<PaybackRequest> FULL_CODEC = RecordCodecBuilder.create(i -> i.group(
          Codec.list(StackIngredient.CODEC, 1, SLOTS).fieldOf("ingredients").forGetter(PaybackRequest::items),
          PaybackDuration.CODEC.optionalFieldOf("duration", PaybackDuration.MEDIUM).forGetter(PaybackRequest::duration)
    ).apply(i, PaybackRequest::new));

    private static final Codec<PaybackRequest> SIMPLE_CODEC = Codec.list(StackIngredient.CODEC, 1, 6)
          .xmap(PaybackRequest::new, PaybackRequest::items);

    public static final Codec<PaybackRequest> CODEC = Codec.withAlternative(FULL_CODEC, SIMPLE_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, PaybackRequest> STREAM_CODEC = StreamCodec.composite(
          StackIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(SLOTS)), PaybackRequest::items,
          PaybackDuration.STREAM_CODEC, PaybackRequest::duration,
          PaybackRequest::new
    );

    public PaybackRequest {
        Preconditions.checkArgument(!items.isEmpty(), "Payback must have at least one requested item.");
    }

    public PaybackRequest(List<StackIngredient> items) {
        this(items, PaybackDuration.MEDIUM);
    }

    public static Optional<PaybackRequest> create(List<StackIngredient> items, PaybackDuration duration) {
        return !items.isEmpty() ? Optional.of(new PaybackRequest(items, duration)) : Optional.empty();
    }

    public static PaybackRequest createOrDefault(List<StackIngredient> items, PaybackDuration duration) {
        return !items.isEmpty()
              ? new PaybackRequest(items, duration)
              : createDefault(duration);
    }

    public static PaybackRequest createDefault() {
        return new PaybackRequest(List.of(StackIngredient.createDefault()), PaybackDuration.MEDIUM);
    }

    public static PaybackRequest createDefault(PaybackDuration duration) {
        return new PaybackRequest(List.of(StackIngredient.createDefault()), duration);
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

    public static boolean isValid(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems() && !stack.is(Envelope.Tags.Items.CANNOT_BE_PACKAGED) && !stack.is(Envelope.Tags.Items.CANNOT_BE_USED_AS_PAYBACK);
    }
}
