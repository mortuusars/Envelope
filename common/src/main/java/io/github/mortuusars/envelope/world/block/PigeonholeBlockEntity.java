package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.occupiable.PigeonOccupiable;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.block.occupiable.Occupant;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PigeonholeBlockEntity extends BlockEntity implements PigeonOccupiable {
    protected List<Occupant.Mutable> occupants = new ArrayList<>();

    protected PigeonholeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PigeonholeBlockEntity(BlockPos pos, BlockState blockState) {
        this(Envelope.BlockEntityTypes.PIGEONHOLE.get(), pos, blockState);
    }

    // -- Events

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        tickOccupants(level, pos, state);
    }

    @Override
    public void setChanged() {
        if (Position.isFireNearby(level, getBlockPos())) {
            releaseAllOccupants(getLevel(), getBlockPos(), getBlockState(), ReleaseReason.EMERGENCY);
        }
        super.setChanged();
    }

//    protected void updateBlockStateIfNeeded() {
//        if (!isRemoved() && level instanceof ServerLevel serverLevel) {
//            BlockState state = getBlockState();
//            BlockState newState = state
//                  .setValue(PigeonholeBlock.HAS_ADDRESS, address != null)
//                  .setValue(PigeonholeBlock.HAS_MAIL, !getItem(PigeonholeBlockEntity.SLOT_MAIL).isEmpty());
//
//            if (state != newState) {
//                serverLevel.setBlockAndUpdate(getBlockPos(), newState);
//            }
//        }
//    }


    // -- Menu

//    @Override
//    protected @NotNull AbstractContainerMenu createMenu(int id, Inventory inventory) {
//        return mapAddressed((level, address, data) ->
//              new PigeonholeMenu(id, inventory, getBlockPos(), getInbox(), getAddress().orElseThrow()))
//              .orElseThrow(() -> new IllegalStateException("Pigeonhole does not have an address, or data is not available."));
//    }
//
//    public boolean openMenu(ServerPlayer player) {
//        if (address == null) {
//            Envelope.LOGGER.error("Cannot open Pigeonhole: it doesn't have an address.");
//            return false;
//        }
//
//        // Forces sync of be data to the client
//        player.level().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
//
//        PlatformHelper.openMenu(player, this, buffer -> {
//            buffer.writeBlockPos(getBlockPos());
//            Inbox.STREAM_CODEC.encode(buffer, getInbox());
//            Address.Block.STREAM_CODEC.encode(buffer, address);
//        });
//
//        return true;
//    }

    // -- Occupiable

    @Override
    public List<Occupant.Mutable> getOccupants() {
        return occupants;
    }

    @Override
    public void onOccupantReleased(Level level, Entity entity, ReleaseReason reason) {
        if (reason == ReleaseReason.EMERGENCY) return;

        if (entity instanceof Pigeon pigeon) {
            pigeon.releasedFromPigeonhole(getBlockPos(), getBlockState(), reason); // Calling before mail sending to set home pos etc
        }

        float wasteChance = getWasteIncreaseChanceOnRelease(entity);
        if (getBlockState().getBlock() instanceof PigeonholeBlock block && level.random.nextFloat() < wasteChance) {
            block.addWaste(level, getBlockPos(), getBlockState());
            setChanged();
        }
    }

//    protected void tryStartDelivery(ServerLevel level, Pigeon pigeon) {
//        if (this.address == null || pigeon.isDelivering()) {
//            return;
//        }
//
//        ItemStack mailStack = getItem(SLOT_MAIL);
//        if (mailStack.isEmpty() || !mailStack.has(Envelope.DataComponents.MAIL_RECIPIENT)) {
//            return;
//        }
//
//        mailStack = mailStack.copyWithCount(1);
//
//        Address recipient = mailStack.getOrDefault(Envelope.DataComponents.MAIL_RECIPIENT, Address.UNKNOWN);
//        if (MailService.of(level).resolve(recipient).matches(this.address)) {
//            return;
//        }
//
//        MailService.of(level).getDeliveryManager()
//              .start(pigeon, Delivery.builder()
//                    .deliver(new Mail(mailStack))
//                    .from(address)
//                    .to(recipient)
//                    .owner(getOwner()))
//              .ifPresent(delivery -> {
//                  removeItem(SLOT_MAIL, 1);
//                  removeItem(SLOT_FOOD, 1);
//              });
//    }

    @Override
    public void onOccupantsChanged() {
        setChanged();
    }

    protected float getWasteIncreaseChanceOnRelease(Entity releasedEntity) {
        return releasedEntity instanceof Courier courier && courier.isDelivering() ? 1f : 0.2f;
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

    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(getSerializedOccupantsName());
    }

    // -- Save/Load

//    @Override
//    public void clearRemoved() {
//        super.clearRemoved();
//
//        if (level instanceof ServerLevel serverLevel && address != null) {
//            if (inboxId != null) {
//                Inboxes.get(serverLevel).retrieve(inboxId)
//                      .ifPresentOrElse(inbox -> {
//                          this.inbox = inbox;
//                          Envelope.LOGGER.error("Retrieved inbox with '{}' mail of {}@[{}].", inbox.getContainerSize(), address, getBlockPos().toShortString());
//                      }, () -> Envelope.LOGGER.error("Cannot retrieve inbox of {}@[{}].", address, getBlockPos().toShortString()));
//            } else {
//                createInbox();
//            }
//        }
//    }
//
//    private void createInbox() {
//        inboxId = UUID.randomUUID();
//        inbox = new Inbox(Collections.emptyList()) {
//            @Override
//            public void setChanged() {
//                PigeonholeBlockEntity.this.setChanged();
//            }
//        };
//        setChanged();
//    }

//    @Override
//    public void setRemoved() {
//        super.setRemoved();
//
//        if (level instanceof ServerLevel serverLevel && address != null && inboxId != null && inbox != null) {
////            if (inbox.getId().equals(Util.NIL_UUID)) {
////                Envelope.LOGGER.error("Cannot store inbox of pigeonhole {}@[{}]: no inboxId", address, getBlockPos().toShortString());
////                return;
////            }
//
//            Inboxes.get(serverLevel).store(inboxId, inbox);
//            Envelope.LOGGER.info("Stored inbox of size {} {}@[{}]", inbox.getContainerSize(), address, getBlockPos().toShortString());
//            inbox = null;
//        }
//    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        loadOccupiable(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        saveOccupiable(tag, registries);
    }

    // --

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }

    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, getBlockPos(), soundEvent, SoundSource.BLOCKS, volume, pitch);
        }
    }
}
