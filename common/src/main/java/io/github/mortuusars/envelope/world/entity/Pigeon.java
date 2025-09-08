package io.github.mortuusars.envelope.world.entity;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.mail.log.MailDeliveryLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.world.Addresses;
import io.github.mortuusars.envelope.world.DeliveringPigeons;
import io.github.mortuusars.envelope.world.PigeonholeNetwork;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
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
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Pigeon extends Animal implements VariantHolder<Pigeon.Variant>, FlyingAnimal, DeliveringPigeon {
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
            "cannot_enter_pigeonhole_ticks",
            "pigeonhole_pos"
    );
    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_SITTING = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Delivery> DATA_DELIVERY = SynchedEntityData.defineId(Pigeon.class, Delivery.ENTITY_DATA_SERIALIZER);
    private static final int COOLDOWN_BEFORE_LOCATING_NEW_PIGEONHOLE = 200;

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    protected float flapping = 1.0F;
    protected float nextFlap = 1.0F;

    protected @Nullable BlockPos pigeonholePos;
    protected @Nullable BlockPos lastPigeonholePos;
    protected long leftPigeonholeAt;
    protected int wouldWantToEnterPigeonholeAfter = random.nextInt(200, 600);
    protected int remainingCooldownBeforeLocatingNewPigeonhole;
    protected GoToPigeonholeGoal goToPigeonholeGoal;

    public Pigeon(EntityType<? extends Pigeon> entityType, Level level) {
        super(entityType, level);
        moveControl = new PigeonFlyingMoveControl(this, 10, false);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
    }

    // -- Spawn

    public static boolean checkPigeonSpawnRules(EntityType<Pigeon> pigeon, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Config.Server.PIGEON_SPAWNS_NATURALLY.get() && level.getBlockState(pos.below()).is(Envelope.Tags.Blocks.PIGEON_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
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
        goalSelector.addGoal(0, new DeliverMailGoal());
        goalSelector.addGoal(1, new EnterPigeonholeGoal());
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.25, itemStack -> itemStack.is(Envelope.Tags.Items.PIGEON_FOOD), false));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.25));
        goalSelector.addGoal(5, new LocatePigeonholeGoal());
        goToPigeonholeGoal = new GoToPigeonholeGoal();
        goalSelector.addGoal(5, goToPigeonholeGoal);
        goalSelector.addGoal(6, new PigeonWanderGoal());
        goalSelector.addGoal(7, new FloatGoal(this));
    }

    @Override
    protected @NotNull PathNavigation createNavigation(Level level) {
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

        if (!this.level().isClientSide) {
            if (this.tickCount % 20 == 0 && !this.isPigeonholeValid()) {
                this.setPigeonholePos(null);
            }
        }
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
            this.setDeltaMovement(vec3.multiply(1.0, 0.8, 1.0));
        }

        this.flap = this.flap + this.flapping * 2.0F;
    }

    public void unloaded(ServerLevel level) {
        if (getRemovalReason() != null) return;
        if (!isDelivering()) return;

        Envelope.LOGGER.info("Transitioning a delivering Pigeon to background...");
        transitionToBackground(level, false);
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
        builder.define(DATA_DELIVERY, Delivery.EMPTY);
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

    public long getLeftPigeonholeAt() {
        return leftPigeonholeAt;
    }

    public long getTicksSinceLeftPigeonhole() {
        return level().getGameTime() - leftPigeonholeAt;
    }

    public boolean isSitting() {
        return entityData.get(DATA_IS_SITTING);
    }

    public void setSitting(boolean sitting) {
        entityData.set(DATA_IS_SITTING, sitting);
    }

    public Delivery getDelivery() {
        return entityData.get(DATA_DELIVERY);
    }

    public void setDelivery(Delivery data) {
        entityData.set(DATA_DELIVERY, data, true);
    }

    public boolean hasFancyHat() {
        //TODO: supporters
        // return getOwnerUUID()
        return true;
    }

    // -- Pigeonhole

    public @Nullable BlockPos getPigeonholePos() {
        return pigeonholePos;
    }

    public void setPigeonholePos(@Nullable BlockPos pigeonholePos) {
        this.pigeonholePos = pigeonholePos;
    }

    public @Nullable BlockPos getLastPigeonholePos() {
        return lastPigeonholePos;
    }

    public Optional<PigeonholeBlockEntity> getPigeonhole() {
        BlockPos pos = getPigeonholePos();
        if (pos != null && level().isLoaded(pos) && level().getBlockEntity(pos) instanceof PigeonholeBlockEntity be) {
            return Optional.of(be);
        }
        return Optional.empty();
    }

    protected boolean wantsToEnterPigeonhole() {
        if (getTicksSinceLeftPigeonhole() < 200) return false; // Cooldown

        boolean wantsToEnter = (level().isNight() || level().isThundering())
                || (level().getGameTime() >= leftPigeonholeAt + wouldWantToEnterPigeonholeAfter);

        return wantsToEnter && !isPigeonholeNearFire();

        // Probably better to send a signal to pickup mail from Pigeonhole itself.
        // Because doing it here will cause all nearby pigeons to go into pigeonhole.
        // return getPigeonhole().filter(PigeonholeBlockEntity::hasMailToDeliver).orElse(false);
    }

    protected boolean isPigeonholeNearFire() {
        return getPigeonhole().map(PigeonholeBlockEntity::isFireNearby).orElse(false);
    }

    protected boolean closerThan(BlockPos pos, int distance) {
        return pos.closerThan(blockPosition(), distance);
    }

    protected boolean isTooFarAway(BlockPos pos) {
        return !this.closerThan(pos, 32);
    }

    protected boolean isPigeonholeValid() {
        BlockPos pos = getPigeonholePos();
        if (pos == null) return false;
        if (isTooFarAway(pos)) return false;
        return level().getBlockEntity(pos) instanceof PigeonholeBlockEntity;
    }

    protected void pathfindRandomlyTowards(BlockPos pos) {
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
            this.navigation.setMaxVisitedNodesMultiplier(0.5F);
            this.navigation.moveTo(vec32.x, vec32.y, vec32.z, 1.0);
        }
    }

    public void releasedFromPigeonhole(BlockPos pos, BlockState state, PigeonholeBlockEntity.ReleaseReason releaseReason) {
        lastPigeonholePos = pos;
        leftPigeonholeAt = level().getGameTime();
        wouldWantToEnterPigeonholeAfter = getRandom().nextInt(400, 800);
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
        if (getPigeonholePos() != null) {
            tag.put("PigeonholePos", NbtUtils.writeBlockPos(getPigeonholePos()));
        }
        if (lastPigeonholePos != null) {
            tag.put("LastPigeonholePos", NbtUtils.writeBlockPos(lastPigeonholePos));
        }
        tag.putLong("LeftPigeonholeAt", leftPigeonholeAt);
        tag.putInt("WouldWantToEnterPigeonholeAfter", wouldWantToEnterPigeonholeAfter);
        tag.putInt("Variant", getVariant().id);
        tag.putBoolean("Sitting", isSitting());

        if (!getDelivery().isEmpty()) {
            tag.put("Delivery", Delivery.CODEC.encodeStart(NbtOps.INSTANCE, getDelivery()).getOrThrow());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPigeonholePos(NbtUtils.readBlockPos(tag, "PigeonholePos").orElse(null));
        lastPigeonholePos = NbtUtils.readBlockPos(tag, "LastPigeonholePos").orElse(null);
        leftPigeonholeAt = tag.getLong("LeftPigeonholeAt");
        wouldWantToEnterPigeonholeAfter = tag.getInt("WouldWantToEnterPigeonholeAfter");
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

    // --

    class LocatePigeonholeGoal extends Goal {
        @Override
        public boolean canUse() {
            Pigeon pigeon = Pigeon.this;
            return pigeon.remainingCooldownBeforeLocatingNewPigeonhole == 0
                    && pigeon.getPigeonholePos() == null
                    && pigeon.wantsToEnterPigeonhole();
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            Pigeon pigeon = Pigeon.this;

            pigeon.remainingCooldownBeforeLocatingNewPigeonhole = COOLDOWN_BEFORE_LOCATING_NEW_PIGEONHOLE;
            List<BlockPos> pigeonholes = findNearbyPigeonholesWithSpace();
            if (!pigeonholes.isEmpty()) {
                for (BlockPos pos : pigeonholes) {
                    if (!pigeon.goToPigeonholeGoal.isTargetBlacklisted(pos)) {
                        pigeon.setPigeonholePos(pos);
                        return;
                    }
                }

                pigeon.goToPigeonholeGoal.clearBlacklist();
                pigeon.setPigeonholePos(pigeonholes.getFirst());
            }
        }

        private List<BlockPos> findNearbyPigeonholesWithSpace() {
            Pigeon pigeon = Pigeon.this;
            BlockPos pos = pigeon.blockPosition();
            PoiManager poiManager = ((ServerLevel) pigeon.level()).getPoiManager();
            Stream<PoiRecord> stream = poiManager.getInRange(holder -> holder.is(Envelope.PoiTypes.PIGEONHOLE), pos, 20, PoiManager.Occupancy.ANY);
            return stream.map(PoiRecord::getPos)
                    .filter(p -> level().getBlockEntity(p) instanceof PigeonholeBlockEntity pigeonhole && pigeonhole.hasSpace())
                    .sorted(Comparator.comparingDouble(p -> p.distSqr(pos)))
                    .collect(Collectors.toList());
        }
    }

    public class GoToPigeonholeGoal extends Goal {
        public static final int MAX_TRAVELLING_TICKS = 600;
        int travellingTicks = Pigeon.this.level().random.nextInt(10);
        private static final int MAX_BLACKLISTED_TARGETS = 3;
        final List<BlockPos> blacklistedTargets = new ArrayList<>();
        @Nullable
        private Path lastPath;
        private static final int TICKS_BEFORE_PIGEONHOLE_DROP = 60;
        private int ticksStuck;

        GoToPigeonholeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Pigeon pigeon = Pigeon.this;
            return pigeon.getPigeonholePos() != null
                    && !pigeon.hasRestriction()
                    && pigeon.wantsToEnterPigeonhole()
                    && !this.hasReachedTarget(pigeon.getPigeonholePos())
                    && pigeon.level().getBlockState(pigeon.getPigeonholePos()).is(Envelope.Tags.Blocks.PIGEONHOLES);
        }

        @Override
        public void start() {
            travellingTicks = 0;
            ticksStuck = 0;
            super.start();
        }

        @Override
        public void stop() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
            Pigeon.this.navigation.stop();
            Pigeon.this.navigation.resetMaxVisitedNodesMultiplier();
        }

        @Override
        public void tick() {
            Pigeon pigeon = Pigeon.this;
            if (pigeon.getPigeonholePos() == null) return;

            travellingTicks++;
            if (travellingTicks > adjustedTickDelay(MAX_TRAVELLING_TICKS)) {
                dropAndBlacklistPigeonhole();
            } else if (!pigeon.navigation.isInProgress()) {
                if (!pigeon.closerThan(getPigeonholePos(), 16)) {
                    if (pigeon.isTooFarAway(getPigeonholePos())) {
                        dropPigeonhole();
                    } else {
                        pigeon.pathfindRandomlyTowards(getPigeonholePos());
                    }
                } else {
                    boolean bl = pathfindDirectlyTowards(getPigeonholePos());
                    if (!bl) {
                        dropAndBlacklistPigeonhole();
                    } else if (lastPath != null && lastPath.sameAs(pigeon.navigation.getPath())) {
                        ticksStuck++;
                        if (ticksStuck > TICKS_BEFORE_PIGEONHOLE_DROP) {
                            dropPigeonhole();
                            ticksStuck = 0;
                        }
                    } else {
                        lastPath = pigeon.navigation.getPath();
                    }
                }
            }
        }

        private boolean pathfindDirectlyTowards(BlockPos pos) {
            Pigeon.this.navigation.setMaxVisitedNodesMultiplier(10.0F);
            Pigeon.this.navigation.moveTo(pos.getX(), pos.getY(), pos.getZ(), 2, 1.0);
            return Pigeon.this.navigation.getPath() != null && Pigeon.this.navigation.getPath().canReach();
        }

        boolean isTargetBlacklisted(BlockPos pos) {
            return this.blacklistedTargets.contains(pos);
        }

        private void blacklistTarget(BlockPos pos) {
            this.blacklistedTargets.add(pos);

            while (this.blacklistedTargets.size() > MAX_BLACKLISTED_TARGETS) {
                this.blacklistedTargets.removeFirst();
            }
        }

        void clearBlacklist() {
            this.blacklistedTargets.clear();
        }

        private void dropAndBlacklistPigeonhole() {
            if (Pigeon.this.getPigeonholePos() != null) {
                blacklistTarget(Pigeon.this.getPigeonholePos());
            }

            dropPigeonhole();
        }

        private void dropPigeonhole() {
            Pigeon.this.setPigeonholePos(null);
            Pigeon.this.remainingCooldownBeforeLocatingNewPigeonhole = COOLDOWN_BEFORE_LOCATING_NEW_PIGEONHOLE;
        }

        private boolean hasReachedTarget(BlockPos pos) {
            if (Pigeon.this.closerThan(pos, 2)) {
                return true;
            } else {
                Path path = Pigeon.this.navigation.getPath();
                return path != null && path.getTarget().equals(pos) && path.canReach() && path.isDone();
            }
        }
    }

    class EnterPigeonholeGoal extends Goal {
        @Override
        public boolean canUse() {
            Pigeon pigeon = Pigeon.this;
            BlockPos pos = pigeon.getPigeonholePos();

            if (pos != null
                    && pigeon.wantsToEnterPigeonhole()
                    && pos.closerToCenterThan(pigeon.position(), 2.0)
                    && level().getBlockEntity(pos) instanceof PigeonholeBlockEntity be) {
                if (!be.isFull()) {
                    return true;
                }

                pigeon.setPigeonholePos(null);
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            getPigeonhole().ifPresent(pigeonhole -> pigeonhole.addOccupant(Pigeon.this));
        }
    }

    class PigeonWanderGoal extends Goal {
        private static final int WANDER_THRESHOLD = 22;

        PigeonWanderGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Pigeon.this.navigation.isDone() && Pigeon.this.random.nextInt(10) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Pigeon.this.navigation.isInProgress();
        }

        @Override
        public void start() {
            Vec3 pos = this.findPos();
            if (pos != null) {
                Pigeon.this.navigation.moveTo(Pigeon.this.navigation.createPath(BlockPos.containing(pos), 1), 1.0);
            }
        }

        @Nullable
        private Vec3 findPos() {
            Vec3 pos;
            BlockPos pigeonholePos = Pigeon.this.getPigeonholePos();
            if (Pigeon.this.isPigeonholeValid() && pigeonholePos != null && !Pigeon.this.closerThan(pigeonholePos, WANDER_THRESHOLD)) {
                Vec3 pigeonholeCenter = Vec3.atCenterOf(pigeonholePos);
                pos = pigeonholeCenter.subtract(Pigeon.this.position()).normalize();
            } else {
                pos = Pigeon.this.getViewVector(0.0F);
            }

            int radius = 8;
            Vec3 vec33 = HoverRandomPos.getPos(Pigeon.this, radius, 12, pos.x, pos.z, (float) (Math.PI / 2), 3, 1);
            return vec33 != null ? vec33 : AirAndWaterRandomPos.getPos(Pigeon.this, radius, 4, -2, pos.x, pos.z, (float) (Math.PI / 2));
        }
    }

    class DeliverMailGoal extends Goal {
        DeliverMailGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isDelivering();
        }

        @Override
        public void stop() {
            setDelivery(Delivery.EMPTY);
            Pigeon.this.navigation.stop();
            Pigeon.this.navigation.resetMaxVisitedNodesMultiplier();
        }

        @Override
        public void tick() {
            if (!(level() instanceof ServerLevel level)) return;
            if (getDelivery().isEmpty()) return;

            if (getDelivery().tick()) {
                advancePhase(level);
                return;
            }

            getDelivery().getCurrentPhase().end().ifPresent(pos -> {
                if (hasReachedTarget(pos)) {
                    advancePhase(level);
                } else if (!Pigeon.this.navigation.isInProgress()) {
                    if (!pathfindDirectlyTowards(pos)) {
                        pathfindRandomlyTowards(pos);
                    }
                }
            });
        }

        private boolean pathfindDirectlyTowards(BlockPos pos) {
            Pigeon.this.navigation.setMaxVisitedNodesMultiplier(10.0F);
            Pigeon.this.navigation.moveTo(pos.getX(), pos.getY(), pos.getZ(), 2, 2);
            return Pigeon.this.navigation.getPath() != null && Pigeon.this.navigation.getPath().canReach();
        }

        private boolean hasReachedTarget(BlockPos pos) {
            if (Pigeon.this.closerThan(pos, 2)) {
                return true;
            } else {
                Path path = Pigeon.this.navigation.getPath();
                return path != null && path.getTarget().equals(pos) && path.canReach() && path.isDone();
            }
        }
    }

    public boolean isDelivering() {
        return !getDelivery().isEmpty();
    }

    public void startDelivery(ServerLevel level, ItemStack mail, @Nullable BlockPos homePos) {
        mail.remove(Envelope.DataComponents.MAIL_DELIVERY_LOG); // Remove previous log before new send

        Address sender = mail.get(Envelope.DataComponents.MAIL_SENDER);
        Address recipient = mail.get(Envelope.DataComponents.MAIL_RECIPIENT);
        int travelDuration = mail.getOrDefault(Envelope.DataComponents.MAIL_TRAVEL_DURATION, Config.Server.TRAVEL_DURATION.get());

        MailDeliveryLog.addRecords(mail,
                TravelingRecord.sentFrom(sender).atTime(level.getGameTime()),
                TravelingRecord.travelingTo(recipient));

        BlockPos targetPos = blockPosition();

        Optional<BlockPos> position = Addresses.getPosition(level, recipient);
        if (position.isPresent()) {
            if (Math.sqrt(position.get().distSqr(blockPosition())) < 48 && level.isLoaded(position.get())) {
                setDelivery(new Delivery(mail, sender, recipient, travelDuration, Optional.ofNullable(homePos),
                        Delivery.Phase.BEGINNING
                                .ofType(Delivery.Phase.Type.APPROACHING_TARGET)
                                .start(blockPosition())
                                .end(position.get())));
                return;
            }
            targetPos = getPosInTheDirectionOf(blockPosition(), position.get(), 16);
        }

        setDelivery(new Delivery(mail, sender, recipient, travelDuration, Optional.ofNullable(homePos),
                Delivery.Phase.BEGINNING
                        .start(blockPosition())
                        .end(targetPos))); //TODO: pos in the direction of address
    }

    protected BlockPos getPosInTheDirectionOf(BlockPos current, BlockPos target, double distance) {
        Vec3 entityVec = Vec3.atCenterOf(current);
        Vec3 targetVec = Vec3.atCenterOf(target);

        Vec3 direction = targetVec.subtract(entityVec).normalize();
        Vec3 newPosVec = entityVec.add(direction.scale(distance));

        return BlockPos.containing(newPosVec);
    }

    public void advancePhase(ServerLevel level) {
        Preconditions.checkState(!getDelivery().isEmpty(), "Cannot advance delivery phase: Pigeon is not delivering.");

        switch (getDelivery().getCurrentPhase().type()) {
            case LEAVING_HOME -> {
                nextDeliveryPhase()
                        .startAt(blockPosition())
                        .endAt(Addresses.getPosition(level, getDelivery().getRecipient()).orElse(BlockPos.ZERO))
                        .duration(getDelivery().getTravelDuration())
                        .begin();
                transitionToBackground(level, true);
            }
            case APPROACHING_TARGET -> {
                nextDeliveryPhase()
                        .startAt(blockPosition())
                        .endAt(blockPosition().above(16))
                        .begin();

                ItemStack mail = getMail();
                if (mail.isEmpty()) return;

                getDelivery().getRecipient()
                        .ifPigeonhole(pigeonhole -> {
                            PigeonholeNetwork pigeonholeNetwork = PigeonholeNetwork.get(level);
                            if (pigeonholeNetwork.putMail(pigeonhole, mail)) {
                                MailDeliveryLog.addRecords(mail, TravelingRecord.arrivedTo(pigeonhole));

                                pigeonholeNetwork.getPositionOf(pigeonhole).ifPresent(pos -> {
                                    if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                                        blockEntity.onMailDelivered(level, mail);
                                    }
                                });

                                setMail(ItemStack.EMPTY);
                            } else {
                                MailDeliveryLog.addRecords(mail, TravelingRecord.returned(pigeonhole).atTime(level.getGameTime()));
                                MailDeliveryLog.addRecords(mail, TravelingRecord.travelingTo(getDelivery().getSender()));
                            }
                        })
                        .ifPlayer(player -> {
                            throw new NotImplementedException("Player addresses are not implemented yet");
                        })
                        .ifNpc(npc -> {
                            throw new NotImplementedException("NPC addresses are not implemented yet");
                        });
                setDelivery(getDelivery());
            }
            case LEAVING_TARGET -> {
                nextDeliveryPhase()
                        .startAt(blockPosition())
                        .endAt(getDelivery().getHomePos().orElse(BlockPos.ZERO))
                        .begin();

//                if (getDelivery().getHomePos().isPresent()) {
//                    BlockPos targetPos = blockPosition();
//
//                    Optional<BlockPos> position = getDelivery().getHomePos().get();
//                    if (position.isPresent()) {
//                        if (Math.sqrt(position.get().distSqr(blockPosition())) < 48 && level.isLoaded(position.get())) {
//                            setDelivery(new Delivery(mail, sender, recipient, travelDuration, Optional.ofNullable(homePos),
//                                    Delivery.Phase.BEGINNING
//                                            .ofType(Delivery.Phase.Type.APPROACHING_TARGET)
//                                            .start(blockPosition())
//                                            .end(position.get())));
//                            return;
//                        }
//                        targetPos = getPosInTheDirectionOf(blockPosition(), position.get(), 16);
//                    }
//                }

                transitionToBackground(level, true);
            }
            case APPROACHING_HOME -> {
                ItemStack mail = getMail();
                if (!mail.isEmpty()) {
                    getDelivery().getSender()
                            .ifPigeonhole(pigeonhole -> {
                                MailDeliveryLog.addRecords(mail, TravelingRecord.arrivedTo(getDelivery().getSender()));
                                PigeonholeNetwork pigeonholeNetwork = PigeonholeNetwork.get(level);

                                if (pigeonholeNetwork.putMail(pigeonhole, mail)) {
                                    pigeonholeNetwork.getPositionOf(pigeonhole).ifPresent(pos -> {
                                        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity blockEntity) {
                                            blockEntity.onMailDelivered(level, mail);
                                        }
                                    });
                                } else {
                                    spawnAtLocation(mail);
                                    Envelope.LOGGER.warn("Dropped returning mail on the ground as there's no Pigeonhole to put it in.");
                                }
                            })
                            .ifPlayer(player -> {
                                throw new IllegalStateException("Player senders should not be a thing.");
                            })
                            .ifNpc(npc -> {
                                throw new IllegalStateException("NPC sender return to home should not be handled by alive Pigeons.");
                            });
                }
                setDelivery(Delivery.EMPTY);
            }
            case TRAVELING_TO_TARGET, TRAVELING_TO_HOME ->
                    throw new IllegalStateException("Traveling phases are handled in background.");
            default -> throw new IllegalStateException("Unexpected value: " + getDelivery().getCurrentPhase().type());
        }
    }

    protected void transitionToBackground(ServerLevel level, boolean effects) {
        Preconditions.checkState(!getDelivery().isEmpty(), "Cannot transition to background: Pigeon is not delivering.");
        DeliveringPigeons.get(level).add(this);
        if (effects) {
            level.sendParticles(ParticleTypes.CLOUD, position().x, position().y, position().z, 16, 0.1, 0.1, 0.1, 0.05);
            level.playSound(null, position().x, position().y, position().z,
                    SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
        }
        discard();
    }
}
