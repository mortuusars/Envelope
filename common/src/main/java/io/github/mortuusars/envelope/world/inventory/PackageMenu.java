package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.slot.PickupOnlySlot;
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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PackageMenu extends AbstractInHandContainerMenu {
    private final PackageContents initialContents;

    protected PackageMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory,
                          InteractionHand hand) {
        super(menuType, containerId, inventory, hand);
        this.initialContents = PackageContents.from(getItemInHand());
    }

    public PackageMenu(int containerId, Inventory inventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PACKAGE.get(), containerId, inventory, hand);
    }

    public static PackageMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PackageMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public PackageContents getInitialContents() {
        return initialContents;
    }

    public boolean isDestroyedOnClose() {
        return getContainer().isEmpty() || !getInitialContents().equals(PackageContents.createFrom(getContainer()));
    }

    // --

    @Override
    protected Container createContainer() {
        List<ItemStack> items = new ArrayList<>(PackageContents.from(getItemInHand()).copyItems());
        while (items.size() < PackageContents.SLOTS) {
            items.add(ItemStack.EMPTY);
        }

        return new SimpleContainer(items.stream().limit(PackageContents.SLOTS).toArray(ItemStack[]::new));
    }

    @Override
    protected void addContainerSlots() {
        int packageSlotsX = 62;
        int packageSlotsY = 33;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int index = column + row * 3;
                int x = packageSlotsX + column * 18;
                int y = packageSlotsY + row * 18;

                addSlot(new PickupOnlySlot(getContainer(), index, x, y));
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index >= getContainer().getContainerSize() && index < slots.size()) {
            // Prevent inserting into package.
            // Otherwise, if items are the same, they will be inserted even if Slot#mayPlace is false.
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    @Override
    public void removed(@NotNull Player player) {
        if (isDestroyedOnClose()) {
            Containers.dropContents(player.level(), player.blockPosition(), getContainer());
            getContainer().clearContent();

            ItemStack stack = getItemInHand();
            onDestroyed(player, stack);
            stack.shrink(1);

            player.level().playSound(player, player, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1, 1);
        }

        super.removed(player);
    }

    protected void onDestroyed(@NotNull Player player, ItemStack stack) {
        if (stack.getItem() instanceof PackageItem item) {
            item.onDestroyed(stack, player.level(), player.position());
        }
    }
}