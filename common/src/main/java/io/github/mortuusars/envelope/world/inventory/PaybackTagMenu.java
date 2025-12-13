package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Pos2i;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.PaybackTagItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.PaybackTagContents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PaybackTagMenu extends AbstractContainerMenu {
    public static final int CONFIRM_BUTTON_ID = 0;

    public static final int COUNT_START_BUTTON_ID = 100;
    public static final int INCREASE_COUNT_START_BUTTON_ID = COUNT_START_BUTTON_ID;
    public static final int INCREASE_COUNT_FAST_START_BUTTON_ID = INCREASE_COUNT_START_BUTTON_ID + PaybackTagContents.SLOTS;
    public static final int DECREASE_COUNT_START_BUTTON_ID = INCREASE_COUNT_FAST_START_BUTTON_ID + PaybackTagContents.SLOTS;
    public static final int DECREASE_COUNT_FAST_START_BUTTON_ID = DECREASE_COUNT_START_BUTTON_ID + PaybackTagContents.SLOTS;

    private final Player player;
    private final InteractionHand hand;
    private final int tagSlot;
    private final ItemAndStack<Item> tagStack;
    private final SimpleContainer paybackContainer;
    protected Pos2i tagSlotPos = new Pos2i(-999, -999);

    protected PaybackTagMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.tagSlot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : playerInventory.selected;
        this.tagStack = new ItemAndStack<>(playerInventory.getItem(tagSlot));
        this.paybackContainer = createPaybackContainer();

        addPaybackSlots();
        addPlayerSlots(playerInventory, 13, 68, tagSlot);
    }

    public PaybackTagMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PAYBACK_TAG.get(), containerId, playerInventory, hand);
    }

    public static PaybackTagMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PaybackTagMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    // --

    protected SimpleContainer createPaybackContainer() {
        List<ItemStack> items = new ArrayList<>();

        @Nullable PaybackTagContents paybackTagContents = tagStack.get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS);
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

    protected void addPaybackSlots() {
        int slotsX = 67;
        int slotsY = 18;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;
                int x = slotsX + column * 18;
                int y = slotsY + row * 18;
                addSlot(new Slot(paybackContainer, index, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        //TODO: Maybe find a better place for valid items check. And maybe even add additional filters for payback.
                        return Envelope.Items.PACKING_BOX.get().canInsert(stack);
                    }

                    @Override
                    public void set(ItemStack stack) {
                        // stack = new ItemStack(stack.getItem(), stack.getCount()); // Remove components
                        super.set(stack);
                    }
                });
            }
        }
    }

    protected void addPlayerSlots(Container playerInventory, int x, int y, int packageSlot) {
        // Hotbar
        for (int slot = 0; slot < 9; slot++) {
            int slotX = x + slot * 18;
            int slotY = y + 58;

            if (slot == packageSlot) {
                tagSlotPos = new Pos2i(slotX, slotY);
                continue;
            }

            addSlot(new Slot(playerInventory, slot, slotX, slotY));
        }

        // Inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = (column + row * 9) + 9;
                int slotX = x + column * 18;
                int slotY = y + row * 18;

                if (slot == packageSlot) {
                    tagSlotPos = new Pos2i(slotX, slotY);
                    continue;
                }

                addSlot(new Slot(playerInventory, slot, slotX, slotY));
            }
        }
    }

    // --

    public Player getPlayer() {
        return player;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public int getTagSlot() {
        return tagSlot;
    }

    public ItemAndStack<Item> getTag() {
        return tagStack;
    }

    public Pos2i getTagSlotPos() {
        return tagSlotPos;
    }

    // --

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getItem();

        if (index < PackageContents.SLOTS) {
            paybackContainer.setItem(index, ItemStack.EMPTY);
            paybackContainer.setChanged();
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
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            getTag().set(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, PaybackTagContents.create(paybackContainer));
            player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS,
                  1f, player.level().getRandom().nextFloat() * 0.3f + 0.85f);
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

            ItemStack stack = paybackContainer.getItem(slotIndex);
            stack.setCount(Mth.clamp(stack.getCount() + change, 1, stack.getMaxStackSize()));
            paybackContainer.setChanged();
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

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof PaybackTagItem;
    }
}
