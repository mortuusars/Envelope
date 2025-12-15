package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.inventory.slot.PreviewSlot;
import io.github.mortuusars.envelope.world.inventory.slot.RequestedItemSlot;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.item.component.StoredItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PaybackPackingMenu extends PackingMenu {
    protected Payback payback;
    protected StoredItemStack paybackSubject;

    protected PaybackPackingMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId, playerInventory, hand);
    }

    public PaybackPackingMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PAYBACK_PACKAGE.get(), containerId, playerInventory, hand);
    }

    public static PaybackPackingMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PaybackPackingMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    @Override
    protected void init() {
        this.paybackSubject = getBoxStack().getOrDefault(Envelope.DataComponents.PAYBACK_SUBJECT, StoredItemStack.EMPTY);
        this.payback = Objects.requireNonNull(paybackSubject.getForReading().get(Envelope.DataComponents.PAYBACK));
        super.init();
        addSlot(new PreviewSlot(paybackSubject.getForReading(), 0, 21, 42));
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

                Slot slot = getPayback().getRequestedItem(index)
                      .map(requestedItem -> (Slot) new RequestedItemSlot(getPackageContainer(), index, x, y, requestedItem))
                      .orElseGet(() -> new DisabledSlot(getPackageContainer(), index, x, y));

                addSlot(slot);
            }
        }
    }

    // --

    public Payback getPayback() {
        return payback;
    }

    public StoredItemStack getPaybackSubject() {
        return paybackSubject;
    }

    @Override
    public boolean canPack() {
        return super.canPack() && getPayback().matches(getPackageContainer());
    }

    @Override
    protected ItemStack createPackingResult() {
        ItemStack stack = getBoxStack().transmuteCopy(Envelope.Items.PAYBACK_PACKAGE.get());
        stack.remove(Envelope.DataComponents.SENDER);
        stack.set(Envelope.DataComponents.RECIPIENT, getPaybackSubject().getForReading().get(Envelope.DataComponents.SENDER));
        return stack;
    }
}