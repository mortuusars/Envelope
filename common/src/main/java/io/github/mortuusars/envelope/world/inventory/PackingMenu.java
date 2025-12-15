package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Pos2i;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.item.PackingBox;
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

public class PackingMenu extends AbstractContainerMenu {
    public static final int PACK_BUTTON_ID = 0;

    private final Player player;
    private final InteractionHand hand;
    private final int packageSlot;
    private final ItemStack packageStack;
    private final PackingBox packingBox;
    private final PackageContents initialPackageContents;
    private final boolean canPack;
    private final SimpleContainer packageContainer;

    protected boolean packed = false;

    protected PackingMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.packageSlot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : playerInventory.selected;
        this.packageStack = playerInventory.getItem(packageSlot);
        this.packingBox = (PackingBox) packageStack.getItem();
        this.initialPackageContents = PackageContents.of(packageStack);
        this.canPack = packingBox.canPack(packageStack);
        this.packageContainer = createPackageContainer();
        init();
    }

    public PackingMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PACKAGE.get(), containerId, playerInventory, hand);
    }

    public static PackingMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PackingMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    protected void init() {
        addPackageSlots();
        addPlayerSlots(player.getInventory(), 8, 96, packageSlot);
    }

    // --

    protected @NotNull SimpleContainer createPackageContainer() {
        final SimpleContainer packageContainer;
        List<ItemStack> items = new ArrayList<>(PackageContents.of(packageStack).copyItems());
        while (items.size() < PackageContents.SLOTS) {
            items.add(ItemStack.EMPTY);
        }

        packageContainer = new SimpleContainer(items.toArray(ItemStack[]::new));
        return packageContainer;
    }

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
                        return getPackage().canInsert(stack);
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

    public int getPackageSlot() {
        return packageSlot;
    }

    public ItemStack getBoxStack() {
        return packageStack;
    }

    public PackingBox getPackage() {
        return packingBox;
    }

    public PackageContents getInitialPackageContents() {
        return initialPackageContents;
    }

    protected @NotNull PackageContents getPackageContentsFromItem() {
        return PackageContents.of(getBoxStack());
    }

    public boolean canPack() {
        return canPack;
    }

    public SimpleContainer getPackageContainer() {
        return packageContainer;
    }

    public boolean needsPacking() {
        if (packed) {
            return false;
        }

        PackageContents contents = PackageContents.of(packageContainer);
        return !contents.isEmpty();
    }

    public boolean isContainerEmpty() {
        return packageContainer.isEmpty();
    }

    public boolean isPackageDestroyedOnClose() {
        return !canPack && (needsPacking() || isContainerEmpty());
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
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (buttonId == PACK_BUTTON_ID && canPack()) {
            pack(player);
            return true;
        }

        return false;
    }

    protected void pack(@NotNull Player player) {
        ItemStack stack = createPackingResult();
        stack.set(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.of(packageContainer));
        stack.set(Envelope.DataComponents.PACKAGE_TIMES_PACKED,
              stack.getOrDefault(Envelope.DataComponents.PACKAGE_TIMES_PACKED, 0) + 1);

        player.setItemInHand(getHand(), stack);
        player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS,
              1f, player.level().getRandom().nextFloat() * 0.3f + 0.85f);
        packed = true;
    }

    protected ItemStack createPackingResult() {
        ItemStack stack = getBoxStack().transmuteCopy(Envelope.Items.PACKAGE.get());
        stack.remove(Envelope.DataComponents.SENDER);
        stack.remove(Envelope.DataComponents.RECIPIENT);
        return stack;
    }

    @Override
    public void removed(@NotNull Player player) {
        if (!packed && player instanceof ServerPlayer serverPlayer) {
            for (ItemStack stack : PackageContents.of(packageContainer).copyItems()) {
                player.drop(stack, true);
            }
            getBoxStack().remove(Envelope.DataComponents.PACKAGE_CONTENTS);

            if (getPackage().shouldBeDestroyedWhenEmpty(getBoxStack())) {
                player.getItemInHand(hand).shrink(1);
                serverPlayer.serverLevel().playSound(null, serverPlayer,
                      Envelope.SoundEvents.PAPER_TEAR.get(), SoundSource.PLAYERS, 1, 1);
            }
        }

        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof PackingBox;
    }
}