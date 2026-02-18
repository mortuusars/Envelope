package io.github.mortuusars.envelope.integration.jei.util;

import io.github.mortuusars.envelope.client.gui.screen.PaybackTagScreen;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.PaybackTagMenuSetSlotC2SP;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaybackTagGhostIngredientHandler implements IGhostIngredientHandler<PaybackTagScreen> {
    @Override
    public <I> @NotNull List<Target<I>> getTargetsTyped(PaybackTagScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> itemStack = ingredient.getItemStack();
        if (itemStack.isEmpty()) {
            return List.of();
        }

        ItemStack stack = itemStack.get();
        if (!PaybackRequest.isValid(stack)) {
            return List.of();
        }

        List<Target<I>> targets = new ArrayList<>();

        for (int index = 0; index < PaybackRequest.SLOTS; index++) {
            Slot slot = screen.getMenu().slots.get(index);
            targets.add(new Target<>() {
                @Override
                public @NotNull Rect2i getArea() {
                    return new Rect2i(screen.getLeftPos() + slot.x - 1, screen.getTopPos() + slot.y - 1, 18, 18);
                }

                @Override
                public void accept(I ingredient) {
                    if (ingredient instanceof ItemStack stackIngredient) {
                        slot.setByPlayer(stackIngredient);
                        Packets.sendToServer(new PaybackTagMenuSetSlotC2SP(slot.index, stackIngredient));
                    }
                }
            });
        }

        return targets;
    }

    @Override
    public void onComplete() {
    }
}
