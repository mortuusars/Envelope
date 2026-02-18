package io.github.mortuusars.envelope.world.inventory;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.GhostSlot;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.PaybackDuration;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PaybackTagMenu extends AbstractInHandContainerMenu {
    public static final int CONFIRM_BUTTON_ID = 0;
    public static final int DURATION_BUTTON_ID = 1;

    public static final int COUNT_START_BUTTON_ID = 100;
    public static final int INCREASE_COUNT_START_BUTTON_ID = COUNT_START_BUTTON_ID;
    public static final int INCREASE_COUNT_FAST_START_BUTTON_ID = INCREASE_COUNT_START_BUTTON_ID + PaybackRequest.SLOTS;
    public static final int DECREASE_COUNT_START_BUTTON_ID = INCREASE_COUNT_FAST_START_BUTTON_ID + PaybackRequest.SLOTS;
    public static final int DECREASE_COUNT_FAST_START_BUTTON_ID = DECREASE_COUNT_START_BUTTON_ID + PaybackRequest.SLOTS;

    protected final ItemStack targetPreview;

    protected DataSlot durationDataSlot = DataSlot.standalone();

    protected PaybackTagMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, InteractionHand hand) {
        super(menuType, containerId, inventory, hand);
        addDataSlot(durationDataSlot);

        targetPreview = getItemInHand().copy();

        @Nullable PaybackRequest existingRequest = getItemInHand().get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS);
        PaybackDuration duration = existingRequest != null
              ? existingRequest.duration()
              : PaybackDuration.MEDIUM;
        durationDataSlot.set(duration.ordinal());
    }

    public PaybackTagMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PAYBACK_TAG.get(), containerId, playerInventory, hand);
    }

    public static PaybackTagMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PaybackTagMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public ItemStack getTargetPreview() {
        return targetPreview;
    }

    // --

    @Override
    protected void init() {
        playerSlotsX = 13;
        playerSlotsY = 79;
        super.init();
    }

    @Override
    protected Container createContainer() {
        SimpleContainer container = new SimpleContainer(PaybackRequest.SLOTS);

        if (getItemInHand().get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS) instanceof PaybackRequest request) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                Optional<StackIngredient> ingredient = request.getRequestedItem(slot);
                if (ingredient.isPresent()) {
                    container.setItem(slot, ingredient.get().stacks()[0]);
                }
            }
        }

        container.addListener(c -> {
            if (getPlayer().level().isClientSide()) {
                updateTargetPreview();
            }
        });

        return container;
    }

    @Override
    protected void addContainerSlots() {
        int slotsX = 67;
        int slotsY = 23;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;
                int x = slotsX + column * 18;
                int y = slotsY + row * 18;
                addSlot(new GhostSlot(getContainer(), index, x, y, PaybackRequest::isValid) {
                    @Override
                    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
                        super.setByPlayer(newStack, oldStack);
                        if (getPlayer().level().isClientSide()
                              && (newStack.getItem() != oldStack.getItem() || newStack.getCount() != oldStack.getCount())) {
                            getPlayer().playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.15f, 2);
                        }
                    }
                });
            }
        }
    }

    // --

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getItem();

        if (index < PackageContents.SLOTS) {
            slots.get(index).setByPlayer(ItemStack.EMPTY);
        } else if (index < slots.size()) {
            for (int i = 0; i < PaybackRequest.SLOTS; i++) {
                Slot paybackSlot = slots.get(i);
                if (paybackSlot.getItem().isEmpty() && paybackSlot.mayPlace(clickedStack)) {
                    paybackSlot.setByPlayer(clickedStack.copy());
                    break;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (buttonId == CONFIRM_BUTTON_ID) {
            if (getContainer().isEmpty()) {
                getItemInHand().remove(Envelope.DataComponents.PAYBACK_TAG_CONTENTS);
            } else {
                PaybackRequest request = createRequest();
                getItemInHand().set(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, request);
            }

            player.level().playSound(player, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS,
                  1f, player.level().getRandom().nextFloat() * 0.3f + 0.85f);
            player.setItemInHand(getHand(), getItemInHand());
            player.swing(getHand());
            return true;
        }

        if (buttonId >= DURATION_BUTTON_ID && buttonId < DURATION_BUTTON_ID + PaybackDuration.values().length) {
            durationDataSlot.set(buttonId - DURATION_BUTTON_ID);
            if (player.level().isClientSide()) {
                updateTargetPreview();
            }
            return true;
        }

        if (buttonId >= COUNT_START_BUTTON_ID && buttonId < DECREASE_COUNT_FAST_START_BUTTON_ID + PaybackRequest.SLOTS) {
            int id = buttonId - COUNT_START_BUTTON_ID;
            int slotIndex = id % PaybackRequest.SLOTS;

            boolean decrease = id >= PaybackRequest.SLOTS * 2;
            boolean fast = id % (PaybackRequest.SLOTS * 2) >= PaybackRequest.SLOTS;

            int change = (fast ? 10 : 1);
            if (decrease) {
                change *= -1;
            }

            if (slots.get(slotIndex) instanceof GhostSlot slot) {
                slot.setCount(Mth.clamp(slot.getItem().getCount() + change, 1, slot.getItem().getMaxStackSize()));
            }

            return true;
        }

        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= PaybackRequest.SLOTS) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        Slot paybackSlot = this.slots.get(slotId);

        if (getCarried().isEmpty() || !paybackSlot.mayPlace(getCarried())) {
            paybackSlot.setByPlayer(ItemStack.EMPTY);
            return;
        }

        ItemStack result = getCarried().copy();

        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (ItemStack.isSameItemSameComponents(paybackSlot.getItem(), result)) {
                result.setCount(paybackSlot.getItem().getCount() + 1);
            } else {
                result.setCount(1);
            }
        }
        paybackSlot.setByPlayer(result);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        // Prevent dragging (QUICK_CRAFT) from working on payback slots. It's difficult to make it work correctly.
        return !(slot instanceof GhostSlot);
    }

    // --

    public PaybackDuration getPaybackDuration() {
        return PaybackDuration.BY_ID.apply(durationDataSlot.get());
    }

    protected PaybackRequest createRequest() {
        Container compactedContainer = ContainerUtils.compact(getContainer(), PaybackRequest.SLOTS);
        List<StackIngredient> ingredients = ContainerUtils.toList(compactedContainer, PaybackRequest.SLOTS).stream()
              .filter(i -> !i.isEmpty())
              .map(StackIngredient::createFromStack)
              .toList();
        return PaybackRequest.createOrDefault(ingredients, getPaybackDuration());
    }

    protected void updateTargetPreview() {
        getTargetPreview().set(Envelope.DataComponents.PAYBACK_TAG_CONTENTS,
              !getContainer().isEmpty()
                    ? createRequest()
                    : null);
    }
}
