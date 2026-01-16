package io.github.mortuusars.envelope.world.block.mailbox;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.MailboxHasNewMailS2CP;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import io.github.mortuusars.envelope.world.item.component.NewMail;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.SimpleBlockAddressGenerator;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class MailboxBlockEntity extends BaseContainerBlockEntity implements Inbox {
    public static final int REGULAR_SLOTS = 2;
    public static final int SLOT_FOOD = 0;
    public static final int SLOT_MAIL = 1;
    public static final int INBOX_SLOT = 2;
    private static final Logger LOGGER = LogUtils.getLogger();

    private NonNullList<ItemStack> items = NonNullList.withSize(REGULAR_SLOTS, ItemStack.EMPTY);
    private @NotNull UUID inboxId = UUID.randomUUID();
    private @Nullable Address.Block address;
    private @Nullable UUID owner;

    private @NotNull List<ItemStack> mail = new ArrayList<>();
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

    // -- Container

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
        return REGULAR_SLOTS + getAllMail().size();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot >= INBOX_SLOT) {
            return getMail(slot - INBOX_SLOT);
        }
        return super.getItem(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot >= INBOX_SLOT) {
            return removeMail(slot - INBOX_SLOT);
        }
        return super.removeItem(slot, amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot >= INBOX_SLOT) {
            return removeMailNoUpdate(slot - INBOX_SLOT);
        }
        return super.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= INBOX_SLOT) {
            addMail(slot - INBOX_SLOT, stack);
            return;
        }
        super.setItem(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FOOD) return stack.is(Envelope.Tags.Items.PIGEON_FOOD);
        if (slot == SLOT_MAIL) return isSendable(stack);
        return false;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot >= INBOX_SLOT
              && target.hasAnyMatching(ItemStack::isEmpty); // Prevents hoppers from removing the item and placing it back
    }

    public boolean isSendable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Envelope.Tags.Items.MAILABLE) && stack.has(Envelope.DataComponents.MAIL_RECIPIENT);
    }

    public boolean isAvailableForPickup() {
        if (level == null) return false;
        return !getItem(SLOT_FOOD).isEmpty() && isSendable(getItem(SLOT_MAIL));
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        applyAddress();
        return new MailboxMenu(containerId, inventory, getBlockPos(), getAddress(), getAllMail());
    }

    public void openMenu(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlatformHelper.openMenu(serverPlayer, this, buffer -> {
                buffer.writeBlockPos(getBlockPos());
                Address.Block.STREAM_CODEC.encode(buffer, getAddress());
                ItemStack.LIST_STREAM_CODEC.encode(buffer, getAllMail());
            });
            playSound(SoundEvents.BARREL_OPEN, 0.6f, 1.1f);
        }
    }

    // -- Inbox

    @Override
    public int getInboxCapacity() {
        return 512;
    }

    @Override
    public @NotNull List<ItemStack> getAllMail() {
        return mail;
    }

    @Override
    public void onMailAdded(ItemStack mail) {
        Inbox.super.onMailAdded(mail);
        if (level instanceof ServerLevel serverLevel) {
            MailboxMenu.playersWithMenu(serverLevel, getAddress())
                  .forEach(player -> Packets.sendToClient(MailboxHasNewMailS2CP.INSTANCE, player));
        }
    }

    @Override
    public void onMailRemoved(int slot, ItemStack mail) {
        Inbox.super.onMailRemoved(slot, mail);
        if (level instanceof ServerLevel serverLevel) {
            Optional.ofNullable(NewMail.getId(mail)).ifPresent(id -> {
                MailboxMenu.executeForPlayersWithMenu(serverLevel, getAddress(),
                      (player, menu) -> menu.onMailRemoved(id));
            });
        }
    }

    @Override
    public void onInboxChanged() {
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
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
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    public void onBlockRemoved(Level level, BlockPos pos, BlockState state, BlockState newState) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        clearMail();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel serverLevel) {
            this.mail = Inboxes.get(serverLevel).retrieve(inboxId)
                  .map(UnloadedInbox::getAllMail)
                  .orElseGet(ArrayList::new);
            if (Bugger.isEnabled() && !getAllMail().isEmpty()) {
                LOGGER.info("Loaded inbox with size [{}] of mailbox {}@[{}]", getAllMail().size(), address, getBlockPos().toShortString());
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel && address != null) {
            Inboxes.get(serverLevel).store(inboxId, this);
            if (Bugger.isEnabled() && !getAllMail().isEmpty()) {
                LOGGER.info("Stored inbox with size [{}] of mailbox {}@[{}]", mail.size(), address, getBlockPos().toShortString());
            }
            this.mail = new ArrayList<>();
        }
        mail.clear();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!isRemoved() && level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            boolean isOpen = state.getValue(MailboxBlock.OPEN);
            boolean shouldBeOpen = isAvailableForPickup();
            if (isOpen != shouldBeOpen) {
                serverLevel.setBlockAndUpdate(getBlockPos(), state.setValue(MailboxBlock.OPEN, shouldBeOpen));
                playSound(shouldBeOpen ? SoundEvents.CHERRY_WOOD_TRAPDOOR_OPEN : SoundEvents.CHERRY_WOOD_TRAPDOOR_CLOSE,
                      0.75f,
                      shouldBeOpen ? 1f : 0.75f);
            }
        }
    }

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
        inboxId = tag.hasUUID("inbox_id") ? tag.getUUID("inbox_id") : UUID.randomUUID();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        if (address != null) tag.putString("address", address.id());
        if (owner != null) tag.putUUID("owner", owner);
        tag.putUUID("inbox_id", inboxId);
    }

    // -- Util

    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, getBlockPos(), soundEvent, SoundSource.BLOCKS, volume, pitch);
        }
    }

    public boolean tryStartDelivery(Pigeon pigeon) {
        ServerLevel level = ((ServerLevel) pigeon.level());

        if (pigeon.isDelivering()) return false;
        ItemStack mailStack = getItem(SLOT_MAIL);
        if (!isSendable(mailStack)) return false;

        applyAddress();

        mailStack = mailStack.copyWithCount(1);

        return MailService.of(level).getDeliveryManager()
              .start(pigeon, Delivery.builder()
                    .deliver(new Mail(mailStack))
                    .from(getAddress())
                    .to(NewMail.getRecipient(mailStack))
                    .owner(getOwner()))
              .getValue()
              .map(delivery -> {
                  removeItem(SLOT_MAIL, 1);
                  removeItem(SLOT_FOOD, 1);

                  Vec3 pos = pigeon.position();
                  level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 10, 0.3, 0.3, 0.3, 0.02);
                  level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.NEUTRAL, 1f, 1.3f);

                  return true;
              })
              .orElse(false);
    }
}
