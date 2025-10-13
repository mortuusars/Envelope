package io.github.mortuusars.envelope.world.entity;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.item.component.MailDeliveryLog;
import io.github.mortuusars.envelope.util.bugger.BuggerPackets;
import io.github.mortuusars.envelope.world.BackgroundDelivery;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.delivery.BackgroundCourier;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import io.github.mortuusars.envelope.world.entity.ai.goal.*;
import io.github.mortuusars.envelope.world.item.component.MailStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntFunction;

public class Pigeon extends Animal implements VariantHolder<Pigeon.Variant>, FlyingAnimal, Courier {
    public static final List<String> IGNORED_TAGS = Arrays.asList(
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
            "PigeonholePos",
            "LastPigeonholePos",
            "LeftPigeonholeAt",
            "WouldWantToEnterPigeonholeAfter",
            "Sitting",
            "Delivery"
    );

    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_SITTING = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_MAIL = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    protected float flapping = 1.0F;
    protected float nextFlap = 1.0F;

    protected PigeonholeHandler pigeonholeHandler;

    protected @Nullable Delivery delivery = null;

    public Pigeon(EntityType<? extends Pigeon> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.pigeonholeHandler = new PigeonholeHandler(level);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
    }

    // -- Spawn

    public static boolean checkPigeonSpawnRules(EntityType<Pigeon> pigeon, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Config.Server.Pigeon.SPAWNS_NATURALLY.get() && level.getBlockState(pos.below()).is(Envelope.Tags.Blocks.PIGEON_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        setVariant(Variant.getRandom(random));

        if (spawnGroupData == null) {
            spawnGroupData = new AgeableMobGroupData(true);
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // -- AI

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PigeonDeliverMailGoal(this));
        goalSelector.addGoal(1, new PigeonEnterPigeonholeGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.25, itemStack -> itemStack.is(Envelope.Tags.Items.PIGEON_FOOD), false));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.25));
        goalSelector.addGoal(5, new PigeonLocatePigeonholeGoal(this));
        goalSelector.addGoal(5, new PigeonGoToPigeonholeGoal(this));
        goalSelector.addGoal(6, new PigeonWanderGoal(this));
        goalSelector.addGoal(7, new FloatGoal(this));
    }

    @Override
    protected @NotNull FlyingPathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.calculateFlapping();

        if (level() instanceof ServerLevel level) {
            if (this.tickCount % 20 == 0 && !getPigeonholeHandler().isPigeonholeValid(blockPosition())) {
                getPigeonholeHandler().setPigeonholePos(null);
            }

            if (isDelivering() && !isInSafeSimulationDistance(level)) {
                transitionToBackground(level, true);
            }
        }
    }

    protected boolean isInSafeSimulationDistance(ServerLevel level) {
        int simDistance = level.getServer().getPlayerList().getSimulationDistance();
        int range = simDistance - 1; // Reduce by 1 chunk to be safe.
        return level.players().stream().anyMatch(player -> {
            double dx = Math.abs(getX() - player.getX()) / 16.0;
            double dz = Math.abs(getZ() - player.getZ()) / 16.0;
            return Math.max(dx, dz) <= range;
        });
    }

    protected void calculateFlapping() {
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed = this.flapSpeed + (float) (!this.onGround() && !this.isPassenger() ? 4 : -1) * 0.3F;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
        if (!this.onGround() && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }

        this.flapping *= 0.9F;
        Vec3 vec3 = this.getDeltaMovement();
        if (!this.onGround() && vec3.y < 0.0) {

            this.setDeltaMovement(vec3.multiply(1.0, 0.85, 1.0));
        }

        this.flap = this.flap + this.flapping * 2.0F;
    }

    public void unloaded(ServerLevel level) {
        if (level().isClientSide()) return;
        if (getRemovalReason() != null) return;
        if (!isDelivering()) return;

        transitionToBackground(level, false);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (!isRemoved() && !dead && isDelivering()) {
            Envelope.LOGGER.info("Delivering pigeon has died!");
            //TODO: send pigeon death notice to sender
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        if (getDelivery() != null && !getDelivery().getMail().isEmpty()) {
            spawnAtLocation(getDelivery().getMail());
            getDelivery().setMail(ItemStack.EMPTY);
        }
    }

    // -- Properties

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FLYING_SPEED, 1.5F)
                .add(Attributes.MOVEMENT_SPEED, 0.5F)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT_ID, 0);
        builder.define(DATA_IS_SITTING, false);
        builder.define(DATA_HAS_MAIL, false);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return Envelope.EntityTypes.PIGEON.get().create(level);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Envelope.Tags.Items.PIGEON_FOOD);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected boolean isFlapping() {
        return flyDist > nextFlap;
    }

    @Override
    protected void onFlap() {
        playSound(Envelope.SoundEvents.PIGEON_FLY.get(), 0.15F, 1.0F);
        nextFlap = flyDist + flapSpeed / 2.0F;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public @NotNull Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5F * getEyeHeight(), getBbWidth() * 0.4F);
    }

    public boolean isSitting() {
        return entityData.get(DATA_IS_SITTING);
    }

    public void setSitting(boolean sitting) {
        entityData.set(DATA_IS_SITTING, sitting);
    }

    public boolean hasMail() {
        return entityData.get(DATA_HAS_MAIL);
    }

    public void setHasMail(boolean hasMail) {
        entityData.set(DATA_HAS_MAIL, hasMail);
    }

    public boolean hasFancyHat() {
        //TODO: supporters
        // return getOwnerUUID()
        return hasMail();
    }

    // -- Pigeonhole

    public @NotNull PigeonholeHandler getPigeonholeHandler() {
        return pigeonholeHandler;
    }

    public boolean pathfindDirectlyTowards(BlockPos pos) {
        getNavigation().setMaxVisitedNodesMultiplier(10.0F);
        getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 2, 1);
        return getNavigation().getPath() != null && getNavigation().getPath().canReach();
    }

    public void pathfindRandomlyTowards(BlockPos pos) {
        Vec3 vec3 = Vec3.atBottomCenterOf(pos);
        int i = 0;
        BlockPos blockPos = this.blockPosition();
        int j = (int) vec3.y - blockPos.getY();
        if (j > 2) {
            i = 4;
        } else if (j < -2) {
            i = -4;
        }

        int k = 6;
        int l = 8;
        int m = blockPos.distManhattan(pos);
        if (m < 15) {
            k = m / 2;
            l = m / 2;
        }

        Vec3 vec32 = AirRandomPos.getPosTowards(this, k, l, i, vec3, (float) (Math.PI / 10));
        if (vec32 != null) {
            this.navigation.setMaxVisitedNodesMultiplier(1.0F);
            this.navigation.moveTo(vec32.x, vec32.y, vec32.z, 1);
        }
    }

    public void releasedFromPigeonhole(BlockPos pos, BlockState state, PigeonholeBlockEntity.ReleaseReason releaseReason) {
        getPigeonholeHandler().setLastPigeonholePos(pos);
        getPigeonholeHandler().setLeftPigeonholeAt(level().getGameTime());
        getPigeonholeHandler().setWouldWantToEnterPigeonholeAfter(getRandom().nextInt(400, 800));
    }

    // -- Sound

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return Envelope.SoundEvents.PIGEON_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return Envelope.SoundEvents.PIGEON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Envelope.SoundEvents.PIGEON_DEATH.get();
    }

    /**
     * Overwritten to use entity instead of entity position.
     * This properly updates sound position for longer sounds and stops ambient sounds when entity dies.
     * I think that might be a bug that Mojang doesn't use entity overload, but maybe it's for some optimization, idk.
     */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!isSilent()) {
            level().playSound(null, this, sound, getSoundSource(), volume, pitch);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(Envelope.SoundEvents.PIGEON_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return getPitch(random) + (isBaby() ? 0.3f : 0);
    }

    public static float getPitch(RandomSource random) {
        return (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F;
    }

    @Override
    public @NotNull SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    // -- Interaction

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void doPush(Entity entity) {
        if (!(entity instanceof Player)) {
            super.doPush(entity);
        }
    }

    // -- Save / Load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getPigeonholeHandler().save(tag);

        tag.putInt("Variant", getVariant().id);
        tag.putBoolean("Sitting", isSitting());

        if (getDelivery() != null) {
            tag.put("Delivery", Delivery.CODEC.encodeStart(NbtOps.INSTANCE, getDelivery()).getOrThrow());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        getPigeonholeHandler().load(tag);
        setVariant(Variant.byId(tag.getInt("Variant")));
        setSitting(tag.getBoolean("Sitting"));
        if (tag.contains("Delivery")) {
            setDelivery(Delivery.CODEC.parse(NbtOps.INSTANCE, tag.get("Delivery")).getOrThrow());
        }
    }

    // -- Variant

    public @NotNull Pigeon.Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT_ID));
    }

    public void setVariant(Variant variant) {
        entityData.set(DATA_VARIANT_ID, variant.id);
    }

    public enum Variant implements StringRepresentable, WeightedEntry {
        GRAY(0, "gray", 12),
        BROWN(1, "brown", 6),
        WHITE(2, "white", 1);

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);
        private static final IntFunction<Variant> BY_ID = ByIdMap.continuous(Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
        public static final WeightedRandomList<Variant> WEIGHTED_LIST = WeightedRandomList.create(Variant.values());

        private final int id;
        private final String name;
        private final Weight weight;

        Variant(int id, String name, int weight) {
            this.id = id;
            this.name = name;
            this.weight = Weight.of(weight);
        }

        public int getId() {
            return this.id;
        }

        @Override
        public @NotNull Weight getWeight() {
            return weight;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }

        public static Variant byId(int id) {
            return BY_ID.apply(id);
        }

        public static Variant getRandom(RandomSource random) {
            return WEIGHTED_LIST.getRandom(random).orElseThrow();
        }
    }

    public boolean hasReachedTarget(BlockPos pos) {
        return hasReachedTarget(pos, 2);
    }

    protected boolean hasReachedTarget(BlockPos pos, int distance) {
        if (closerThan(pos, distance)) {
            return true;
        } else {
            Path path = Pigeon.this.navigation.getPath();
            return path != null && path.getTarget().equals(pos) && path.canReach() && path.isDone();
        }
    }

    public boolean closerThan(BlockPos pos, int distance) {
        return pos.closerThan(blockPosition(), distance);
    }

    // -- Delivery

    public @Nullable Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(@Nullable Delivery delivery) {
        this.delivery = delivery;
    }

    @Override
    public void onDeliveryChanged(ServerLevel level) {
        setHasMail(getDelivery() != null && !getDelivery().getMail().isEmpty());
        BuggerPackets.sendPigeonDelivery(this);
    }

    @Override
    public Optional<BlockPos> getCurrentPos() {
        return Optional.of(blockPosition());
    }

    @Override
    public void advanceDeliveryPhase(ServerLevel level) {
        Preconditions.checkNotNull(getDelivery());

        if (getDelivery().getPhase().getType() == Delivery.Phase.Type.LEAVING_HOME
                && !hasReachedTarget(getDelivery().getPhase().getEnd().orElseThrow())) {
            // Return home if ascent position cannot be reached.
            getDelivery().getPhase()
                    .setType(Delivery.Phase.Type.APPROACHING_HOME)
                    .setTicks(0);

            MailDeliveryLog.addRecords(getDelivery().getMail(),
                    MailDeliveryLog.Record.returned(getDelivery().getRecipient()).atTime(level.getGameTime()));
            return;
        }

        Courier.super.advanceDeliveryPhase(level);
    }

    @Override
    public void startDeliveryPhase(ServerLevel level) {
        Preconditions.checkNotNull(getDelivery());

        getDelivery().getPhase().setStart(blockPosition());

        Envelope.LOGGER.info("Starting phase '{}'", getDelivery().getPhase().getType().getSerializedName());

        switch (getDelivery().getPhase().getType()) {
            case LEAVING_HOME ->
                    getDelivery().getPhase().setEnd(Position.ascent(level, blockPosition(), getDelivery().getRecipientPos()));
            case TRAVELING_TO_TARGET -> {
                getDelivery().getRecipientPos().ifPresent(recipientPos -> {
                    getDelivery().getPhase().setEnd(Position.ascent(level, recipientPos, Optional.of(blockPosition())));
                });
                transitionToBackground(level, true);
            }
            case APPROACHING_TARGET -> getDelivery().getPhase().setEnd(getDelivery().getRecipientPos().orElseThrow());
            case LEAVING_TARGET ->
                    getDelivery().getPhase().setEnd(Position.ascent(level, blockPosition(), getDelivery().getSenderPos()));
            case TRAVELING_TO_HOME -> {
                getDelivery().getSenderPos().ifPresent(senderPos -> {
                    getDelivery().getPhase().setEnd(Position.ascent(level, senderPos, Optional.of(blockPosition())));
                });
                transitionToBackground(level, true);
            }
            case APPROACHING_HOME -> getDelivery().getPhase().setEnd(getDelivery().getSenderPos().orElseThrow());
        }
    }

    @Override
    public void endDeliveryPhase(ServerLevel level) {
        Preconditions.checkNotNull(getDelivery());

        switch (getDelivery().getPhase().getType()) {
            case APPROACHING_TARGET -> {
                ItemStack mail = getDelivery().getMail();
                if (mail.isEmpty()) return;

                if (tryDeliverMail(level, mail, getDelivery().getRecipient())) {
                    getDelivery().setMail(ItemStack.EMPTY);
                } else {
                    mail.set(Envelope.DataComponents.MAIL_STATUS, MailStatus.RETURNED);
                    MailDeliveryLog.addRecords(mail,
                            MailDeliveryLog.Record.returned(getDelivery().getRecipient()).atTime(level.getGameTime()));
                }
            }
            case APPROACHING_HOME -> {
                ItemStack mail = getDelivery().getMail();

                if (!mail.isEmpty() && !tryDeliverMail(level, mail, getDelivery().getSender())) {
                    spawnAtLocation(mail);
                    Envelope.LOGGER.info("Returning mail has been dropped on the ground because it cannot be delivered to Pigeonhole.");
                }
            }
        }
    }

    @Override
    public boolean tryDeliverMail(ServerLevel level, ItemStack mail, Address address) {
        if (Courier.super.tryDeliverMail(level, mail, address)) {
            level().playSound(null, this, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 1, 1);
            return true;
        }
        return false;
    }

    protected void transitionToBackground(ServerLevel level, boolean effects) {
        Envelope.LOGGER.debug("Transitioning a delivering Pigeon to background...");
        BackgroundDelivery.get(level).add(toBackgroundCourier());
        if (effects) {
            level.sendParticles(ParticleTypes.CLOUD, position().x, position().y, position().z, 16, 0.1, 0.1, 0.1, 0.05);
            level.playSound(null, position().x, position().y, position().z,
                    SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
        }
        discard();
    }

    protected BackgroundCourier toBackgroundCourier() {
        return new BackgroundCourier(saveToRecreatableTag().orElse(new CompoundTag()), getDeliveryOrThrow());
    }

    protected Optional<CompoundTag> saveToRecreatableTag() {
        CompoundTag tag = new CompoundTag();

        if (!save(tag)) {
            Envelope.LOGGER.error("Failed to save Pigeon to a tag. " +
                    "Entity is passenger, about to be removed or entity type is not serializable.");
            return Optional.empty();
        }

        Pigeon.IGNORED_TAGS.forEach(tag::remove);
        return Optional.of(tag);
    }
}