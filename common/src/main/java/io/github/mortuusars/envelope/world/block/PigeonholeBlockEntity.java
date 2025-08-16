package io.github.mortuusars.envelope.world.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.Mail;
import io.github.mortuusars.envelope.api.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.api.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.PigeonholeAddressMenu;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.mail.Mailboxes;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class PigeonholeBlockEntity extends BlockEntity {
    public static final int MAX_OCCUPANTS = 3;

    static final List<String> IGNORED_PIGEON_TAGS = Arrays.asList(
            "Air",
            "ArmorDropChances",
            "ArmorItems",
            "Brain",
            "CanPickUpLoot",
            "DeathTime",
            "FallDistance",
            "FallFlying",
            "Fire",
            "HandDropChances",
            "HandItems",
            "HurtByTimestamp",
            "HurtTime",
            "LeftHanded",
            "Motion",
            "NoGravity",
            "OnGround",
            "PortalCooldown",
            "Pos",
            "Rotation",
            "SleepingX",
            "SleepingY",
            "SleepingZ",
            "Passengers",
            "UUID",
            "leash",
            "cannot_enter_pigeonhole_ticks",
            "pigeonhole_pos"
    );

    protected final List<OccupantData> occupants = new ArrayList<>();

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

    public PigeonholeBlockEntity setAddress(@NotNull Address.Pigeonhole address) {
        this.address = address;
        if (!getLevelOrThrow().isClientSide()) {
            Mail.getMailboxes().create(this.address);
        }
        setChanged();
        return this;
    }

    // -- Events

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (address != null) {
            dropOrReturnAllMail();
            Mail.getMailboxes().remove(address);
        }
    }

    // -- Mail

    public List<ItemStack> getAllMail() {
        return Mail.getMailboxes().getAllMail(getAddress().orElseThrow());
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

    public ItemStack takeMail(ItemStack mail, @Nullable Player player) {
        if (!mail.has(Envelope.DataComponents.MAIL_ID)) {
            return ItemStack.EMPTY;
        }

        Result<ItemStack> extractResult = Mail.getMailboxes().removeMail(address, mail.get(Envelope.DataComponents.MAIL_ID));
        return extractResult
                .mapValue(extractedMail -> {
                    MailTravelingLog.addRecords(extractedMail, TravelingRecord.receivedAt(getAddress().orElseThrow(),
                            getLevelOrThrow().getGameTime(), Optional.ofNullable(player).map(Player::getName)));
                    extractedMail.remove(Envelope.DataComponents.MAIL_ID);
                    extractedMail.remove(Envelope.DataComponents.MAIL_RECIPIENT);
                    extractedMail.remove(Envelope.DataComponents.MAIL_SENDER);
                    extractedMail.remove(Envelope.DataComponents.MAIL_SENT_AT);
                    extractedMail.remove(Envelope.DataComponents.MAIL_TRAVEL_DURATION);
                    return extractedMail;
                })
                .handleFailure(f -> Envelope.LOGGER.error(f.getMessage()), ItemStack.EMPTY);
    }

    protected void dropOrReturnAllMail() {
        if (level instanceof ServerLevel serverLevel && getAddress().isPresent()) {
            Vec3 p = Vec3.atCenterOf(getBlockPos());
            for (ItemStack itemStack : getAllMail()) {
                Containers.dropItemStack(serverLevel, p.x, p.y, p.z, itemStack);
            }

            Mailboxes.get(serverLevel.getServer()).remove(address);
        }
    }

    // -- Menu

    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("gui.envelope.pigeonhole");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new PigeonholeMenu(id, inventory, getBlockPos(), getAllMail());
            }
        };
    }

    public MenuProvider createAddressMenuProvider(InteractionHand hand, String suggestedAddress) {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("gui.envelope.pigeonhole_address.enter_address");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new PigeonholeAddressMenu(id, inventory, hand, getBlockPos(), suggestedAddress);
            }
        };
    }

    // -- Save/Load

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("address", Tag.TAG_STRING)) {
            address = new Address.Pigeonhole(tag.getString("address"));
        }

        occupants.clear();
        if (tag.contains("pigeons")) {
            Occupant.LIST_CODEC
                    .parse(NbtOps.INSTANCE, tag.get("pigeons"))
                    .resultOrPartial(string -> Envelope.LOGGER.error("Failed to parse bees: '{}'", string))
                    .ifPresent(list -> list.forEach(this::storeOccupant));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (address != null) {
            tag.putString("address", address.id());
        }
        tag.put("pigeons", Occupant.LIST_CODEC.encodeStart(NbtOps.INSTANCE, getPigeons()).getOrThrow());
    }

    private List<Occupant> getPigeons() {
        return occupants.stream().map(OccupantData::toOccupant).toList();
    }

    // --

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }

    @Override
    public void setChanged() {
        if (isFireNearby()) {
            releaseAllOccupants(getLevelOrThrow().getBlockState(getBlockPos()), ReleaseReason.EMERGENCY);
        }

        super.setChanged();
    }

    public boolean isFireNearby() {
        if (this.level == null) return false;

        for (BlockPos blockPos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
            if (this.level.getBlockState(blockPos).getBlock() instanceof FireBlock) {
                return true;
            }
        }

        return false;
    }

    // -- Pigeons

    public boolean isEmpty() {
        return occupants.isEmpty();
    }

    public boolean isFull() {
        return occupants.size() >= MAX_OCCUPANTS;
    }

    public boolean hasSpace() {
        return !isFull();
    }

    public void addOccupant(Entity occupant) {
        if (isFull()) return;

        occupant.stopRiding();
        occupant.ejectPassengers();
        storeOccupant(Occupant.of(occupant));

        if (level != null) {
            BlockPos pos = getBlockPos();
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    getEnterSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(occupant, getBlockState()));
        }

        occupant.discard();
        super.setChanged();
    }

    public void storeOccupant(Occupant occupant) {
        occupants.add(new OccupantData(occupant));
    }

    protected Optional<Entity> releaseOccupant(Level level, BlockPos pos, BlockState state,
                                               Occupant occupant, ReleaseReason releaseStatus) {
        if ((level.isNight() || level.isThundering()) && releaseStatus != ReleaseReason.EMERGENCY) {
            return Optional.empty();
        }

        Direction direction = state.getValue(BeehiveBlock.FACING);
        BlockPos releasePos = pos.relative(direction);

        boolean isFrontBlockedOff = !level.getBlockState(releasePos).getCollisionShape(level, releasePos).isEmpty();
        if (isFrontBlockedOff && releaseStatus != ReleaseReason.EMERGENCY) {
            return Optional.empty();
        }

        Entity entity = occupant.createEntity(level, pos);
        if (entity == null) {
            return Optional.empty();
        }

        if (entity instanceof Pigeon) {
            float wasteChance = 0.2f;
            if (releaseStatus != ReleaseReason.EMERGENCY
                    && state.getBlock() instanceof PigeonholeBlock block
                    && level.random.nextFloat() < wasteChance) {
                block.addWaste(level, pos, state);
            }

            double offset = isFrontBlockedOff ? 0.0 : 0.55 + (double)(entity.getBbWidth() / 2.0F);
            double x = (double)pos.getX() + 0.5 + offset * (double)direction.getStepX();
            double y = (double)pos.getY() + 0.5 - (double)(entity.getBbHeight() / 2.0F);
            double z = (double)pos.getZ() + 0.5 + offset * (double)direction.getStepZ();
            entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
        }

        level.playSound(null, pos, getExitSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, level.getBlockState(pos)));

        if (level.addFreshEntity(entity)) {
            return Optional.of(entity);
        }

        return Optional.empty();
    }

    public void releaseAllOccupants(BlockState state, ReleaseReason reason) {
        boolean releasedSomeone = occupants.removeIf(occupant ->
                releaseOccupant(getLevelOrThrow(), getBlockPos(), state, occupant.toOccupant(), reason).isPresent());

        if (releasedSomeone) {
            super.setChanged();
        }
    }

    public SoundEvent getEnterSound() {
        return SoundEvents.BEEHIVE_ENTER;
    }

    public SoundEvent getExitSound() {
        return SoundEvents.BEEHIVE_EXIT;
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
            return new PigeonholeBlockEntity.Occupant(this.occupant.entityData, this.ticksInside, this.occupant.minTicksInPigeonhole);
        }
    }

    public record Occupant(CustomData entityData, int ticksInPigeonhole, int minTicksInPigeonhole) {
        public static final Codec<PigeonholeBlockEntity.Occupant> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                CustomData.CODEC.optionalFieldOf("entity_data", CustomData.EMPTY).forGetter(PigeonholeBlockEntity.Occupant::entityData),
                                Codec.INT.fieldOf("ticks_in_pigeonhole").forGetter(PigeonholeBlockEntity.Occupant::ticksInPigeonhole),
                                Codec.INT.fieldOf("min_ticks_in_pigeonhole").forGetter(PigeonholeBlockEntity.Occupant::minTicksInPigeonhole)
                        )
                        .apply(instance, PigeonholeBlockEntity.Occupant::new)
        );
        public static final Codec<List<PigeonholeBlockEntity.Occupant>> LIST_CODEC = CODEC.listOf();
        @SuppressWarnings("deprecation")
        public static final StreamCodec<ByteBuf, PigeonholeBlockEntity.Occupant> STREAM_CODEC = StreamCodec.composite(
                CustomData.STREAM_CODEC,
                PigeonholeBlockEntity.Occupant::entityData,
                ByteBufCodecs.VAR_INT,
                PigeonholeBlockEntity.Occupant::ticksInPigeonhole,
                ByteBufCodecs.VAR_INT,
                PigeonholeBlockEntity.Occupant::minTicksInPigeonhole,
                PigeonholeBlockEntity.Occupant::new
        );

        public static PigeonholeBlockEntity.Occupant of(Entity entity) {
            CompoundTag tag = new CompoundTag();
            entity.save(tag);
            PigeonholeBlockEntity.IGNORED_PIGEON_TAGS.forEach(tag::remove);
            return new PigeonholeBlockEntity.Occupant(CustomData.of(tag), 0, 600);
        }

        public static PigeonholeBlockEntity.Occupant create(int ticksInHive) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(Envelope.EntityTypes.PIGEON.get()).toString());
            return new PigeonholeBlockEntity.Occupant(CustomData.of(compoundTag), ticksInHive, 600);
        }

        @Nullable
        public Entity createEntity(Level level, BlockPos pos) {
            CompoundTag compoundTag = entityData.copyTag();
            PigeonholeBlockEntity.IGNORED_PIGEON_TAGS.forEach(compoundTag::remove);
            Entity entity = EntityType.loadEntityRecursive(compoundTag, level, Function.identity());
            if (entity == null || !entity.getType().is(Envelope.Tags.EntityTypes.PIGEONHOLE_INHABITORS)) {
                return null;
            }

            // entity.setNoGravity(true); ??

            if (entity instanceof Pigeon pigeon) {
                pigeon.setPigeonholePos(pos);
                setBeeReleaseData(ticksInPigeonhole, pigeon);
            }

            return entity;
        }

        private void setBeeReleaseData(int ticksInHive, Pigeon pigeon) {
            int i = pigeon.getAge();
            if (i < 0) {
                pigeon.setAge(Math.min(0, i + ticksInHive));
            } else if (i > 0) {
                pigeon.setAge(Math.max(0, i - ticksInHive));
            }

            pigeon.setInLoveTime(Math.max(0, pigeon.getInLoveTime() - ticksInHive));
        }
    }

    public enum ReleaseReason {
        MAIL_DELIVERED,
        RELEASED,
        EMERGENCY;
    }
}
