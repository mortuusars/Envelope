package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.mailbox.Inbox;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MailboxMenu extends AbstractContainerMenu {
    public static final int ADDRESS_BUTTON_ID = 0;
    public static final int REFRESH_MAIL_BUTTON_ID = 1;

    protected final DataSlot hasDefault = DataSlot.standalone();
    protected final DataSlot isDefault = DataSlot.standalone();

    protected final Inventory playerInventory;
    protected final Player player;
    protected final BlockPos pos;
    protected final Address.Block address;
    protected final MailboxBlockEntity blockEntity;

    protected Inbox inbox;
    protected List<ItemStack> mail;
    protected boolean hasNewMail;

    protected MailboxMenu(@Nullable MenuType<?> menuType, int id, Inventory playerInventory,
                          BlockPos pos, Address.Block address, Inbox inbox) {
        super(menuType, id);
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        this.pos = pos;
        this.address = address;
        if (!(playerInventory.player.level().getBlockEntity(pos) instanceof MailboxBlockEntity be)) {
            throw new IllegalStateException("MailboxBlockEntity is not available at " + pos);
        }
        this.blockEntity = be;
        setInbox(inbox);

        addSlot(new Slot(be, MailboxBlockEntity.SLOT_FOOD, 201, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return be.canPlaceItem(MailboxBlockEntity.SLOT_FOOD, stack);
            }
        });
        addSlot(new Slot(be, MailboxBlockEntity.SLOT_MAIL, 222, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return be.canPlaceItem(MailboxBlockEntity.SLOT_MAIL, stack);
            }
        });
        addPlayerSlots(playerInventory, 140, 88);
        addDataSlot(hasDefault);
        addDataSlot(isDefault);
        updateHasDefault();
        updateIsDefault();
    }

    public MailboxMenu(int id, Inventory playerInventory, BlockPos pos, Address.Block address, Inbox inbox) {
        this(Envelope.MenuTypes.PIGEONHOLE.get(), id, playerInventory, pos, address, inbox);
    }

    public static MailboxMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos mailboxPos = buffer.readBlockPos();
        Address.Block address = Address.Block.STREAM_CODEC.decode(buffer);
        Inbox inbox = Inbox.STREAM_CODEC.decode(buffer);
        return new MailboxMenu(id, inventory, mailboxPos, address, inbox);
    }

    @Override
    public boolean stillValid(Player player) {
        return getBlockEntity().stillValid(player)
              // Close menu if address changes. Easier than resyncing it to the client.
              && getBlockEntity().getAddress().equals(address);
    }

    // --

    public BlockPos getBlockPosition() {
        return pos;
    }

    public MailboxBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public Address getAddress() {
        return address;
    }

    public boolean hasDefaultAddress() {
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.serverLevel().getEnvelopeMailService().getPlayers()
                  .getDefaultAddressOf(player)
                  .isPresent();
        }
        return hasDefault.get() == 1;
    }

    protected void updateHasDefault() {
        if (player instanceof ServerPlayer) {
            hasDefault.set(hasDefaultAddress() ? 1 : 0);
        }
    }

    public boolean isDefaultAddress() {
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.serverLevel().getEnvelopeMailService().getPlayers()
                  .getDefaultAddressOf(player)
                  .map(address::equals)
                  .orElse(false);
        }
        return isDefault.get() == 1;
    }

    protected void updateIsDefault() {
        if (player instanceof ServerPlayer) {
            isDefault.set(isDefaultAddress() ? 1 : 0);
        }
    }

    public List<ItemStack> getMail() {
        return mail;
    }

    public void setInbox(Inbox inbox) {
        this.inbox = inbox;
        this.mail = new ArrayList<>(inbox.mail().reversed());
        for (int i = 0; i < 40; i++) {
            ItemStack stack = new ItemStack(ThreadLocalRandom.current().nextBoolean() ? Envelope.Items.LETTER.get() : Envelope.Items.PACKAGE.get());
            stack.set(Envelope.DataComponents.MAIL_SENDER, new Address.Player("" + (i + 1)));
            mail.add(stack);
        }
        setHasNewMail(false);
    }

    public boolean hasNewMail() {
        return hasNewMail;
    }

    public void setHasNewMail(boolean hasNewMail) {
        this.hasNewMail = hasNewMail;
    }

    // --

    protected void addPlayerSlots(Inventory playerInventory, int x, int y) {
        // Hotbar
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(playerInventory, slot, x + slot * 18, y + 58));
        }

        // Inventory
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, (column + row * 9) + 9, x + column * 18, y + row * 18));
            }
        }
    }

    // --

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack clickedStack = slot.getItem();
        ItemStack returnedStack = clickedStack.copy();

        if (index < MailboxBlockEntity.REGULAR_SLOTS) {
            if (!moveItemStackTo(clickedStack, MailboxBlockEntity.REGULAR_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < slots.size()) {
            if (!moveItemStackTo(clickedStack, 0, MailboxBlockEntity.REGULAR_SLOTS, false))
                return ItemStack.EMPTY;
        }

        if (clickedStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return returnedStack;
    }

    public boolean doMailAction(Player player, int index, MailAction action) {
        if (index < 0 || index >= getMail().size()) return false;

        return switch (action) {
            case PICK_UP -> pickUpMail(player, index);
            case MOVE_TO_INVENTORY -> moveMailToInventory(player, index);
            case MOVE_ALL_TO_INVENTORY -> moveAllMailToInventory(player);
        };
    }

    protected boolean pickUpMail(Player player, int index) {
        if (!getCarried().isEmpty()) {
            return false;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            setCarried(extractMail(serverLevel, index));
        }

        return true;
    }

    protected boolean moveMailToInventory(Player player, int index) {
        ItemStack mail = getMail().get(index).copy();

        if (!PlayerInventoryUtil.canAddWholeStack(player, mail)) {
            return false;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            ItemStack taken = extractMail(serverLevel, index);
            if (!taken.isEmpty()) {
                player.getInventory().add(taken);
            }
        }

        return true;
    }

    protected boolean moveAllMailToInventory(Player player) {
        boolean movedSomething = false;
        while (!getMail().isEmpty()) {
            if (!moveMailToInventory(player, 0)) {
                return movedSomething;
            }
            movedSomething = true;
        }
        return true;
    }

    protected ItemStack extractMail(ServerLevel level, int index) {
//        return Optional.ofNullable(NewMail.getId(getMail().get(index)))
//              .map(id -> getBlockEntity().extractMail(id))
//              .orElse(ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    // --

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == ADDRESS_BUTTON_ID && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.serverLevel().getEnvelopeMailService().getPlayers().setDefaultAddress(player, address);
            updateHasDefault();
            updateIsDefault();
            return true;
        }

        if (id == REFRESH_MAIL_BUTTON_ID && player instanceof ServerPlayer serverPlayer) {
//            Inbox inbox = getBlockEntity().getInbox();
//            setInbox(inbox);
//            Packets.sendToClient(new PigeonholeMenuSetInboxS2CP(inbox), serverPlayer);
            return true;
        }

        return false;
    }

    public static List<ServerPlayer> playersWithMenu(ServerLevel level, @Nullable Address.Block address) {
        return level.players().stream()
              .filter(pl -> pl.containerMenu instanceof MailboxMenu menu && menu.getAddress().equals(address))
              .toList();
    }

    // --

    public enum MailAction {
        PICK_UP,
        MOVE_TO_INVENTORY,
        MOVE_ALL_TO_INVENTORY;

        public static final StreamCodec<ByteBuf, MailAction> STREAM_CODEC = ByteBufCodecs.idMapper(
              ByIdMap.continuous(MailAction::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), MailAction::ordinal);
    }
}
