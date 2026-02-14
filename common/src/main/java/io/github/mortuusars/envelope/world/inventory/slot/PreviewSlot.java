package io.github.mortuusars.envelope.world.inventory.slot;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PreviewSlot extends Slot {
    public static final ResourceLocation OVERLAY_SPRITE = Envelope.resource("preview_slot_overlay");

    public PreviewSlot(ItemStack stack, int slot, int x, int y) {
        super(new SimpleContainer(stack), slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
