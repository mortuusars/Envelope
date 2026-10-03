package io.github.mortuusars.envelope.world.block.mailbox;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.packet.clientbound.ClientboundMailboxHasNewMailPacket;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.CourierBat;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.SimpleBlockAddressGenerator;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import io.github.mortuusars.mortaar.Platform;
import net.minecraft.Util;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
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

    protected static final Logger LOGGER = LogUtils.getLogger();

    protected NonNullList<ItemStack> items = NonNullList.withSize(REGULAR_SLOTS, ItemStack.EMPTY);
    protected @NotNull UUID inboxId = UUID.randomUUID();
    protected @Nullable BlockAddress address;
    protected @Nullable UUID owner;

    protected @NotNull List<ItemStack> mail = new ArrayList<>();
    protected boolean loaded = false;
    protected boolean blockRemoved = false;
    protected int batSummonCooldown;
    protected int batSummonAttempt;

    protected boolean deliveredWithPigeon;
    protected boolean deliveredWithBat;

    protected MailboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public MailboxBlockEntity(BlockPos pos, BlockState blockState) {
        this(Envelope.BlockEntityTypes.MAILBOX.get(), pos, blockState);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return address != null
              ? address.getComponent()
              : Component.translatable("container.envelope.mailbox");
    }

    // -- Address

    public @NotNull BlockAddress getAddress() {
        return Preconditions.checkNotNull(address,
              "Address of mailbox at [" + getBlockPos().toShortString() + "] was not set.");
    }

    public void setAddress(@Nullable BlockAddress address) {
        @Nullable BlockAddress currentAddress = this.address;
        this.address = address;

        if (getLevel() instanceof ServerLevel serverLevel && MailService.operatesIn(serverLevel)) {
            address = Objects.requireNonNullElseGet(address, () -> generateRandomAddress(serverLevel));
            address = MailService.of(serverLevel).getMailboxes().correctOrRegisterIfNeeded(address, getBlockPos());

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

    protected @NotNull BlockAddress generateRandomAddress(ServerLevel level) {
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

    public ItemStack getFood() {
        return getItem(SLOT_FOOD);
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
        if (slot == SLOT_FOOD) return stack.is(Envelope.Tags.Items.PIGEON_FOOD)
              || (Config.Server.BAT_ENABLED.get() && Config.Server.BAT_SUMMONED_TO_MAILBOX.get() && stack.is(Envelope.Tags.Items.BAT_FOOD));
        if (slot == SLOT_MAIL) return isSendable(stack);
        return false;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot >= INBOX_SLOT
              && target.hasAnyMatching(ItemStack::isEmpty); // Prevents hoppers from removing the item and placing it back
    }

    public boolean isSendable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Envelope.Tags.Items.MAILABLE) && stack.has(Envelope.DataComponents.MAIL_ADDRESS_TAG);
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
            if (getOwner() == null) {
                setOwner(player.getUUID());
            }

            Platform.openMenu(serverPlayer, this, buffer -> {
                buffer.writeBlockPos(getBlockPos());
                BlockAddress.STREAM_CODEC.encode(buffer, getAddress());
                ItemStack.LIST_STREAM_CODEC.encode(buffer, getAllMail());
            });
            playSound(SoundEvents.BARREL_OPEN, 0.6f, 1.1f);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        mail.clear();
    }

    // -- Delivery

    public boolean tryStartDelivery(PhysicalCourier courier) {
        //noinspection PatternVariableHidesField
        if (!(getLevel() instanceof ServerLevel level) || !MailService.operatesIn(level)) {
            return false;
        }

        if (courier.isDelivering()) return false;
        ItemStack mailStack = getItem(SLOT_MAIL);
        if (!isSendable(mailStack)) return false;

        applyAddress();

        ItemStack mail = Mail.removePreviousDeliveryData(mailStack.copyWithCount(1));

        MailService.of(level).getDeliveryManager()
              .start(courier, Delivery.draft()
                    .deliver(mail)
                    .from(getAddress())
                    .to(Mail.getRecipientOrUnknown(mail))
                    .owner(getOwner()));

        removeItem(SLOT_MAIL, 1);
        removeItem(SLOT_FOOD, 1);

        Vec3 pos = courier.position();
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 10, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.NEUTRAL, 1f, 1.3f);

        if (courier.asCourierEntity().getType().equals(Envelope.EntityTypes.PIGEON.get())) {
            deliveredWithPigeon = true;
        } else if (courier.asCourierEntity().getType().equals(Envelope.EntityTypes.COURIER_BAT.get())) {
            deliveredWithBat = true;
        }

        if (deliveredWithPigeon && deliveredWithBat) {
            getOwnerPlayer().ifPresent(player -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    Envelope.CriteriaTriggers.DELIVER_WITH_PIGEON_AND_BAT.get().trigger(serverPlayer);
                }
            });
        }

        return true;
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
    public void onMailInserted(ItemStack mail) {
        playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1, 1);
    }

    @Override
    public void onMailAdded(ItemStack mail) {
        if (level instanceof ServerLevel serverLevel) {
            MailboxMenu.playersWithMenu(serverLevel, getAddress())
                  .forEach(ClientboundMailboxHasNewMailPacket.INSTANCE::sendToClient);
        }
    }

    @Override
    public void onMailRemoved(int slot, ItemStack mail) {
        if (level instanceof ServerLevel serverLevel) {
            MailboxMenu.executeForPlayersWithMenu(serverLevel, getAddress(), (player, menu) -> menu.onMailRemoved(slot));
        }
    }

    @Override
    public void onInboxChanged() {
        setChanged();
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    // As inbox can be quite large in size, we cannot store it as regular block entity data.
    // So we use dedicated inbox storage for that, and load/unload each time block entity is being loaded/unloaded.

    public void loadInbox() {
        if (level instanceof ServerLevel serverLevel && !inboxId.equals(Util.NIL_UUID)) {
            mail = InboxStorage.get(serverLevel).remove(inboxId)
                  .map(Inbox::getAllMail)
                  .orElseGet(ArrayList::new);
        } else {
            mail = new ArrayList<>();
        }
    }

    public void unloadInbox() {
        if (level instanceof ServerLevel serverLevel && address != null && !inboxId.equals(Util.NIL_UUID)) {
            InboxStorage.get(serverLevel).put(inboxId, this);
        }
        mail.clear();
    }

    // -- Events

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!loaded) {
            onLoaded();
            loaded = true;
        }

        maybeSummonBat(level, pos);
    }

    protected boolean maybeSummonBat(ServerLevel level, BlockPos pos) {
        batSummonCooldown--;

        if (batSummonCooldown > 0
              || !Config.Server.BAT_ENABLED.get()
              || !Config.Server.BAT_SUMMONED_TO_MAILBOX.get()
              || (!Config.Server.BAT_MAILBOX_SUMMON_IGNORES_DOMOBSPAWNING.get() && !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
              || !level.isNight()
              || level.isRaining()
              || level.isThundering()
              || !isAvailableForPickup()
              || !getFood().is(Envelope.Tags.Items.BAT_FOOD)
              || level.getRandom().nextInt(Math.max(1, Config.Server.BAT_SUMMON_INTERVAL.get() - batSummonAttempt++)) != 0) {
            return false;
        }

        BlockPos spawnPos = Position.ascendTowards(level, pos, Optional.empty(),
              Config.Server.DELIVERY_ASCEND_DISTANCE.get(), level.getRandom().nextInt());
        @Nullable CourierBat bat = Envelope.EntityTypes.COURIER_BAT.get().spawn(level, spawnPos, MobSpawnType.NATURAL);
        if (bat != null) {
            bat.setSpawnPos(spawnPos);
            bat.getMailboxHandler().setTargetPos(pos);
            bat.onAppeared(level);
            level.playSound(null, spawnPos, SoundEvents.APPLY_EFFECT_BAD_OMEN, SoundSource.BLOCKS, 1f, 1f);
            batSummonCooldown = Config.Server.BAT_SUMMON_COOLDOWN.get();
            batSummonAttempt = 0;
            return true;
        }

        return false;
    }

    protected void onLoaded() {
        applyAddress();
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    public void onBlockRemoved(Level level, BlockPos pos, BlockState state, BlockState newState) {
        blockRemoved = true;
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        clearMail();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        loadInbox();
        blockRemoved = false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!blockRemoved) {
            unloadInbox();
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateBlockStateIfNeeded();
    }

    private void updateBlockStateIfNeeded() {
        if (!blockRemoved && !isRemoved() && level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            boolean isOpen = state.getValue(MailboxBlock.OPEN);
            boolean hasMail = state.getValue(MailboxBlock.HAS_MAIL);

            boolean shouldBeOpen = isAvailableForPickup();
            boolean shouldHaveMail = !getAllMail().isEmpty();

            if (isOpen != shouldBeOpen || hasMail != shouldHaveMail) {
                serverLevel.setBlockAndUpdate(getBlockPos(), state
                      .setValue(MailboxBlock.OPEN, shouldBeOpen)
                      .setValue(MailboxBlock.HAS_MAIL, shouldHaveMail));

                if (isOpen != shouldBeOpen) {
                    playSound(shouldBeOpen ? SoundEvents.CHERRY_WOOD_TRAPDOOR_OPEN : SoundEvents.CHERRY_WOOD_TRAPDOOR_CLOSE,
                          0.75f,
                          shouldBeOpen ? 1f : 0.75f);
                }
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        if (address != null) tag.putString("address", address.getString());
        if (owner != null) tag.putUUID("owner", owner);
        if (!inboxId.equals(Util.NIL_UUID)) tag.putUUID("inbox_id", inboxId);
        if (batSummonCooldown > 0) tag.putInt("bat_summon_cooldown", batSummonCooldown);
        if (batSummonAttempt > 0) tag.putInt("bat_summon_attempt", batSummonCooldown);
        if (deliveredWithPigeon) tag.putBoolean("delivered_with_pigeon", true);
        if (deliveredWithBat) tag.putBoolean("delivered_with_bat", true);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
        setAddress(tag.contains("address", Tag.TAG_STRING) ? new BlockAddress(tag.getString("address")) : null);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        inboxId = tag.hasUUID("inbox_id") ? tag.getUUID("inbox_id") : UUID.randomUUID();
        batSummonCooldown = tag.getInt("bat_summon_cooldown");
        batSummonAttempt = tag.getInt("bat_summon_attempt");
        deliveredWithPigeon = tag.getBoolean("delivered_with_pigeon");
        deliveredWithBat = tag.getBoolean("delivered_with_bat");
    }

    // -- Util

    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, getBlockPos(), soundEvent, SoundSource.BLOCKS, volume, pitch);
        }
    }
}
