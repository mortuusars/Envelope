package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class AbstractInHandContainerMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final Item usedItem;
    private final int usedSlot;
    private final Container container;

    protected int playerSlotsX = 8;
    protected int playerSlotsY = 96;

    public AbstractInHandContainerMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.usedItem = player.getItemInHand(hand).getItem();
        this.usedSlot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : playerInventory.selected;
        this.container = createContainer();
        init();
    }

    public Player getPlayer() {
        return player;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getItemInHand() {
        return getPlayer().getItemInHand(getHand());
    }

    public Item getUsedItem() {
        return usedItem;
    }

    public int getUsedSlot() {
        return usedSlot;
    }

    public Container getContainer() {
        return Objects.requireNonNull(container, "Called too early. Container has not been created yet.");
    }

    // --

    protected void init() {
        addContainerSlots();
        addPlayerSlots(getPlayer().getInventory(), playerSlotsX, playerSlotsY);
    }

    protected abstract Container createContainer();

    protected abstract void addContainerSlots();

    protected void addPlayerSlots(Container inventory, int x, int y) {
        // Hotbar
        for (int index = 0; index < 9; index++) {
            int slotX = x + index * 18;
            int slotY = y + 58;
            addSlot(index == getUsedSlot()
                  ? new DisabledSlot(inventory, index, slotX, slotY)
                  : new Slot(inventory, index, slotX, slotY));
        }

        // Inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = (column + row * 9) + 9;
                int slotX = x + column * 18;
                int slotY = y + row * 18;
                addSlot(index == getUsedSlot()
                      ? new DisabledSlot(inventory, index, slotX, slotY)
                      : new Slot(inventory, index, slotX, slotY));
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getItem();

        if (index < getContainer().getContainerSize()) {
            if (!moveItemStackTo(clickedStack, getContainer().getContainerSize(), slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < slots.size()) {
            if (!moveItemStackTo(clickedStack, 0, getContainer().getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }
        }

        getContainer().setChanged();

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() == getUsedItem();
    }
}
