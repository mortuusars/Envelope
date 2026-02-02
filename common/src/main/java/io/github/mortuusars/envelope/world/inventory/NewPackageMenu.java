package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.item.PackageItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
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

import java.util.ArrayList;
import java.util.List;

public class NewPackageMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final int usedSlot;
    //    private final ItemStack packageStack;
//    private final PackingBox packingBox;
    private final PackageContents initialContents;
    private final SimpleContainer packageContainer;

    protected NewPackageMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.usedSlot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : playerInventory.selected;
//        this.packageStack = playerInventory.getItem(usedSlot);
//        this.packingBox = (PackingBox) packageStack.getItem();
        this.initialContents = PackageContents.from(playerInventory.getItem(usedSlot));
        this.packageContainer = createContainer();
        init();
    }

    public NewPackageMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.NEW_PACKAGE.get(), containerId, playerInventory, hand);
    }

    public static NewPackageMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new NewPackageMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    protected void init() {
        addPackageSlots();
        addPlayerSlots(player.getInventory(), 8, 96, getUsedSlot());
    }

    // --

    protected @NotNull SimpleContainer createContainer() {
        List<ItemStack> items = new ArrayList<>(getInitialContents().copyItems());
        while (items.size() < PackageContents.SLOTS) {
            items.add(ItemStack.EMPTY);
        }

        return new SimpleContainer(items.toArray(ItemStack[]::new));
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        super.initializeContents(stateId, items, carried);
    }

    protected void addPackageSlots() {
        int packageSlotsX = 62;
        int packageSlotsY = 33;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;
                int x = packageSlotsX + column * 18;
                int y = packageSlotsY + row * 18;

                if (packageContainer.getItem(index).isEmpty()) {
                    addSlot(new DisabledSlot(packageContainer, index, x, y));
                } else {
                    addSlot(new Slot(packageContainer, index, x, y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }
                    });
                }
            }
        }
    }

    protected void addPlayerSlots(Container inventory, int x, int y, int packageSlot) {
        // Hotbar
        for (int index = 0; index < 9; index++) {
            int slotX = x + index * 18;
            int slotY = y + 58;
            addSlot(index == packageSlot
                  ? new DisabledSlot(inventory, index, slotX, slotY)
                  : new Slot(inventory, index, slotX, slotY));
        }

        // Inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = (column + row * 9) + 9;
                int slotX = x + column * 18;
                int slotY = y + row * 18;
                addSlot(index == packageSlot
                      ? new DisabledSlot(inventory, index, slotX, slotY)
                      : new Slot(inventory, index, slotX, slotY));
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

    public int getUsedSlot() {
        return usedSlot;
    }

//    public ItemStack getBoxStack() {
//        return packageStack;
//    }
//
//    public PackingBox getPackage() {
//        return packingBox;
//    }

    public PackageContents getInitialContents() {
        return initialContents;
    }

//    protected @NotNull PackageContents getPackageContentsFromItem() {
//        return PackageContents.from(getBoxStack());
//    }

    public SimpleContainer getContainer() {
        return packageContainer;
    }

    public boolean isContainerEmpty() {
        return packageContainer.isEmpty();
    }

    public boolean isDestroyedOnClose() {
        return !getInitialContents().equals(PackageContents.createFrom(getContainer()));
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

        packageContainer.setChanged();

        return ItemStack.EMPTY;
    }

    @Override
    public void removed(@NotNull Player player) {
        if (isDestroyedOnClose()) {
            Containers.dropContents(player.level(), player.blockPosition(), packageContainer);
            packageContainer.clearContent();
            player.getItemInHand(hand).shrink(1);

            //TODO: give Paper Box back? with chance?

            player.level().playSound(player, player, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1, 1);
        }

        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof PackageItem;
    }
}