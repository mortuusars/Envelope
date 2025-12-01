package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public record Payback(List<Ingredient> ingredients) {
    public static final Codec<Payback> CODEC =
          Codec.list(Ingredient.CODEC, 0, 6).xmap(Payback::new, Payback::ingredients);
    public static final StreamCodec<RegistryFriendlyByteBuf, Payback> STREAM_CODEC =
          Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(6)).map(Payback::new, Payback::ingredients);

    public static final int SLOTS = 6;

    public static Payback of(Container container) {
        List<Ingredient> ingredients = new ArrayList<>();

        //TODO: maybe use another container (using .add() method) to collapse same items into stacks?
        //TODO: extended ingredients?

        for (int slot = 0; slot < Math.min(SLOTS, container.getContainerSize()); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                ingredients.add(Ingredient.of(stack));
            }
        }

        return new Payback(ingredients);
    }
}
