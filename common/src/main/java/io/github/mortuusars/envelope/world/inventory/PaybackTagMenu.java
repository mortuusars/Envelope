package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.FilteredSlot;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import io.github.mortuusars.envelope.world.item.component.PaybackTagContents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PaybackTagMenu extends AbstractInHandContainerMenu {
    public static final int CONFIRM_BUTTON_ID = 0;

    public static final int COUNT_START_BUTTON_ID = 100;
    public static final int INCREASE_COUNT_START_BUTTON_ID = COUNT_START_BUTTON_ID;
    public static final int INCREASE_COUNT_FAST_START_BUTTON_ID = INCREASE_COUNT_START_BUTTON_ID + PaybackTagContents.SLOTS;
    public static final int DECREASE_COUNT_START_BUTTON_ID = INCREASE_COUNT_FAST_START_BUTTON_ID + PaybackTagContents.SLOTS;
    public static final int DECREASE_COUNT_FAST_START_BUTTON_ID = DECREASE_COUNT_START_BUTTON_ID + PaybackTagContents.SLOTS;

    protected PaybackTagMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, InteractionHand hand) {
        super(menuType, containerId, inventory, hand);
    }

    public PaybackTagMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PAYBACK_TAG.get(), containerId, playerInventory, hand);
    }

    public static PaybackTagMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PaybackTagMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    // --


    @Override
    protected void init() {
        playerSlotsX = 13;
        playerSlotsY = 72;
        super.init();
    }

    @Override
    protected Container createContainer() {
        List<ItemStack> items = new ArrayList<>();

        @Nullable PaybackTagContents paybackTagContents = getItemInHand().get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS);
        if (paybackTagContents != null) {
            for (int i = 0; i < Math.min(paybackTagContents.size(), PaybackTagContents.SLOTS); i++) {
                items.add(paybackTagContents.getItemForReading(i));
            }
        }

        while (items.size() < PaybackTagContents.SLOTS) {
            items.add(ItemStack.EMPTY);
        }

        return new SimpleContainer(items.toArray(ItemStack[]::new));
    }

    @Override
    protected void addContainerSlots() {
        int slotsX = 67;
        int slotsY = 20;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;
                int x = slotsX + column * 18;
                int y = slotsY + row * 18;
                addSlot(new FilteredSlot(getContainer(), index, x, y, PaybackRequest::isValidPaybackItem));
            }
        }
    }

    // --

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getItem();

        if (index < PackageContents.SLOTS) {
            getContainer().setItem(index, ItemStack.EMPTY);
            getContainer().setChanged();
        }
        else if (index < slots.size()) {
            for (int i = 0; i < PaybackTagContents.SLOTS; i++) {
                Slot paybackSlot = slots.get(i);
                if (paybackSlot.getItem().isEmpty() && paybackSlot.mayPlace(clickedStack)) {
                    paybackSlot.set(clickedStack.copy());
                    paybackSlot.setChanged();
                    break;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (buttonId == CONFIRM_BUTTON_ID) {
            PaybackTagContents contents = PaybackTagContents.create(getContainer());
            if (contents.isEmpty()) {
                getItemInHand().remove(Envelope.DataComponents.PAYBACK_TAG_CONTENTS);
            } else {
                getItemInHand().set(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, contents);
            }
            player.level().playSound(player, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS,
                  1f, player.level().getRandom().nextFloat() * 0.3f + 0.85f);
            player.setItemInHand(getHand(), getItemInHand());
            player.swing(getHand());
            return true;
        }

        if (buttonId >= COUNT_START_BUTTON_ID) {
            int id = buttonId - COUNT_START_BUTTON_ID;
            int slotIndex = id % PaybackTagContents.SLOTS;

            boolean decrease = id >= PaybackTagContents.SLOTS * 2;
            boolean fast = id % (PaybackTagContents.SLOTS * 2) >= PaybackTagContents.SLOTS;

            int change = (fast ? 5 : 1);
            if (decrease) {
                change *= -1;
            }

            ItemStack stack = getContainer().getItem(slotIndex);
            stack.setCount(Mth.clamp(stack.getCount() + change, 1, stack.getMaxStackSize()));
            getContainer().setChanged();
        }

        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= PaybackTagContents.SLOTS) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        Slot paybackSlot = this.slots.get(slotId);

        if (!getCarried().isEmpty() && paybackSlot.mayPlace(getCarried())) {
            ItemStack result = getCarried().copy();

            if (button == 1) {
                if (ItemStack.isSameItemSameComponents(paybackSlot.getItem(), result)) {
                    result.setCount(paybackSlot.getItem().getCount() + 1);
                } else {
                    result.setCount(1);
                }
            }
            paybackSlot.set(result);
        } else {
            paybackSlot.set(ItemStack.EMPTY);
        }
    }
}
