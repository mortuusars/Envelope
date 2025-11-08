package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeHasNewMailS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeMenuMailRemovedS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeMenuMailS2CP;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.world.block.occupiable.Occupant;
import io.github.mortuusars.envelope.world.block.occupiable.Occupiable;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeData;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.item.component.MailId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
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
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class PigeonholeBlockEntity extends BaseContainerBlockEntity implements Occupiable {
    public static final int SLOTS = 2;
    public static final int SLOT_FOOD = 0;
    public static final int SLOT_MAIL = 1;
    public static final int SLOT_INBOX = 2;

    protected List<Occupant.Mutable> occupants = new ArrayList<>();
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    protected @Nullable Address.Pigeonhole address;
    protected @Nullable PigeonholeData data;
    protected @Nullable UUID owner;
    protected boolean updatedAfterLoading;

    protected PigeonholeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PigeonholeBlockEntity(BlockPos pos, BlockState blockState) {
        this(Envelope.BlockEntityTypes.PIGEONHOLE.get(), pos, blockState);
    }

    // -- Address

    public Optional<Address.Pigeonhole> getAddress() {
        return Optional.ofNullable(address);
    }

    public void setAddress(@NotNull Address.Pigeonhole address) {
        if (Objects.equals(this.address, address)) {
            return;
        }

        ifAddressed((level, address_, data) ->
              dropOrReturnAllMail());

        this.address = address;
        this.data = null; // Force re-query
        setChanged();
    }

    // -- Owner

    public Optional<Player> getOwner() {
        if (owner == null || level == null) return Optional.empty();
        for (Player player : level.players()) {
            if (player.getUUID().equals(owner)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    // -- Mail

    public Optional<PigeonholeData> getData() {
        if (address == null) return Optional.empty();

        if (level instanceof ServerLevel serverLevel) {
            if (data == null || !data.stillValid()) {
                data = serverLevel.getEnvelopeContext().getPigeonholeManager().getOrRegister(address, getBlockPos());
                address = data.getAddress(); // Update address to correct and registered value
            }
        }

        return Optional.ofNullable(data);
    }

    public void insertMail(ItemStack mail) {
        ifAddressed((level, address, data) -> {
            data.insertMail(mail);
            level.playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 1, 1);
            PigeonholeMenu.playersWithMenu(level, address).forEach(player ->
                  Packets.sendToClient(PigeonholeHasNewMailS2CP.INSTANCE, player));
            setChanged();
        });
    }

    public ItemStack extractMail(MailId id) {
        return mapAddressed((level, address, data) -> {
            ItemStack mail = Mail.stripInboxData(data.extractMail(id));
            PigeonholeMenu.playersWithMenu(level, address).forEach(player ->
                  Packets.sendToClient(new PigeonholeMenuMailRemovedS2CP(id), player));
            setChanged();
            return mail;
        }).orElse(ItemStack.EMPTY);
    }

    public void dropOrReturnAllMail() {
        ifAddressed((level, address, data) -> {
            List<ItemStack> allMail = data.extractAllMail();

            NonNullList<ItemStack> itemsToDrop = allMail.stream()
                  .filter(this::isExtractable)
                  .map(Mail::stripInboxData)
                  .collect(Collectors.toCollection(NonNullList::create));

            Containers.dropContents(level, getBlockPos(), itemsToDrop);

            PigeonholeMenu.playersWithMenu(level, address).forEach(player ->
                  Packets.sendToClient(new PigeonholeMenuMailS2CP(Collections.emptyList()), player));

            setChanged();
        });
    }

    // -- Events

    public static void serverTick(Level level, BlockPos pos, BlockState state, PigeonholeBlockEntity blockEntity) {
        if (level instanceof ServerLevel serverLevel) {
            blockEntity.serverTick(serverLevel, pos, state);
        }
    }

    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!updatedAfterLoading) {
            setChanged();
            updatedAfterLoading = true;
        }

        if (!getOccupants().isEmpty()
              && (level.getGameTime() + pos.hashCode()) % 20 == 0
              && CampfireBlock.isSmokeyPos(level, pos)) {
            releaseAllOccupants(level, pos, state, ReleaseReason.EMERGENCY);
        }

        tickOccupants(level, pos, state);

        // Make some nearby pigeons prioritize this pigeonhole to pick up and deliver mail
        if (getOccupants().isEmpty() && !getItem(SLOT_MAIL).isEmpty() && !getItem(SLOT_FOOD).isEmpty()) {
            for (Pigeon nearbyPigeon : level.getEntitiesOfClass(Pigeon.class, new AABB(getBlockPos()).inflate(16))) {
                nearbyPigeon.getPigeonholeHandler().setCurrentPos(getBlockPos());
                break;
            }
        }
    }

    public void onBlockRemoved() {
        items.stream().skip(1).forEach(stack -> {
            Containers.dropItemStack(getLevelOrThrow(), getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), stack);
        });

        ifAddressed((level, address, data) -> {
            dropOrReturnAllMail();
            level.getEnvelopeContext().getPigeonholeManager().remove(address);
            this.data = null;
            this.address = null;
        });
    }

    @Override
    public void setChanged() {
        if (isFireNearby()) {
            releaseAllOccupants(getLevel(), getBlockPos(), getBlockState(), ReleaseReason.EMERGENCY);
        }

        updateBlockStateIfNeeded();
        super.setChanged();

        if (level instanceof ServerLevel serverLevel && !PigeonholeMenu.playersWithMenu(serverLevel, address).isEmpty()) {
            serverLevel.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    protected void updateBlockStateIfNeeded() {
        if (!isRemoved() && level instanceof ServerLevel serverLevel) {
            BlockState state = getBlockState();
            BlockState newState = state
                  .setValue(PigeonholeBlock.HAS_ADDRESS, address != null)
                  .setValue(PigeonholeBlock.HAS_MAIL, !getItem(PigeonholeBlockEntity.SLOT_MAIL).isEmpty());

            if (state != newState) {
                serverLevel.setBlockAndUpdate(getBlockPos(), newState);
            }
        }
    }

    // Container

    @Override
    public int getContainerSize() {
        return address != null ? SLOTS + 1 : 0; // +1 for inbox
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public ItemStack getFirstAvailableMailToExtract() {
        return mapAddressed((level, address, data) -> {
            for (ItemStack mail : data.getMail()) {
                if (isExtractable(mail)) {
                    return mail;
                }
            }
            return ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot == SLOT_INBOX) {
            return getFirstAvailableMailToExtract();
        }
        return super.getItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == SLOT_INBOX) return;
        super.setItem(slot, stack);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot == SLOT_INBOX) {
            return MailId.of(getFirstAvailableMailToExtract())
                  .map(this::extractMail)
                  .orElse(ItemStack.EMPTY);
        }
        return super.removeItem(slot, amount);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (address == null) return false;
        if (slot == SLOT_FOOD) return stack.is(Envelope.Tags.Items.PIGEON_FOOD);
        if (slot == SLOT_MAIL) return isSendable(stack);
        return false;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot == SLOT_INBOX;
    }

    public boolean isSendable(ItemStack stack) {
        return address != null
              && !stack.isEmpty()
              && stack.get(Envelope.DataComponents.MAIL_RECIPIENT) instanceof Address recipient
              && !recipient.equals(address);
    }

    public boolean isExtractable(ItemStack stack) {
        //TODO: C.O.D, etc
        return true;
    }

    // -- Menu

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return mapAddressed((level, address, data) ->
              new PigeonholeMenu(id, inventory, getBlockPos(), data.getMail(), getAddress().orElseThrow()))
              .orElseThrow(() -> new IllegalStateException("Pigeonhole does not have an address, or data is not available."));
    }

    public boolean openMenu(ServerPlayer player) {
        if (address == null) {
            Envelope.LOGGER.error("Cannot open Pigeonhole: it doesn't have an address.");
            return false;
        }

        // Forces sync of be data to the client
        player.level().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

        PlatformHelper.openMenu(player, this, buffer -> {
            List<ItemStack> mail = mapAddressed((level, address, data) -> data.getMail()).orElse(Collections.emptyList());
            buffer.writeBlockPos(getBlockPos());
            buffer.writeVarInt(mail.size());
            for (ItemStack item : mail) {
                ItemStack.STREAM_CODEC.encode(buffer, item);
            }
            Address.Pigeonhole.STREAM_CODEC.encode(buffer, address);
        });

        return true;
    }

    // -- Occupiable

    @Override
    public boolean canBeOccupiedBy(Entity entity) {
        return entity.getType().is(Envelope.Tags.EntityTypes.PIGEONHOLE_INHABITORS);
    }

    @Override
    public List<Occupant.Mutable> getOccupants() {
        return occupants;
    }

    @Override
    public SoundEvent getOccupantEnterSound(Entity entity) {
        return SoundEvents.BEEHIVE_ENTER;
    }

    @Override
    public SoundEvent getOccupantExitSound(Entity entity) {
        return SoundEvents.BEEHIVE_EXIT;
    }

    @Override
    public SoundEvent getOccupantWorkSound() {
        return Envelope.SoundEvents.PIGEON_AMBIENT.get();
    }

    @Override
    public int getMinimumTicksInsideForOccupant(Entity entity) {
        return entity instanceof Pigeon
              ? Config.Server.PIGEON_MIN_TICKS_INSIDE_PIGEONHOLE.get()
              : Occupiable.super.getMinimumTicksInsideForOccupant(entity);
    }

    @Override
    public void onOccupantReleased(Level level, Entity entity, ReleaseReason reason) {
        if (reason == ReleaseReason.EMERGENCY) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (entity instanceof Pigeon pigeon) {
            pigeon.releasedFromPigeonhole(getBlockPos(), getBlockState(), reason); // Calling before mail sending to set home pos etc

            ItemStack mail = getItem(SLOT_MAIL);
            if (isSendable(mail) && !getItem(SLOT_FOOD).isEmpty() && !pigeon.isDelivering()) {
                mail = mail.copyWithCount(1);
                mail.set(Envelope.DataComponents.MAIL_SENDER, address);
                if (!mail.has(Envelope.DataComponents.MAIL_RECIPIENT)) {
                    mail.set(Envelope.DataComponents.MAIL_RECIPIENT, Address.UNKNOWN);
                }
                pigeon.startDelivery(Delivery.create(serverLevel, mail));

                getItem(SLOT_FOOD).shrink(1);
                if (getItem(SLOT_FOOD).isEmpty()) {
                    setItem(SLOT_FOOD, ItemStack.EMPTY);
                }

                getItem(SLOT_MAIL).shrink(1);
                if (getItem(SLOT_MAIL).isEmpty()) {
                    setItem(SLOT_MAIL, ItemStack.EMPTY);
                }
            }
        }

        float wasteChance = getWasteIncreaseChanceOnRelease(entity);
        if (getBlockState().getBlock() instanceof PigeonholeBlock block && level.random.nextFloat() < wasteChance) {
            block.addWaste(level, getBlockPos(), getBlockState());
            setChanged();
        }
    }

    @Override
    public void onOccupantsChanged() {
        setChanged();
    }

    @Override
    public String getSerializedOccupantsName() {
        return "pigeons";
    }

    @Override
    public void cleanupEntityTag(CompoundTag tag) {
        Pigeon.IGNORED_TAGS.forEach(tag::remove);
    }

    public boolean isFireNearby() {
        if (level == null) return false;

        for (BlockPos blockPos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
            if (level.getBlockState(blockPos).getBlock() instanceof FireBlock) {
                return true;
            }
        }

        return false;
    }

    protected float getWasteIncreaseChanceOnRelease(Entity releasedEntity) {
        return releasedEntity instanceof Courier courier && courier.isDelivering() ? 1f : 0.2f;
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

    // -- Component

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        this.occupants.clear();
        List<Occupant> occupants = componentInput.getOrDefault(Envelope.DataComponents.PIGEONS, List.of());
        occupants.forEach(o -> getOccupants().add(o.toMutable()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(Envelope.DataComponents.PIGEONS, getImmutableOccupants());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(getSerializedOccupantsName());
    }

    // -- Save/Load

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
        address = tag.contains("address", Tag.TAG_STRING) ? new Address.Pigeonhole(tag.getString("address")) : null;
        data = null; // Force re-query
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        loadOccupiable(tag, registries);

        updateBlockStateIfNeeded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        if (address != null) tag.putString("address", address.id());
        if (owner != null) tag.putUUID("owner", owner);
        saveOccupiable(tag, registries);
    }

    // --

    @Override
    protected @NotNull Component getDefaultName() {
        return getAddress().map(Address::getDisplayName).orElse(Component.translatable("container.envelope.pigeonhole"));
    }

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }

    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, getBlockPos(), soundEvent, SoundSource.BLOCKS, volume, pitch);
        }
    }

    public void ifAddressed(TriConsumer<ServerLevel, Address.Pigeonhole, PigeonholeData> consumer) {
        if (address != null && level instanceof ServerLevel serverLevel) {
            getData().ifPresent(data -> consumer.accept(serverLevel, address, data));
        }
    }

    public void ifAddressedOrElse(TriConsumer<ServerLevel, Address.Pigeonhole, PigeonholeData> consumer, Runnable orElse) {
        if (address != null && level instanceof ServerLevel serverLevel) {
            getData().ifPresentOrElse(data -> consumer.accept(serverLevel, address, data), orElse);
        } else {
            orElse.run();
        }
    }

    public <T> Optional<T> mapAddressed(TriFunction<ServerLevel, Address.Pigeonhole, PigeonholeData, T> function) {
        if (address != null && level instanceof ServerLevel serverLevel) {
            return getData().map(data -> function.apply(serverLevel, address, data));
        }
        return Optional.empty();
    }
}
