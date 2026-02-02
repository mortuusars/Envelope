package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.item.PackingBoxItem;
import io.github.mortuusars.envelope.world.item.PaperBoxItem;
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

public class NewPackingMenu extends AbstractContainerMenu {
    public static final int PACK_BUTTON_ID = 0;

    private final Player player;
    private final InteractionHand hand;
    private final int usedSlot;
//    private final ItemStack packageStack;
//    private final PackingBox packingBox;
    private final SimpleContainer container;

    protected boolean packed = false;

    protected NewPackingMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.usedSlot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : playerInventory.selected;
//        this.packageStack = playerInventory.getItem(usedSlot);
//        this.packingBox = (PackingBox) packageStack.getItem();
        this.container = new SimpleContainer(PackageContents.SLOTS);
        init();
    }

    public NewPackingMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PACKING.get(), containerId, playerInventory, hand);
    }

    public static NewPackingMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new NewPackingMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    protected void init() {
        addPackageSlots();
        addPlayerSlots(player.getInventory(), 8, 96, getUsedSlot());
    }

    protected void addPackageSlots() {
        int packageSlotsX = 62;
        int packageSlotsY = 33;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;
                int x = packageSlotsX + column * 18;
                int y = packageSlotsY + row * 18;
                addSlot(new Slot(container, index, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return canInsert(stack);
                    }
                });
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
//
//    public PackageContents getInitialPackageContents() {
//        return initialPackageContents;
//    }
//
//    protected @NotNull PackageContents getPackageContentsFromItem() {
//        return PackageContents.from(getBoxStack());
//    }

    public SimpleContainer getContainer() {
        return container;
    }

    public boolean canInsert(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems() && !stack.is(Envelope.Tags.Items.CANNOT_BE_PACKAGED);
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

        container.setChanged();

        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (buttonId == PACK_BUTTON_ID) {
            ItemStack result = new ItemStack(Envelope.Items.PACKAGE.get());
            result.set(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.createFrom(container));

            player.getItemInHand(hand).shrink(1);
            if (!player.addItem(result)) {
                player.drop(result, false);
            }

            player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS,
                  1f, player.level().getRandom().nextFloat() * 0.3f + 0.85f);
            packed = true;
            return true;
        }

        return false;
    }

    @Override
    public void removed(@NotNull Player player) {
        if (!packed /*&& player instanceof ServerPlayer serverPlayer*/) {
            Containers.dropContents(player.level(), player.blockPosition(), getContainer());
        }

        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof PaperBoxItem;
    }
}