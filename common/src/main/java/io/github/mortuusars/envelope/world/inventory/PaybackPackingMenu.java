package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.inventory.slot.PreviewSlot;
import io.github.mortuusars.envelope.world.inventory.slot.RequestedItemSlot;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.RequestedPayback;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department.PaybackSubject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class PaybackPackingMenu extends PackingMenu {
    protected RequestedPayback requestedPayback;
    protected ItemStack paybackSubject;

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
        this.paybackSubject = Optional.ofNullable(getBoxStack().get(Envelope.DataComponents.PAYBACK_SUBJECT))
              .map(PaybackSubject::mail)
              .orElse(ItemStack.EMPTY);
        this.requestedPayback = Objects.requireNonNull(paybackSubject.get(Envelope.DataComponents.MAIL_REQUESTED_PAYBACK));
        super.init();
        addSlot(new PreviewSlot(paybackSubject, 0, 21, 42));
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

    public RequestedPayback getPayback() {
        return requestedPayback;
    }

    public ItemStack getPaybackSubject() {
        return paybackSubject;
    }

    @Override
    public boolean canPack() {
        return super.canPack() && getPayback().matches(getPackageContainer());
    }

    @Override
    protected ItemStack createPackingResult() {
        ItemStack stack = getBoxStack().transmuteCopy(Envelope.Items.PAYBACK_PACKAGE.get());
        Mail.setRecipient(stack, Mail.getSenderOrUnknown(paybackSubject));
        Mail.setSender(stack, null);
        return stack;
    }
}