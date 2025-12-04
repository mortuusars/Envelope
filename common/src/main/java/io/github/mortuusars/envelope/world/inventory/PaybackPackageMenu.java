package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Pos2i;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.item.component.StoredItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PaybackPackageMenu extends AbstractContainerMenu {
    public static final int PACK_BUTTON_ID = 0;

    protected final Player player;
    protected final InteractionHand hand;
    protected final int packageSlot;
    protected final ItemAndStack<PaybackPackageItem> packageStack;
    protected final SimpleContainer packageContainer;
    protected final Payback payback;

    protected Pos2i packageSlotPos = new Pos2i(-999, -999);

    protected boolean packed = false;

    protected PaybackPackageMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.packageSlot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : playerInventory.selected;
        this.packageStack = new ItemAndStack<>(playerInventory.getItem(packageSlot));
        this.payback = packageStack.getOrDefault(Envelope.DataComponents.PAYBACK, new Payback(List.of()));
        this.packageContainer = new SimpleContainer(PackageContents.SLOTS);

        StoredItemStack paybackItem = packageStack.getOrDefault(Envelope.DataComponents.PAYBACK_ITEM, StoredItemStack.EMPTY);

        addSlot(new Slot(new SimpleContainer(paybackItem.getForReading()), 0, 21, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return false;
            }
        });

        addPackageSlots();
        addPlayerSlots(playerInventory, 8, 96, packageSlot);
    }

    public PaybackPackageMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PAYBACK_PACKAGE.get(), containerId, playerInventory, hand);
    }

    public static PaybackPackageMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PaybackPackageMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    // --

    protected void addPackageSlots() {
        int packageSlotsX = 62;
        int packageSlotsY = 33;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;
                int x = packageSlotsX + column * 18;
                int y = packageSlotsY + row * 18;
                addSlot(new Slot(packageContainer, index, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return mayPlaceIntoPackageSlot(stack, getContainerSlot());
                    }

                    @Override
                    public int getMaxStackSize() {
                        return getPackageSlotMaxStackSize(getContainerSlot());
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
                packageSlotPos = new Pos2i(slotX, slotY);
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
                    packageSlotPos = new Pos2i(slotX, slotY);
                    continue;
                }

                addSlot(new Slot(playerInventory, slot, slotX, slotY));
            }
        }
    }

    // --

    public ItemAndStack<PaybackPackageItem> getPackage() {
        return packageStack;
    }

    protected @NotNull PackageContents getPackageContentsFromItem() {
        return PackageContents.of(getPackage().getItemStack());
    }

    public Payback getPayback() {
        return payback;
    }

    public Pos2i getPackageSlotPos() {
        return packageSlotPos;
    }

    public boolean canPack() {
        return getPayback().matches(packageContainer);
    }

    // --

    public boolean mayPlaceIntoPackageSlot(ItemStack stack, int slot) {
        if (slot > getPayback().items().size()) {
            return false;
        }
        return getPayback().items().get(slot).matches(stack);
    }

    public int getPackageSlotMaxStackSize(int slot) {
        return getPayback().items().get(slot).getCount();
    }

    // --

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getItem();

        if (index < PackageContents.SLOTS) {
            if (!moveItemStackTo(clickedStack, PackageContents.SLOTS, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (index < slots.size()) {
            if (!moveItemStackTo(clickedStack, 0, PackageContents.SLOTS, false))
                return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;

        if (buttonId == PACK_BUTTON_ID && canPack()) {
            getPackage().set(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.of(packageContainer));
            packed = true;

            player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS,
                  1f, player.level().getRandom().nextFloat() * 0.3f + 0.85f);

            return true;
        }

        return false;
    }

    @Override
    public void removed(@NotNull Player player) {
        if (player instanceof ServerPlayer serverPlayer && !packed) {
            for (int slot = 0; slot < packageContainer.getContainerSize(); slot++) {
                ItemStack stack = packageContainer.getItem(slot);
                if (!stack.isEmpty()) {
                    serverPlayer.drop(stack, true);
                }
            }
        }

        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof PaybackPackageItem;
    }
}