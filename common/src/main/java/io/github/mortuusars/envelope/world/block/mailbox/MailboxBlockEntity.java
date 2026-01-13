package io.github.mortuusars.envelope.world.block.mailbox;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.SimpleBlockAddressGenerator;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MailboxBlockEntity extends BaseContainerBlockEntity {
    public static final int REGULAR_SLOTS = 2;
    public static final int SLOT_FOOD = 0;
    public static final int SLOT_MAIL = 1;
    public static final int SLOT_INBOX = 2;

    private final InboxContainer inboxContainer = new InboxContainer(512, Collections.emptyList()) {
        @Override
        public void setChanged() {
            MailboxBlockEntity.this.setChanged();
        }
    };
    private NonNullList<ItemStack> items = NonNullList.withSize(REGULAR_SLOTS, ItemStack.EMPTY);
    private @Nullable Address.Block address;
    private @Nullable UUID owner;
    private @Nullable UUID inboxId;

    private boolean loaded = false;

    protected MailboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public MailboxBlockEntity(BlockPos pos, BlockState blockState) {
        this(Envelope.BlockEntityTypes.MAILBOX.get(), pos, blockState);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return address != null
              ? getAddress().getName()
              : Component.translatable("container.envelope.mailbox");
    }

    // -- Address

    public @NotNull Address.Block getAddress() {
        return Preconditions.checkNotNull(address,
              "Address of mailbox at [" + getBlockPos().toShortString() + "] was not set.");
    }

    public void setAddress(@Nullable Address.Block address) {
        @Nullable Address.Block currentAddress = this.address;
        this.address = address;

        if (getLevel() instanceof ServerLevel serverLevel) {
            address = Objects.requireNonNullElseGet(address, () -> generateRandomAddress(serverLevel));
            address = MailService.of(serverLevel).mailboxes().correctOrRegisterIfNeeded(address, getBlockPos());

            if (!address.equals(currentAddress)) {
                this.address = address;
                setChanged();
                // Syncs address to the client:
                serverLevel.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), MailboxBlock.UPDATE_ALL);
            }
        }
    }

    protected void applyAddress() {
        setAddress(this.address);
    }

    protected @NotNull Address.Block generateRandomAddress(ServerLevel level) {
        return new SimpleBlockAddressGenerator(MailService.of(level).getKnownAddresses(), 50).generate(level.getRandom());
    }

    // -- Owner

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public Optional<Player> getOwnerPlayer() {
        if (owner == null || level == null) return Optional.empty();
        for (Player player : level.players()) {
            if (player.getUUID().equals(owner)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    // Container

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FOOD) return stack.is(Envelope.Tags.Items.PIGEON_FOOD);
        if (slot == SLOT_MAIL) return isSendable(stack);
        return false;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot == SLOT_INBOX;
    }

    public boolean isSendable(ItemStack stack) {
        return stack.is(Envelope.Tags.Items.MAILABLE);
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        applyAddress();
        return new MailboxMenu(containerId, inventory, getBlockPos(), new Inbox(inboxContainer.getMail()), getAddress());
    }

    // -- Events

    public void serverTick(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        if (!loaded) {
            onLoaded();
            loaded = true;
        }
    }

    protected void onLoaded() {
        applyAddress();

        if (level instanceof ServerLevel serverLevel) {

//            if (inboxId != null) {
//                Inboxes.get(serverLevel).retrieve(inboxId).ifPresent(inbox -> inboxContainer.setMail(inbox.mail()));
//            } else {
//                inboxId = UUID.randomUUID();
//                setChanged();
//            }
        }
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

//    @Override
//    public void clearRemoved() {
//        super.clearRemoved();
//        // onLoad();
//    }

//    @Override
//    public void setRemoved() {
//        super.setRemoved();
//
//        if (level instanceof ServerLevel serverLevel) {
//            if (address != null && level.getBlockState(getBlockPos()).isAir()) {
//                MailService.of(serverLevel).getMailboxManager().remove(address);
//            }
//
//            if (!inboxContainer.isEmpty()) {
//                Inboxes.get(serverLevel).store(inboxId, new Inbox(inboxContainer.getMail()));
//                inboxContainer.setMail(Collections.emptyList());
//            }
//        }
//    }

    // -- Sync

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    // -- Loading/Saving

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
        setAddress(tag.contains("address", Tag.TAG_STRING) ? new Address.Block(tag.getString("address")) : null);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        inboxId = tag.hasUUID("inbox_id") ? tag.getUUID("inbox_id") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        if (address != null) tag.putString("address", address.id());
        if (owner != null) tag.putUUID("owner", owner);
        if (inboxId != null) tag.putUUID("inbox_id", inboxId);
    }
}
