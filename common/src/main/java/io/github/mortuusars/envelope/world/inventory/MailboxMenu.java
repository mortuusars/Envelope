package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.MailboxMenuMailS2CP;
import io.github.mortuusars.envelope.world.block.MailboxBlock;
import io.github.mortuusars.envelope.world.block.MailboxBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MailboxMenu extends AbstractContainerMenu {
    public static final int REFRESH_MAIL_BUTTON_ID = 0;

    protected final Inventory playerInventory;
    protected final BlockPos mailboxPos;
    protected List<ItemStack> mail;
    protected final MailboxBlockEntity blockEntity;
    protected final String address;
    @Nullable
    protected ItemStack recentlySentMail = null;
    protected long recentlySentAt = 0L;
    protected boolean hasNewMail;

    protected MailboxMenu(@Nullable MenuType<?> menuType, int id, Inventory playerInventory, BlockPos mailboxPos, List<ItemStack> mail) {
        super(menuType, id);
        this.playerInventory = playerInventory;
        this.mailboxPos = mailboxPos;
        this.mail = new ArrayList<>(mail.reversed());

        if (!(playerInventory.player.level().getBlockEntity(mailboxPos) instanceof MailboxBlockEntity be)) {
            throw new IllegalStateException("MailboxBlockEntity is not available at " + mailboxPos);
        }

        this.blockEntity = be;
        this.address = blockEntity.getAddress();

        Player player = playerInventory.player;

        addSlot(new Slot(new SimpleContainer(1), 0, 181, 60) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && stack.get(Envelope.DataComponents.MAIL_RECIPIENT) != null;
            }

            @Override
            public void set(ItemStack stack) {
                if (stack.isEmpty()) return;
                if (!blockEntity.sendMail(stack, player)) {
                    if (player.level().isClientSide) {
                        player.level().playSound(player, player, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1, 1);
                    }
                } else {
                    player.level().playSound(player, MailboxMenu.this.mailboxPos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 1, 1);
                    recentlySentMail = stack;
                    recentlySentAt = player.level().getGameTime();
                }
            }
        });
        addPlayerSlots(playerInventory, 8, 143);
    }

    public MailboxMenu(int id, Inventory playerInventory, BlockPos mailboxPos, List<ItemStack> mail) {
        this(Envelope.MenuTypes.MAILBOX.get(), id, playerInventory, mailboxPos, mail);
    }

    public static MailboxMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos mailboxPos = buffer.readBlockPos();
        List<ItemStack> mail = new ArrayList<>();
        int mailCount = buffer.readVarInt();
        for (int i = 0; i < mailCount; i++) {
            mail.add(ItemStack.STREAM_CODEC.decode(buffer));
        }
        return new MailboxMenu(id, inventory, mailboxPos, mail);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(mailboxPos).getBlock() instanceof MailboxBlock
                && player.distanceToSqr(Vec3.atCenterOf(mailboxPos)) <= 64.0D;
    }

    // --

    public BlockPos getMailboxPosition() {
        return mailboxPos;
    }

    public MailboxBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public String getAddress() {
        return address;
    }

    public List<ItemStack> getMail() {
        return mail;
    }

    public void setMail(List<ItemStack> mail) {
        this.mail = new ArrayList<>(mail.reversed());
        setHasNewMail(false);
    }

    public @Nullable ItemStack getRecentlySentMail() {
        return recentlySentMail;
    }

    public long getRecentlySentAt() {
        return recentlySentAt;
    }

    public int ticksSinceLastSend() {
        return (int) (Objects.requireNonNull(blockEntity.getLevel()).getGameTime() - getRecentlySentAt());
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
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getItem();
        ItemStack returnedStack = clickedStack.copy();

        if (index > 0) { // From player inventory
            if (!moveItemStackTo(clickedStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (clickedStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return returnedStack;
    }

    public boolean doMailAction(Player player, int index, Action action) {
        if (index < 0 || index >= getMail().size()) return false;

        return switch (action) {
            case PICK_UP -> pickUpMail(player, index);
            case MOVE_TO_INVENTORY -> moveMailToInventory(player, index);
            case MOVE_ALL_TO_INVENTORY -> moveAllMailToInventory(player);
            case REJECT -> rejectMail(player, index);
        };
    }

    protected boolean pickUpMail(Player player, int index) {
        if (!getCarried().isEmpty()) {
            return false;
        }

        ItemStack mail = getMail().remove(index);

        if (!player.level().isClientSide) {
            ItemStack takenMail = getBlockEntity().takeMail(mail, player);
            setCarried(takenMail);
        }

        return true;
    }

    protected boolean moveMailToInventory(Player player, int index) {
        ItemStack mail = getMail().get(index);

        if (!PlayerInventoryUtil.canAddWholeStack(player, mail)) {
            return false;
        }

        mail = getMail().remove(index);

        if (!player.level().isClientSide) {
            ItemStack takenMail = getBlockEntity().takeMail(mail, player);
            if (!takenMail.isEmpty()) {
                player.getInventory().add(takenMail);
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

    protected boolean rejectMail(Player player, int index) {
        throw new NotImplementedException("C.O.D. and rejection of C.O.D. mail is not implemented yet.");
    }

    // --

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == REFRESH_MAIL_BUTTON_ID && player instanceof ServerPlayer serverPlayer) {
            List<ItemStack> mail = getBlockEntity().getAllMail();
            setMail(mail);
            Packets.sendToClient(new MailboxMenuMailS2CP(mail), serverPlayer);
            return true;
        }
        return false;
    }

    // --

    public enum Action {
        PICK_UP,
        MOVE_TO_INVENTORY,
        MOVE_ALL_TO_INVENTORY,
        REJECT;

        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(
                ByIdMap.continuous(Action::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Action::ordinal);
    }
}
