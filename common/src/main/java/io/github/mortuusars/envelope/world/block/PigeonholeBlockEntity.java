package io.github.mortuusars.envelope.world.block;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.item.component.MailDeliveryLog;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeHasNewMailS2CP;
import io.github.mortuusars.envelope.network.packet.clientbound.PigeonholeSyncBlockDataS2CP;
import io.github.mortuusars.envelope.world.pigeonhole.PigeonholeManager;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.item.component.MailId;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class PigeonholeBlockEntity extends BaseContainerBlockEntity {
    public static final int MAX_OCCUPANTS = 3;
    public static final int SLOTS = 2;
    public static final int SLOT_FOOD = 0;
    public static final int SLOT_MAIL = 1;

    protected final List<OccupantData> occupants = new ArrayList<>();
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    @Nullable
    protected Address.Pigeonhole address = null;

    public PigeonholeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
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

    protected void ensureAddressCorrectness() {
        if (address == null || !(level instanceof ServerLevel serverLevel)) return;
        address = serverLevel.getEnvelopePigeonholeManager().resolve(address, getBlockPos());
    }

    // -- Events

    public static void serverTick(Level level, BlockPos pos, BlockState state, PigeonholeBlockEntity blockEntity) {
        if (level instanceof ServerLevel serverLevel) {
            blockEntity.serverTick(serverLevel, pos, state);
        }
    }

    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        tickOccupants(pos, state);

        if (!occupants.isEmpty() && level.getRandom().nextDouble() < 0.005) {
            double x = pos.getX() + 0.5;
            double y = pos.getY();
            double z = pos.getZ() + 0.5;
            //TODO: pigeonhole work sound
            level.playSound(null, x, y, z, Envelope.SoundEvents.PIGEON_AMBIENT.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }

        if (occupants.isEmpty() && !getItem(SLOT_MAIL).isEmpty()) {
            for (Pigeon nearbyPigeon : level.getEntitiesOfClass(Pigeon.class, new AABB(getBlockPos()).inflate(16))) {
                nearbyPigeon.getPigeonholeHandler().setPigeonholePos(getBlockPos());
                break;
            }
        }
    }

    protected void tickOccupants(BlockPos pos, BlockState state) {
        Iterator<OccupantData> iterator = occupants.iterator();

        boolean releasedSomeone = false;
        while (iterator.hasNext()) {
            OccupantData occupantData = iterator.next();
            if (occupantData.tick()) {
                if (releaseOccupant(pos, state, occupantData.toOccupant(), ReleaseReason.RELEASED).isPresent()) {
                    releasedSomeone = true;
                    iterator.remove();
                }
            }
        }

        if (releasedSomeone) {
            super.setChanged();
            onOccupantsChanged();
        }
    }

    public void onBlockRemoved() {
        if (this.address == null) {
            return;
        }

        if (getLevel() instanceof ServerLevel serverLevel) {
            PigeonholeManager pigeonholeManager = serverLevel.getEnvelopePigeonholeManager();
            dropOrReturnAllMail();
            pigeonholeManager.remove(this.address);
        }

        this.address = null;
    }

    protected void onOccupantsChanged() {
        for (Player player : getLevelOrThrow().players()) {
            if (player instanceof ServerPlayer serverPlayer
                    && player.containerMenu instanceof PigeonholeMenu menu
                    && menu.getAddress().equals(address)) {
                Packets.sendToClient(new PigeonholeSyncBlockDataS2CP(getOccupants()), serverPlayer);
            }
        }
    }

    @Override
    public void setChanged() {
        if (isFireNearby()) {
            releaseAllOccupants(getLevelOrThrow().getBlockState(getBlockPos()), ReleaseReason.EMERGENCY);
        }

        super.setChanged();
    }

    // -- Mail

    public List<ItemStack> getAllMail() {
        if (address != null && level instanceof ServerLevel serverLevel) {
            return serverLevel.getEnvelopePigeonholeManager().getAllMail(address);
        }
        return Collections.emptyList();
    }

//    public boolean sendMail(ItemStack mail, @Nullable Player player) {
//        if (mail.isEmpty()) {
//            Envelope.LOGGER.error("Cannot send empty mail.");
//            return false;
//        }
//
//        if (!mail.has(Envelope.DataComponents.MAIL_RECIPIENT)) {
//            Envelope.LOGGER.error("Cannot send mail: no 'envelope:recipient' defined. {}", mail);
//            return false;
//        }
//
//        Address sender = new Address.Mailbox(address);
//        mail.set(Envelope.DataComponents.MAIL_SENDER, sender);
//
//        if (level instanceof ServerLevel) {
//            Mail.send(mail, player);
//        }
//
//        return true;
//    }

    public ItemStack takeMail(MailId id, @Nullable Player player) {
        Preconditions.checkState(level instanceof ServerLevel, "Can only be called server-side.");
        Preconditions.checkState(address != null, "Can only be called when Pigeonhole has an address.");

        return ((ServerLevel) level).getEnvelopePigeonholeManager().removeMailById(address, id)
                .mapValue(extractedMail -> {
                    MailDeliveryLog.addRecords(extractedMail,
                            MailDeliveryLog.TravelingRecord.receivedAt(address)
                                    .atTime(level.getGameTime())
                                    .withOperatorMessage(Optional.ofNullable(player).map(Player::getName)));
                    extractedMail.remove(Envelope.DataComponents.MAIL_TRAVEL_DURATION);
                    return extractedMail;
                })
                .handleFailure(f -> Envelope.LOGGER.error(f.getMessage()), ItemStack.EMPTY);
    }

    public void dropOrReturnAllMail() {
        if (address != null && level instanceof ServerLevel serverLevel) {
            Vec3 pos = Vec3.atCenterOf(getBlockPos());
            for (ItemStack itemStack : getAllMail()) {
                //TODO: Return mail
                Containers.dropItemStack(serverLevel, pos.x, pos.y, pos.z, itemStack);
            }
        }
    }

    public void onMailDelivered(ServerLevel level, ItemStack mail) {
        getAddress().ifPresent(address -> {
            for (ServerPlayer player : level.players()) {
                if (player.containerMenu instanceof PigeonholeMenu menu && menu.getAddress().equals(address)) {
                    Packets.sendToClient(PigeonholeHasNewMailS2CP.INSTANCE, player);
                }
            }
        });
    }

    // Container

    @Override
    public int getContainerSize() {
        return SLOTS;
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
    public void setItem(int slot, ItemStack stack) {
        if (slot == SLOT_MAIL) {
            stack.remove(Envelope.DataComponents.MAIL_SENDER);
            stack.remove(Envelope.DataComponents.MAIL_DELIVERY_LOG);
        }
        super.setItem(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (address == null) return false;
        if (slot == SLOT_FOOD) return stack.is(Envelope.Tags.Items.PIGEON_FOOD);
        if (slot == SLOT_MAIL) return canSend(stack);
        return false;
    }

    public boolean canSend(ItemStack stack) {
        return address != null
                && stack.get(Envelope.DataComponents.MAIL_RECIPIENT) instanceof Address recipient
                && !recipient.equals(address);
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return false;
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

        Packets.sendToClient(new PigeonholeSyncBlockDataS2CP(getOccupants()), player);

        return true;
    }

    // --

    public boolean isFireNearby() {
        if (this.level == null) return false;

        for (BlockPos blockPos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
            if (this.level.getBlockState(blockPos).getBlock() instanceof FireBlock) {
                return true;
            }
        }

        return false;
    }

    // -- Occupants

    public boolean isFull() {
        return occupants.size() >= MAX_OCCUPANTS;
    }

    public boolean hasSpace() {
        return !isFull();
    }

    public List<Occupant> getOccupants() {
        return occupants.stream().map(OccupantData::toOccupant).toList();
    }

    public void addOccupant(Entity occupant) {
        if (isFull()) return;

        occupant.stopRiding();
        occupant.ejectPassengers();

        storeOccupant(Occupant.of(occupant, getFirstFreeOccupantSlot()));

        if (level != null) {
            BlockPos pos = getBlockPos();
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    getEnterSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(occupant, getBlockState()));
        }

        occupant.discard();
        super.setChanged();
        onOccupantsChanged();
    }

    protected int getFirstFreeOccupantSlot() {
        int slot = 0;
        while (true) {
            int s = slot;
            boolean taken = false;
            for (OccupantData obj : occupants) {
                if (obj.occupant.slot == s) {
                    taken = true;
                    break;
                }
            }
            if (!taken) return s;
            slot++;
        }
    }

    protected void storeOccupant(Occupant occupant) {
        occupants.add(new OccupantData(occupant));
    }

    protected Optional<Entity> releaseOccupant(BlockPos pos, BlockState state,
                                               Occupant occupant, ReleaseReason releaseStatus) {
        //noinspection PatternVariableHidesField
        if (!(level instanceof ServerLevel level) || (level.isNight() || level.isThundering()) && releaseStatus != ReleaseReason.EMERGENCY) {
            return Optional.empty();
        }

        Direction direction = state.getValue(BeehiveBlock.FACING);
        BlockPos releasePos = pos.relative(direction);

        boolean isFrontBlockedOff = !level.getBlockState(releasePos).getCollisionShape(level, releasePos).isEmpty();
        if (isFrontBlockedOff && releaseStatus != ReleaseReason.EMERGENCY) {
            return Optional.empty();
        }

        Entity entity = occupant.createEntity(level, pos);
        if (entity == null) return Optional.empty();

        double offset = isFrontBlockedOff ? 0.0 : 0.55 + (double) (entity.getBbWidth() / 2.0F);
        double x = (double) pos.getX() + 0.5 + offset * (double) direction.getStepX();
        double y = (double) pos.getY() + 0.5 - (double) (entity.getBbHeight() / 2.0F);
        double z = (double) pos.getZ() + 0.5 + offset * (double) direction.getStepZ();
        entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());

        level.playSound(null, pos, getExitSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, level.getBlockState(pos)));

        if (level.addFreshEntity(entity)) {
            if (entity instanceof Pigeon pigeon) {
                pigeon.releasedFromPigeonhole(pos, state, releaseStatus); // Calling before mail sending to set home pos etc

                ItemStack mail = getItem(SLOT_MAIL);
                if (canSend(mail) && !pigeon.isDelivering()) {
                    mail.set(Envelope.DataComponents.MAIL_SENDER, address);
                    pigeon.startDelivery(level, mail, getBlockPos());
                    setItem(SLOT_MAIL, ItemStack.EMPTY);
                }

                float wasteChance = 0.2f;

                if (pigeon.isDelivering()) {
                    wasteChance = 1f;
                }

                if (releaseStatus != ReleaseReason.EMERGENCY
                        && state.getBlock() instanceof PigeonholeBlock block
                        && level.random.nextFloat() < wasteChance) {
                    block.addWaste(level, pos, state);
                    setChanged();
                }
            }

            return Optional.of(entity);
        }

        return Optional.empty();
    }

    public void releaseAllOccupants(BlockState state, ReleaseReason reason) {
        boolean releasedSomeone = occupants.removeIf(occupant ->
                releaseOccupant(getBlockPos(), state, occupant.toOccupant(), reason).isPresent());

        if (releasedSomeone) {
            super.setChanged();
            onOccupantsChanged();
        }
    }

    public SoundEvent getEnterSound() {
        return SoundEvents.BEEHIVE_ENTER;
    }

    public SoundEvent getExitSound() {
        return SoundEvents.BEEHIVE_EXIT;
    }

    // -- Save/Load

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.contains("address", Tag.TAG_STRING)) {
            address = new Address.Pigeonhole(tag.getString("address"));
        }

        occupants.clear();
        if (tag.contains("pigeons")) {
            Occupant.LIST_CODEC
                    .parse(NbtOps.INSTANCE, tag.get("pigeons"))
                    .resultOrPartial(string -> Envelope.LOGGER.error("Failed to parse pigeons: '{}'", string))
                    .ifPresent(list -> list.forEach(this::storeOccupant));
        }

        ensureAddressCorrectness();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.setBlockAndUpdate(getBlockPos(), getBlockState()
                .setValue(PigeonholeBlock.HAS_ADDRESS, getAddress().isPresent())
                .setValue(PigeonholeBlock.HAS_MAIL, !getItem(PigeonholeBlockEntity.SLOT_MAIL).isEmpty()));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        ContainerHelper.saveAllItems(tag, items, registries);
        if (address != null) {
            tag.putString("address", address.id());
        }
        tag.put("pigeons", Occupant.LIST_CODEC.encodeStart(NbtOps.INSTANCE, getOccupants()).getOrThrow());
    }

    // --

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return getAddress().map(Address::getDisplayName).orElse(Component.translatable("container.envelope.pigeonhole"));
    }

    // --

    protected static class OccupantData {
        protected final PigeonholeBlockEntity.Occupant occupant;
        protected int ticksInside;

        OccupantData(PigeonholeBlockEntity.Occupant occupant) {
            this.occupant = occupant;
            this.ticksInside = occupant.ticksInPigeonhole();
        }

        public boolean tick() {
            return this.ticksInside++ > this.occupant.minTicksInPigeonhole;
        }

        public PigeonholeBlockEntity.Occupant toOccupant() {
            return new PigeonholeBlockEntity.Occupant(this.occupant.entityData, this.occupant.slot, this.ticksInside, this.occupant.minTicksInPigeonhole);
        }
    }

    public record Occupant(CustomData entityData, int slot, int ticksInPigeonhole, int minTicksInPigeonhole) {
        public static final Codec<PigeonholeBlockEntity.Occupant> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                CustomData.CODEC.optionalFieldOf("entity_data", CustomData.EMPTY).forGetter(PigeonholeBlockEntity.Occupant::entityData),
                                Codec.INT.fieldOf("slot").forGetter(PigeonholeBlockEntity.Occupant::slot),
                                Codec.INT.fieldOf("ticks_in_pigeonhole").forGetter(PigeonholeBlockEntity.Occupant::ticksInPigeonhole),
                                Codec.INT.fieldOf("min_ticks_in_pigeonhole").forGetter(PigeonholeBlockEntity.Occupant::minTicksInPigeonhole)
                        )
                        .apply(instance, PigeonholeBlockEntity.Occupant::new)
        );
        public static final Codec<List<PigeonholeBlockEntity.Occupant>> LIST_CODEC = CODEC.listOf();
        @SuppressWarnings("deprecation")
        public static final StreamCodec<ByteBuf, PigeonholeBlockEntity.Occupant> STREAM_CODEC = StreamCodec.composite(
                CustomData.STREAM_CODEC, PigeonholeBlockEntity.Occupant::entityData,
                ByteBufCodecs.VAR_INT, PigeonholeBlockEntity.Occupant::slot,
                ByteBufCodecs.VAR_INT, PigeonholeBlockEntity.Occupant::ticksInPigeonhole,
                ByteBufCodecs.VAR_INT, PigeonholeBlockEntity.Occupant::minTicksInPigeonhole,
                PigeonholeBlockEntity.Occupant::new
        );

        public static PigeonholeBlockEntity.Occupant of(Entity entity, int slot, int minTicksInPigeonhole) {
            CompoundTag tag = new CompoundTag();
            entity.save(tag);
            Pigeon.IGNORED_TAGS.forEach(tag::remove);
            return new PigeonholeBlockEntity.Occupant(CustomData.of(tag), slot, 0, minTicksInPigeonhole);
        }

        public static PigeonholeBlockEntity.Occupant of(Entity entity, int slot) {
            return of(entity, slot, 100);
        }

        public static PigeonholeBlockEntity.Occupant create(int slot, int ticksInHive) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(Envelope.EntityTypes.PIGEON.get()).toString());
            return new PigeonholeBlockEntity.Occupant(CustomData.of(compoundTag), slot, ticksInHive, 600);
        }

        @Nullable
        public Entity createEntity(Level level, BlockPos pos) {
            CompoundTag compoundTag = entityData.copyTag();
            Pigeon.IGNORED_TAGS.forEach(compoundTag::remove);
            Entity entity = EntityType.loadEntityRecursive(compoundTag, level, Function.identity());
            if (entity == null || !entity.getType().is(Envelope.Tags.EntityTypes.PIGEONHOLE_INHABITORS)) {
                return null;
            }

            entity.setNoGravity(true);

            if (entity instanceof Pigeon pigeon) {
                pigeon.getPigeonholeHandler().setPigeonholePos(pos);
                setPigeonReleaseData(ticksInPigeonhole, pigeon);
            }

            return entity;
        }

        private void setPigeonReleaseData(int ticksInPigeonhole, Pigeon pigeon) {
            int i = pigeon.getAge();
            if (i < 0) {
                pigeon.setAge(Math.min(0, i + ticksInPigeonhole));
            } else if (i > 0) {
                pigeon.setAge(Math.max(0, i - ticksInPigeonhole));
            }

            pigeon.setInLoveTime(Math.max(0, pigeon.getInLoveTime() - ticksInPigeonhole));
        }
    }

    public enum ReleaseReason {
        MAIL_DELIVERED,
        RELEASED,
        EMERGENCY;
    }
}
