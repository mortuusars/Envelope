package io.github.mortuusars.envelope.world.block;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeHasNewMailS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeGuiSyncBlockDataS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeMenuMailRemovedS2CP;
import io.github.mortuusars.envelope.world.block.occupiable.Occupant;
import io.github.mortuusars.envelope.world.block.occupiable.Occupiable;
import io.github.mortuusars.envelope.world.pigeonhole.PigeonholeManager;
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
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PigeonholeBlockEntity extends BaseContainerBlockEntity implements Occupiable {
    public static final int SLOTS = 3;
    public static final int SLOT_INBOX = 0;
    public static final int SLOT_FOOD = 1;
    public static final int SLOT_MAIL = 2;

    protected List<Occupant.Mutable> occupants = new ArrayList<>();
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    @Nullable
    protected Address.Pigeonhole address = null;
    protected boolean updatedAfterLoading = false;

    public PigeonholeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PigeonholeBlockEntity(BlockPos pos, BlockState blockState) {
        this(Envelope.BlockEntityTypes.PIGEONHOLE.get(), pos, blockState);
    }

    // -- Address

    public @Nullable Address.Pigeonhole address() {
        return address;
    }

    public Optional<Address.Pigeonhole> getAddress() {
        return Optional.ofNullable(address());
    }

    public void setAddress(@NotNull Address.Pigeonhole address) {
        if (Objects.equals(this.address, address)) {
            return;
        }

        if (getLevel() instanceof ServerLevel serverLevel) {
            PigeonholeManager pigeonholeManager = serverLevel.getEnvelopePigeonholeManager();

            if (this.address != null) {
                dropOrReturnAllMail();
                pigeonholeManager.remove(this.address);
            }

            pigeonholeManager.register(address, getBlockPos());
        }

        this.address = address;
        setChanged();
    }

    public boolean hasAddress() {
        return address() != null;
    }

    protected void ensureAddressCorrectness() {
        if (address == null || !(level instanceof ServerLevel serverLevel)) return;
        address = serverLevel.getEnvelopePigeonholeManager().resolve(address, getBlockPos());
    }

    // -- Mail

    public List<ItemStack> getAllMail() {
        if (address != null && level instanceof ServerLevel serverLevel) {
            return serverLevel.getEnvelopePigeonholeManager().getAllMail(address);
        }
        return Collections.emptyList();
    }

    public ItemStack takeMail(MailId mailId, @Nullable Player player) {
        if (address == null || !(level instanceof ServerLevel serverLevel)) return ItemStack.EMPTY;

        return serverLevel.getEnvelopePigeonholeManager().removeMailById(address, mailId)
              .mapValue(extractedMail -> {
                  extractedMail.remove(Envelope.DataComponents.MAIL_ID);
                  extractedMail.remove(Envelope.DataComponents.MAIL_STATUS);
                  extractedMail.remove(Envelope.DataComponents.MAIL_DELIVERY_LOG);
                  extractedMail.remove(Envelope.DataComponents.MAIL_TRAVEL_DURATION);

                  for (ServerPlayer pl : serverLevel.players()) {
                      if (pl.containerMenu instanceof PigeonholeMenu menu
                            && menu.getAddress().equals(address)
                            && (player == null || pl.getUUID().equals(player.getUUID()))) {
                          menu.getMail().removeIf(stack -> MailId.from(stack).map(id -> id.matches(mailId)).orElse(false));
                          Packets.sendToClient(new PigeonholeMenuMailRemovedS2CP(mailId), pl);
                      }
                  }

                  setChanged();

                  return extractedMail;
              })
              .handleFailure(f -> Envelope.LOGGER.error(f.getMessage()), ItemStack.EMPTY);
    }

    public void dropOrReturnAllMail() {
        if (address != null && level instanceof ServerLevel serverLevel) {
            Vec3 pos = Vec3.atCenterOf(getBlockPos());
            for (ItemStack itemStack : getAllMail()) {
                //TODO: Return C.O.D mail
                Containers.dropItemStack(serverLevel, pos.x, pos.y, pos.z, itemStack);
            }
        }
    }

    public void onMailReceived(ServerLevel level, ItemStack mail) {
        setChanged();
        getAddress().ifPresent(address -> {
            for (ServerPlayer player : level.players()) {
                if (player.containerMenu instanceof PigeonholeMenu menu && menu.getAddress().equals(address)) {
                    Packets.sendToClient(PigeonholeHasNewMailS2CP.INSTANCE, player);
                }
            }
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

        // Make some nearby pigeons prioritize this pigeonhole to pick up and deliver mail
        if (getOccupants().isEmpty() && !getItem(SLOT_MAIL).isEmpty() && !getItem(SLOT_FOOD).isEmpty()) {
            for (Pigeon nearbyPigeon : level.getEntitiesOfClass(Pigeon.class, new AABB(getBlockPos()).inflate(16))) {
                nearbyPigeon.getPigeonholeHandler().setPigeonholePos(getBlockPos());
                break;
            }
        }
    }

    public void onBlockRemoved() {
        items.stream().skip(1).forEach(stack -> {
            Containers.dropItemStack(getLevelOrThrow(), getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), stack);
        });

        if (address != null && getLevel() instanceof ServerLevel serverLevel) {
            PigeonholeManager pigeonholeManager = serverLevel.getEnvelopePigeonholeManager();
            dropOrReturnAllMail();
            pigeonholeManager.remove(address);
            address = null;
        }
    }

    @Override
    public void setChanged() {
        if (isFireNearby()) {
            releaseAllOccupants(getLevel(), getBlockPos(), getBlockState(), ReleaseReason.EMERGENCY);
        }

        updateBlockStateIfNeeded();
        super.setChanged();
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
        return address != null ? SLOTS : 0;
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
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

    public ItemStack getFirstAvailableMailToExtract() {
        for (ItemStack stack : getAllMail()) {
            //TODO: Unextractable mail check (payback, etc)
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot == SLOT_INBOX) {
            return MailId.from(getFirstAvailableMailToExtract())
                  .map(id -> takeMail(id, null))
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

    public boolean isSendable(ItemStack stack) {
        return address != null
              && !stack.isEmpty()
              && stack.get(Envelope.DataComponents.MAIL_RECIPIENT) instanceof Address recipient
              && !recipient.equals(address);
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot == SLOT_INBOX;
    }

    // -- Menu

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, Inventory inventory) {
        Preconditions.checkNotNull(address, "Cannot open PigeonholeMenu without an address.");
        return new PigeonholeMenu(id, inventory, getBlockPos(), getAllMail(), getAddress().orElseThrow());
    }

    public boolean openMenu(ServerPlayer player, InteractionHand hand) {
        if (address == null) {
            Envelope.LOGGER.error("Cannot open Pigeonhole: it doesn't have an address.");
            return false;
        }

        ensureAddressCorrectness();

        PlatformHelper.openMenu(player, this, buffer -> {
            List<ItemStack> mail = getAllMail();
            buffer.writeBlockPos(getBlockPos());
            buffer.writeVarInt(mail.size());
            for (ItemStack item : mail) {
                ItemStack.STREAM_CODEC.encode(buffer, item);
            }
            Address.Pigeonhole.STREAM_CODEC.encode(buffer, address);
        });

        Packets.sendToClient(new PigeonholeGuiSyncBlockDataS2CP(getImmutableOccupants()), player);

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
        if (!(entity instanceof Pigeon pigeon) || !(level instanceof ServerLevel serverLevel)) return;

        pigeon.releasedFromPigeonhole(getBlockPos(), getBlockState(), reason); // Calling before mail sending to set home pos etc

        ItemStack mail = getItem(SLOT_MAIL);
        if (isSendable(mail) && !getItem(SLOT_FOOD).isEmpty() && !pigeon.isDelivering()) {
            mail = mail.copyWithCount(1);
            mail.set(Envelope.DataComponents.MAIL_SENDER, address);
            pigeon.startDelivery(serverLevel, mail, getBlockPos());

            getItem(SLOT_FOOD).shrink(1);
            if (getItem(SLOT_FOOD).isEmpty()) {
                setItem(SLOT_FOOD, ItemStack.EMPTY);
            }

            getItem(SLOT_MAIL).shrink(1);
            if (getItem(SLOT_MAIL).isEmpty()) {
                setItem(SLOT_MAIL, ItemStack.EMPTY);
            }
        }

        float wasteChance = getWasteIncreaseChanceOnRelease(entity);

        if (reason != ReleaseReason.EMERGENCY
              && getBlockState().getBlock() instanceof PigeonholeBlock block
              && level.random.nextFloat() < wasteChance) {
            block.addWaste(level, getBlockPos(), getBlockState());
            setChanged();
        }
    }

    @Override
    public void onOccupantsChanged() {
        setChanged();
        for (Player player : getLevelOrThrow().players()) {
            if (player instanceof ServerPlayer serverPlayer
                  && player.containerMenu instanceof PigeonholeMenu menu
                  && menu.getAddress().equals(address)) {
                Packets.sendToClient(new PigeonholeGuiSyncBlockDataS2CP(getImmutableOccupants()), serverPlayer);
            }
        }
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
        return releasedEntity instanceof Pigeon pigeon && pigeon.isDelivering() ? 1f : 0.2f;
    }

    // -- Sync

//    @Nullable
//    @Override
//    public Packet<ClientGamePacketListener> getUpdatePacket() {
//        return ClientboundBlockEntityDataPacket.create(this);
//    }
//
//    @Override
//    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
//        return saveCustomOnly(registries);
//    }

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

        if (tag.contains("address", Tag.TAG_STRING)) {
            address = new Address.Pigeonhole(tag.getString("address"));
        }

        loadOccupiable(tag, registries);

        ensureAddressCorrectness();
        updateBlockStateIfNeeded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        if (address != null) {
            tag.putString("address", address.id());
        }

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
}
