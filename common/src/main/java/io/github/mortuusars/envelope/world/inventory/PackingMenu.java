package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PackingMenu extends AbstractInHandContainerMenu {
    public static final int PACK_BUTTON_ID = 0;

    private @Nullable Address presetAddress;
    protected boolean packed = false;

    protected PackingMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, InteractionHand hand) {
        super(menuType, containerId, playerInventory, hand);
    }

    public PackingMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        this(Envelope.MenuTypes.PACKING.get(), containerId, playerInventory, hand);
    }

    public static PackingMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PackingMenu(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    @Override
    protected Container createContainer() {
        return new SimpleContainer(PackageContents.SLOTS);
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
                
                addSlot(createContainerSlot(index, x, y));
            }
        }
    }

    protected Slot createContainerSlot(int index, int x, int y) {
        return new Slot(getContainer(), index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PackageContents.canHold(stack);
            }
        };
    }

    // --

    public @Nullable Address getPresetAddress() {
        return presetAddress;
    }

    public void presetAddress(@Nullable Address address) {
        presetAddress = address;
    }

    public ItemStack getAddressTag() {
        return PlayerInventoryUtil.findFirstMatching(getPlayer(),
              stack -> stack.getItem() instanceof AddressTagItem);
    }

    // --

    public boolean isPacked() {
        return packed;
    }

    public boolean canPack() {
        return !getContainer().isEmpty();
    }

    protected void pack() {
        Player player = getPlayer();

        ItemStack addressTag = getAddressTag();

        if (addressTag.isEmpty()) {
            presetAddress = null;
        } else if (presetAddress != null) {
            addressTag.shrink(1);
        }

        ItemStack result = createPackingResult();

        getItemInHand().shrink(1);

        if (!player.addItem(result)) {
            player.drop(result, false);
        }

        player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS,
              1f, player.level().getRandom().nextFloat() * 0.3f + 0.85f);
        player.swing(getHand());
        player.awardStat(Envelope.Stats.PACKAGES_CREATED.get());
        packed = true;
    }

    protected @NotNull ItemStack createPackingResult() {
        ItemStack result = new ItemStack(Envelope.Items.PACKAGE.get());
        result.set(Envelope.DataComponents.PACKAGE_CONTENTS, new PackageContents(getContainer()));
        result.set(Envelope.DataComponents.MAIL_RECIPIENT, presetAddress);
        return result;
    }

    // --

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == PACK_BUTTON_ID) {
            if (canPack()) {
                pack();
            }
            return true;
        }

        return false;
    }

    @Override
    public void removed(@NotNull Player player) {
        if (!isPacked()) {
            clearContainer(player, getContainer());
        }

        super.removed(player);
    }
}