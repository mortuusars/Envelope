package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeHasNewMailS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeMenuMailRemovedS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeMenuMailS2CP;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.occupiable.PigeonOccupiable;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.StoredMail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.world.block.occupiable.Occupant;
import io.github.mortuusars.envelope.world.service.MailService;
import io.github.mortuusars.envelope.world.service.pigeonhole.PigeonholeData;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.mail.MailId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.*;
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

public class PigeonholeBlockEntity extends BaseContainerBlockEntity implements PigeonOccupiable {
    public static final int SLOTS = 2;
    public static final int SLOT_FOOD = 0;
    public static final int SLOT_MAIL = 1;
    public static final int SLOT_INBOX = 2;

    protected List<Occupant.Mutable> occupants = new ArrayList<>();
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    protected @Nullable Address.Block address;
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

    public Optional<Address.Block> getAddress() {
        return Optional.ofNullable(address);
    }

    public void setAddress(@NotNull Address.Block address) {
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

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
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

    // -- Mail

    public Optional<PigeonholeData> getData() {
        if (address == null) return Optional.empty();

        if (level instanceof ServerLevel serverLevel) {
            if (data == null || !data.stillValid()) {
                data = MailService.of(serverLevel).getPigeonholeManager().getOrRegister(address, getBlockPos());
                address = data.getAddress(); // Update address to correct and registered value
            }
        }

        return Optional.ofNullable(data);
    }

    public void insertMail(Mail mail) {
        ifAddressed((level, address, data) -> {
            data.insertMail(mail);
            level.playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 1, 1);
            PigeonholeMenu.playersWithMenu(level, address).forEach(player ->
                  Packets.sendToClient(PigeonholeHasNewMailS2CP.INSTANCE, player));
            setChanged();
        });
    }

    public ItemStack extractMail(MailId id) {
        return mapAddressed((level, address, data) ->
              data.extractMail(id)
                    .map(mail -> {
                        PigeonholeMenu.playersWithMenu(level, address).forEach(player -> {
                            ((PigeonholeMenu) player.containerMenu).getMail().removeIf(storedMail -> storedMail.matches(mail));
                            Packets.sendToClient(new PigeonholeMenuMailRemovedS2CP(mail.getId()), player);
                        });
                        setChanged();
                        return mail.getItem().copy();
                    })
                    .orElse(ItemStack.EMPTY))
              .orElse(ItemStack.EMPTY);
    }

    public void dropOrReturnAllMail() {
        ifAddressed((level, address, data) -> {
            List<StoredMail> allMail = data.extractAllMail();

            NonNullList<ItemStack> itemsToDrop = allMail.stream()
                  .filter(this::isExtractable)
                  .map(mail -> mail.getItem().copy())
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

        tickOccupants(level, pos, state);
    }

    @Override
    public void tickOccupants(Level level, BlockPos pos, BlockState state) {
        PigeonOccupiable.super.tickOccupants(level, pos, state);

        // Make some nearby pigeons prioritize this pigeonhole to pick up and deliver mail
        if (getOccupants().isEmpty() && !getItem(SLOT_MAIL).isEmpty() && !getItem(SLOT_FOOD).isEmpty()) {
            for (Pigeon nearbyPigeon : level.getEntitiesOfClass(Pigeon.class, new AABB(getBlockPos()).inflate(16))) {
                nearbyPigeon.getPigeonholeHandler().setCurrentPos(getBlockPos());
                break;
            }
        }
    }

    public void onBlockRemoved() {
        Containers.dropItemStack(getLevelOrThrow(), getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), getItem(SLOT_FOOD));
        Containers.dropItemStack(getLevelOrThrow(), getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), getItem(SLOT_MAIL));

        if (data != null) {
            data.invalidate();
            data = null;
        }

        address = null;
    }

    @Override
    public void setChanged() {
        if (Position.isFireNearby(level, getBlockPos())) {
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

    public StoredMail getFirstAvailableMailToExtract() {
        return mapAddressed((level, address, data) -> {
            for (StoredMail mail : data.getMail()) {
                if (isExtractable(mail)) {
                    return mail;
                }
            }
            return StoredMail.EMPTY;
        }).orElse(StoredMail.EMPTY);
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (slot == SLOT_INBOX) {
            return getFirstAvailableMailToExtract().getItem();
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
            StoredMail mail = getFirstAvailableMailToExtract();
            return mail != StoredMail.EMPTY ? extractMail(mail.getId()) : ItemStack.EMPTY;
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
        return stack.is(Envelope.Tags.Items.MAILABLE);
    }

    public boolean isExtractable(StoredMail stack) {
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
        player.level().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);

        PlatformHelper.openMenu(player, this, buffer -> {
            List<StoredMail> mail = mapAddressed((level, address, data) -> data.getMail()).orElse(Collections.emptyList());
            buffer.writeBlockPos(getBlockPos());
            buffer.writeVarInt(mail.size());
            for (StoredMail stored : mail) {
                StoredMail.STREAM_CODEC.encode(buffer, stored);
            }
            Address.Block.STREAM_CODEC.encode(buffer, address);
        });

        return true;
    }

    // -- Occupiable

    @Override
    public List<Occupant.Mutable> getOccupants() {
        return occupants;
    }

    @Override
    public void onOccupantReleased(Level level, Entity entity, ReleaseReason reason) {
        if (reason == ReleaseReason.EMERGENCY) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (entity instanceof Pigeon pigeon) {
            pigeon.releasedFromPigeonhole(getBlockPos(), getBlockState(), reason); // Calling before mail sending to set home pos etc
            tryStartDelivery(serverLevel, pigeon);
        }

        float wasteChance = getWasteIncreaseChanceOnRelease(entity);
        if (getBlockState().getBlock() instanceof PigeonholeBlock block && level.random.nextFloat() < wasteChance) {
            block.addWaste(level, getBlockPos(), getBlockState());
            setChanged();
        }
    }

    protected void tryStartDelivery(ServerLevel level, Pigeon pigeon) {
        if (this.address == null || pigeon.isDelivering()) {
            return;
        }

        ItemStack mailStack = getItem(SLOT_MAIL);
        if (mailStack.isEmpty() || !mailStack.has(Envelope.DataComponents.RECIPIENT_ADDRESS)) {
            return;
        }

        mailStack = mailStack.copyWithCount(1);

        Address recipient = mailStack.getOrDefault(Envelope.DataComponents.RECIPIENT_ADDRESS, Address.UNKNOWN);
        if (MailService.of(level).resolve(recipient).matches(this.address)) {
            return;
        }

        MailService.of(level).getDeliveryManager()
              .start(pigeon, Delivery.builder()
                    .deliver(new Mail(mailStack))
                    .from(address)
                    .to(recipient)
                    .owner(getOwner()))
              .ifPresent(delivery -> {
                  removeItem(SLOT_MAIL, 1);
                  removeItem(SLOT_FOOD, 1);
              });
    }

    @Override
    public void onOccupantsChanged() {
        setChanged();
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
        address = tag.contains("address", Tag.TAG_STRING)
              ? new Address.Block(tag.getString("address"))
              : null;
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
        return getAddress().map(Address::getName).orElse(Component.translatable("container.envelope.pigeonhole"));
    }

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }

    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, getBlockPos(), soundEvent, SoundSource.BLOCKS, volume, pitch);
        }
    }

    public void ifAddressed(TriConsumer<ServerLevel, Address.Block, PigeonholeData> consumer) {
        if (address != null && level instanceof ServerLevel serverLevel) {
            getData().ifPresent(data -> consumer.accept(serverLevel, address, data));
        }
    }

    public void ifAddressedOrElse(TriConsumer<ServerLevel, Address.Block, PigeonholeData> consumer, Runnable orElse) {
        if (address != null && level instanceof ServerLevel serverLevel) {
            getData().ifPresentOrElse(data -> consumer.accept(serverLevel, address, data), orElse);
        } else {
            orElse.run();
        }
    }

    public <T> Optional<T> mapAddressed(TriFunction<ServerLevel, Address.Block, PigeonholeData, T> function) {
        if (address != null && level instanceof ServerLevel serverLevel) {
            return getData().map(data -> function.apply(serverLevel, address, data));
        }
        return Optional.empty();
    }
}
