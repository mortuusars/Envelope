package io.github.mortuusars.envelope.world.inventory;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.mail.Mailboxes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MailboxAddressMenu extends AbstractContainerMenu {
    public static final int MAX_NAME_LENGTH = AnvilMenu.MAX_NAME_LENGTH;
    public static final int APPLY_BUTTON_ID = 0;

    // We need BlockPlaceContext on the client to have block place effects (like sounds),
    // but sending it over the network is cumbersome,
    // so it's easier to just store it as the item is used and then get it when screen is created.
    @Nullable
    public static BlockPlaceContext STORED_CLIENT_BLOCK_PLACE_CONTEXT;

    protected final Inventory playerInventory;
    protected final ItemStack mailbox;
    protected final BlockPos pos;
    protected final BlockPlaceContext context;

    protected final DataSlot canConfirm = DataSlot.standalone();
    protected String address;

    public MailboxAddressMenu(int containerId, Inventory playerInventory, ItemStack mailbox, BlockPos pos, BlockPlaceContext context) {
        super(Envelope.MenuTypes.MAILBOX_ADDRESS.get(), containerId);
        this.playerInventory = playerInventory;
        this.mailbox = mailbox;
        this.pos = pos;
        Preconditions.checkNotNull(context);
        this.context = context;

        addDataSlot(canConfirm);

        SimpleContainer container = new SimpleContainer(mailbox);
        addSlot(new Slot(container, 0, 4, 16) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
            @Override
            public boolean mayPickup(Player player) { return false; }
            @Override
            public boolean isHighlightable() { return false; }
        });
    }

    public static MailboxAddressMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        @Nullable BlockPlaceContext lastClientContext = STORED_CLIENT_BLOCK_PLACE_CONTEXT;
        STORED_CLIENT_BLOCK_PLACE_CONTEXT = null;
        return new MailboxAddressMenu(containerId, playerInventory, ItemStack.STREAM_CODEC.decode(buffer), buffer.readBlockPos(), lastClientContext);
    }

    public Player getPlayer() {
        return playerInventory.player;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return context != null && context.canPlace();
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
        if (setAddress(address) && !Mailboxes.get(level.getServer()).exists(address)) {
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
            if (!(mailbox.getItem() instanceof BlockItem blockItem)) {
                Envelope.LOGGER.error("Cannot place the mailbox: item is not BlockItem. {}", mailbox);
                return false;
            }

            blockItem.place(context);
            if (player.level() instanceof ServerLevel
                    && getPlayer().level().getBlockEntity(context.getClickedPos()) instanceof MailboxBlockEntity mailboxBlockEntity) {
                    mailboxBlockEntity.setAddress(address);
            }

            return true;
        }
        return super.clickMenuButton(player, id);
    }
}
