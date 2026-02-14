package io.github.mortuusars.envelope.world.inventory.slot;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DisabledSlot extends Slot {
    public static final ResourceLocation OVERLAY_SPRITE = Envelope.resource("disabled_slot_overlay");

    private boolean active = true;

    public DisabledSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public DisabledSlot setActive(boolean active) {
        this.active = active;
        return this;
    }
}
