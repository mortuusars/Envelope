package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Pos2i;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.PackageItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PackageMenu extends AbstractContainerMenu {
    public static final int PACK_BUTTON_ID = 0;

    protected final Player player;
    protected final InteractionHand hand;
    protected final int packageSlot;
    protected final ItemAndStack<PackageItem> packageStack;
    protected final boolean canPack;

    protected final SimpleContainer packageContainer;

    protected Pos2i packageSlotPos = new Pos2i(-999, -999);

    protected boolean packed = false;

    protected PackageMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.packageSlot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : playerInventory.selected;
        this.packageStack = new ItemAndStack<>(playerInventory.getItem(packageSlot));
        this.canPack = packageStack.map(PackageItem::canPack);

        List<ItemStack> items = new ArrayList<>(PackageContents.of(packageStack.getItemStack()).copyItems());
        while (items.size() < PackageContents.SLOTS) {
            items.add(ItemStack.EMPTY);
        }

        packageContainer = new SimpleContainer(items.toArray(ItemStack[]::new));

        addPackageSlots();
        addPlayerSlots(playerInventory, 8, 96, packageSlot);
    }

    public PackageMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PACKAGE.get(), containerId, playerInventory, hand);
    }

    public static PackageMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PackageMenu(id, inventory, buffer.readEnum(InteractionHand.class));
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
                        return getPackage().map((i, s) -> i.canInsert(stack));
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

    public ItemAndStack<PackageItem> getPackage() {
        return packageStack;
    }

    protected @NotNull PackageContents getPackageContentsFromItem() {
        return PackageContents.of(getPackage().getItemStack());
    }

    public Pos2i getPackageSlotPos() {
        return packageSlotPos;
    }

    public boolean canPack() {
        return canPack;
    }

    public boolean needsPacking() {
        if (packed) {
            return false;
        }

        PackageContents contents = PackageContents.of(packageContainer);
        return !contents.isEmpty() && !contents.equals(getPackageContentsFromItem());
    }

    public boolean isContainerEmpty() {
        return packageContainer.isEmpty();
    }

    public boolean isPackageDestroyedOnClose() {
        return !canPack() && (needsPacking() || isContainerEmpty());
    }

    // --

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getItem();

        if (index < PackageContents.SLOTS) {
            if (!moveItemStackTo(clickedStack, PackageContents.SLOTS, slots.size(), true))
                return ItemStack.EMPTY;
        }
        else if (index < slots.size()) {
            if (!moveItemStackTo(clickedStack, 0, PackageContents.SLOTS, false))
                return ItemStack.EMPTY;
        }

        packageContainer.setChanged();

        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;

        if (buttonId == PACK_BUTTON_ID && getPackage().map(PackageItem::canPack)) {
            getPackage().set(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.of(packageContainer));
            getPackage().set(Envelope.DataComponents.PACKAGE_TIMES_PACKED,
                    getPackage().getOrDefault(Envelope.DataComponents.PACKAGE_TIMES_PACKED, 0) + 1);
            packed = true;

            player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS,
                    1f, player.level().getRandom().nextFloat() * 0.3f + 0.85f);

            return true;
        }

        return false;
    }

    @Override
    public void removed(@NotNull Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!PackageContents.of(packageContainer).equals(getPackageContentsFromItem())) {
                for (ItemStack stack : PackageContents.of(packageContainer).copyItems()) {
                    serverPlayer.drop(stack, true);
                }
                getPackage().remove(Envelope.DataComponents.PACKAGE_CONTENTS);
            }

            if (getPackageContentsFromItem().isEmpty()
                    && getPackage().map(PackageItem::shouldBeDestroyedWhenEmpty)) {
                PackageItem packageItem = getPackage().getItem();
                ItemStack stack = getPackage().getItemStack();
                packageItem.unpack(stack).forEach(itemStack -> serverPlayer.drop(itemStack, false));
                stack.setCount(0);
                serverPlayer.serverLevel().playSound(null, serverPlayer, Envelope.SoundEvents.PAPER_TEAR.get(), SoundSource.PLAYERS, 1, 1);
            }
        }

        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof PackageItem;
    }
}