package io.github.mortuusars.envelope.world.inventory;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.StoredMail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeMenuMailS2CP;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeData;
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
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PigeonholeMenu extends AbstractContainerMenu {
    public static final int ADDRESS_BUTTON_ID = 0;
    public static final int REFRESH_MAIL_BUTTON_ID = 1;

    protected final DataSlot hasDefault = DataSlot.standalone();
    protected final DataSlot isDefault = DataSlot.standalone();

    protected final Inventory playerInventory;
    protected final Player player;
    protected final BlockPos pigeonholePos;
    protected final Address.Pigeonhole address;
    protected final PigeonholeBlockEntity blockEntity;

    protected List<StoredMail> mail;
    protected boolean hasNewMail;

    protected PigeonholeMenu(@Nullable MenuType<?> menuType, int id, Inventory playerInventory,
                             BlockPos pigeonholePos, List<StoredMail> mail, Address.Pigeonhole address) {
        super(menuType, id);
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        this.pigeonholePos = pigeonholePos;
        this.address = address;
        if (!(playerInventory.player.level().getBlockEntity(pigeonholePos) instanceof PigeonholeBlockEntity be)) {
            throw new IllegalStateException("PigeonholeBlockEntity is not available at " + pigeonholePos);
        }
        this.blockEntity = be;
        this.mail = new ArrayList<>(mail.reversed());

        addSlot(new Slot(be, PigeonholeBlockEntity.SLOT_FOOD, 227, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return be.canPlaceItem(PigeonholeBlockEntity.SLOT_FOOD, stack);
            }
        });
        addSlot(new Slot(be, PigeonholeBlockEntity.SLOT_MAIL, 248, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return be.canPlaceItem(PigeonholeBlockEntity.SLOT_MAIL, stack);
            }
        });
        addPlayerSlots(playerInventory, 140, 121);
        addDataSlot(hasDefault);
        addDataSlot(isDefault);
        updateHasDefault();
        updateIsDefault();
    }

    public PigeonholeMenu(int id, Inventory playerInventory, BlockPos pigeonholePos, List<StoredMail> mail, Address.Pigeonhole address) {
        this(Envelope.MenuTypes.PIGEONHOLE.get(), id, playerInventory, pigeonholePos, mail, address);
    }

    public static PigeonholeMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos mailboxPos = buffer.readBlockPos();
        List<StoredMail> mail = new ArrayList<>();
        int mailCount = buffer.readVarInt();
        for (int i = 0; i < mailCount; i++) {
            mail.add(StoredMail.STREAM_CODEC.decode(buffer));
        }
        Address.Pigeonhole address = Address.Pigeonhole.STREAM_CODEC.decode(buffer);
        return new PigeonholeMenu(id, inventory, mailboxPos, mail, address);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(pigeonholePos)) <= 64.0D
              && player.level().getBlockEntity(getBlockPosition()) instanceof PigeonholeBlockEntity be
              // Close menu if address changes. Easier than resyncing it to the client.
              && be.getAddress().map(a -> a.equals(address)).orElse(false);
    }

    // --

    public BlockPos getBlockPosition() {
        return pigeonholePos;
    }

    public PigeonholeBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public Address getAddress() {
        return address;
    }

    public boolean hasDefaultAddress() {
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.serverLevel().getEnvelopeContext().getPlayers()
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
            return serverPlayer.serverLevel().getEnvelopeContext().getPlayers()
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

    public List<StoredMail> getMail() {
        return mail;
    }

    public void setMail(List<StoredMail> mail) {
        this.mail = new ArrayList<>(mail.reversed());
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

        if (index < PigeonholeBlockEntity.SLOTS) {
            if (!moveItemStackTo(clickedStack, PigeonholeBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < slots.size()) {
            if (!moveItemStackTo(clickedStack, 0, PigeonholeBlockEntity.SLOTS, false))
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
            case REJECT -> rejectMail(player, index);
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
        ItemStack mail = getMail().get(index).getItemCopy();

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
        StoredMail mail = getMail().get(index);
        return getBlockEntity().extractMail(mail.getId());
    }

    protected boolean rejectMail(Player player, int index) {
        throw new NotImplementedException("C.O.D. and rejection of C.O.D. mail is not implemented yet.");
    }

    // --

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == ADDRESS_BUTTON_ID && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.serverLevel().getEnvelopeContext().getPlayers().setDefaultAddress(player, address);
            updateHasDefault();
            updateIsDefault();
            return true;
        }

        if (id == REFRESH_MAIL_BUTTON_ID && player instanceof ServerPlayer serverPlayer) {
            List<StoredMail> mail = getBlockEntity().getData().map(PigeonholeData::getMail).orElse(Collections.emptyList());
            setMail(mail);
            Packets.sendToClient(new PigeonholeMenuMailS2CP(mail), serverPlayer);
            return true;
        }

        return false;
    }

    public static List<ServerPlayer> playersWithMenu(ServerLevel level, @Nullable Address.Pigeonhole address) {
        return level.players().stream()
              .filter(pl -> pl.containerMenu instanceof PigeonholeMenu menu && menu.getAddress().equals(address))
              .toList();
    }

    // --

    public enum MailAction {
        PICK_UP,
        MOVE_TO_INVENTORY,
        MOVE_ALL_TO_INVENTORY,
        REJECT;

        public static final StreamCodec<ByteBuf, MailAction> STREAM_CODEC = ByteBufCodecs.idMapper(
              ByIdMap.continuous(MailAction::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), MailAction::ordinal);
    }
}
