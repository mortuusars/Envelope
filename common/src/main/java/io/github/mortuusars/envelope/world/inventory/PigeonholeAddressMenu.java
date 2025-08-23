package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.world.PigeonholeNetwork;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PigeonholeAddressMenu extends AbstractContainerMenu {
    public static final int MAX_NAME_LENGTH = 30;
    public static final int APPLY_BUTTON_ID = 0;

    protected final Inventory playerInventory;
    protected final InteractionHand hand;
    protected final BlockPos pos;
    protected final boolean isRenaming;
    protected final PigeonholeBlockEntity blockEntity;

    protected final DataSlot canConfirm = DataSlot.standalone();

    protected String address;

    protected PigeonholeAddressMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                    InteractionHand hand, BlockPos pos, String suggestedAddress, boolean isRenaming) {
        super(type, containerId);
        this.playerInventory = playerInventory;
        this.hand = hand;
        this.pos = pos;
        this.isRenaming = isRenaming;
        this.blockEntity = ((PigeonholeBlockEntity) playerInventory.player.level().getBlockEntity(pos));

        this.address = suggestedAddress;

        addDataSlot(canConfirm);
    }

    public PigeonholeAddressMenu(int containerId, Inventory playerInventory,
                                 InteractionHand hand, BlockPos pos, String suggestedAddress, boolean isRenaming) {
        this(Envelope.MenuTypes.PIGEONHOLE_ADDRESS.get(), containerId, playerInventory, hand, pos, suggestedAddress, isRenaming);
    }

    public static PigeonholeAddressMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        return new PigeonholeAddressMenu(containerId, playerInventory, buffer.readEnum(InteractionHand.class),
                buffer.readBlockPos(), buffer.readUtf(), buffer.readBoolean());
    }

    public Player getPlayer() {
        return playerInventory.player;
    }

    public boolean isRenaming() {
        return isRenaming;
    }

    public String getAddress() {
        return address;
    }

    public boolean setAddress(String address) {
        String validatedString = validateAddress(address);
        if (validatedString == null || !validatedString.equals(address) || validatedString.isBlank()) {
            return false;
        }

        this.address = validatedString;
        return true;
    }

    public void setAddressAndUpdateConfirmState(ServerLevel level, String address) {
        if (setAddress(address) && !PigeonholeNetwork.get(level).exists(new Address.Pigeonhole(address))) {
            canConfirm.set(1);
        } else {
            canConfirm.set(0);
        }
    }

    @Nullable
    public String validateAddress(String address) {
        String string = StringUtil.filterText(address);
        if (string.length() <= MAX_NAME_LENGTH) {
            return string;
        }
        return null;
    }

    public boolean canConfirm() {
        return canConfirm.get() == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == APPLY_BUTTON_ID && canConfirm()) {
            BlockState state = player.level().getBlockState(pos);
            if (state.getBlock() instanceof PigeonholeBlock pigeonhole) {
                pigeonhole.applyAddress(player, state, pos, hand, address);
            }

            return true;
        }
        return super.clickMenuButton(player, id);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).is(Items.NAME_TAG)
                && player.level().getBlockState(pos).getBlock() instanceof PigeonholeBlock
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0D;
    }
}
