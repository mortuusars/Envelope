package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.inventory.slot.PreviewSlot;
import io.github.mortuusars.envelope.world.inventory.slot.RequestedItemSlot;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.PaybackSubject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PaybackPackingMenu extends PackingMenu {
    protected PaybackSubject subject;
    protected PaybackRequest request;

    protected PaybackPackingMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId, playerInventory, hand);
    }

    public PaybackPackingMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PAYBACK_PACKING.get(), containerId, playerInventory, hand);
    }

    public static PaybackPackingMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PaybackPackingMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    // --

    public PaybackRequest getPaybackRequest() {
        return request;
    }

    public PaybackSubject getPaybackSubject() {
        return subject;
    }

    @Override
    public boolean canPack() {
        return super.canPack() && getPaybackRequest().matches(getContainer());
    }

    // --

    @Override
    protected void init() {
        this.subject = Objects.requireNonNull(getItemInHand().get(Envelope.DataComponents.PAYBACK_SUBJECT),
              "Item in hand does not have 'envelope:payback_subject' component.");
        this.request = subject.getRequest();
        super.init();
        addSlot(new PreviewSlot(subject.mail(), 0, 19, 42));
    }

    @Override
    protected Slot createContainerSlot(int index, int x, int y) {
        return getPaybackRequest().getRequestedItem(index)
              .map(requestedItem -> (Slot) new RequestedItemSlot(getContainer(), index, x, y, requestedItem))
              .orElseGet(() -> new DisabledSlot(getContainer(), index, x, y));
    }

    @Override
    protected @NotNull ItemStack createPackingResult() {
        ItemStack result = getItemInHand().transmuteCopy(Envelope.Items.PAYBACK_PACKAGE.get());
        result.set(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.createFrom(getContainer()));
        Mail.removePreviousDeliveryData(result);
        Mail.setRecipient(result, getPaybackSubject().returnAddress());
        return result;
    }
}