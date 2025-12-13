package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.item.component.StoredItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PaybackPackingMenu extends PackingMenu {
    protected final Payback payback;
    protected final StoredItemStack paybackItem;

    protected PaybackPackingMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId, playerInventory, hand);
        this.paybackItem = getBoxStack().getOrDefault(Envelope.DataComponents.PAYBACK_SUBJECT, StoredItemStack.EMPTY);
        this.payback = Objects.requireNonNull(paybackItem.getForReading().get(Envelope.DataComponents.PAYBACK));

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
            public boolean isFake() {
                return true;
            }
        });
    }

    public PaybackPackingMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PAYBACK_PACKAGE.get(), containerId, playerInventory, hand);
    }

    public static PaybackPackingMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PaybackPackingMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    // --

    @Override
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

                    @Override
                    public boolean isActive() {
                        return getContainerSlot() < getPayback().items().size();
                    }
                });
            }
        }
    }

    // --

    public Payback getPayback() {
        return payback;
    }

    public StoredItemStack getPaybackSubject() {
        return paybackItem;
    }

    @Override
    public boolean canPack() {
        return super.canPack() && getPayback().matches(packageContainer);
    }

    // --

    public boolean mayPlaceIntoPackageSlot(ItemStack stack, int slot) {
        if (slot >= getPayback().items().size()) {
            return false;
        }
        // Ignore counts, otherwise we will need to place stack of exactly the same count as requested:
        RequestedItem requestedItem = getPayback().items().get(slot);
        return requestedItem.typeMatches(stack) && requestedItem.componentsMatch(stack);
    }

    public int getPackageSlotMaxStackSize(int slot) {
        if (slot >= getPayback().items().size()) {
            return 0;
        }
        return getPayback().items().get(slot).count();
    }

    @Override
    protected ItemStack createPackingResult() {
        ItemStack stack = getBoxStack().transmuteCopy(Envelope.Items.PAYBACK_PACKAGE.get());
        stack.remove(Envelope.DataComponents.SENDER);
        stack.set(Envelope.DataComponents.RECIPIENT, getPaybackSubject().getForReading().get(Envelope.DataComponents.SENDER));
        return stack;
    }

    // --

//    @Override
//    public void removed(@NotNull Player player) {
//        if (player instanceof ServerPlayer serverPlayer && !packed) {
//            for (int slot = 0; slot < packageContainer.getContainerSize(); slot++) {
//                ItemStack stack = packageContainer.getItem(slot);
//                if (!stack.isEmpty()) {
//                    serverPlayer.drop(stack, true);
//                }
//            }
//        }
//
//        super.removed(player);
//    }
}