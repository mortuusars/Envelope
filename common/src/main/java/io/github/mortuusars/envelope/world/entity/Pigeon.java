package io.github.mortuusars.envelope.world.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.delivery.*;
import io.github.mortuusars.envelope.world.delivery.background.BackgroundCourier;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import io.github.mortuusars.envelope.world.entity.ai.goal.*;
import io.github.mortuusars.envelope.world.mail.Mail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
import org.slf4j.Logger;

import java.util.*;
import java.util.function.IntFunction;

public class Pigeon extends Animal implements VariantHolder<Pigeon.Variant>, FlyingAnimal, TransitionableCourier {
    public static final Logger LOGGER = LogUtils.getLogger();

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
          "Sitting",
          "Delivery"
    );

    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DELIVERING = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SERVICE = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_MAIL = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    protected float flapping = 1.0F;
    protected float nextFlap = 1.0F;

    protected PigeonholeHandler pigeonholeHandler;
    protected PigeonDeliveryHandler deliveryHandler = new PigeonDeliveryHandler(this);

    protected @Nullable Delivery delivery = null;
    protected boolean service;

    public Pigeon(EntityType<? extends Pigeon> entityType, Level level) {
        super(entityType, level);
        moveControl = new FlyingMoveControl(this, 10, false);
        pigeonholeHandler = new PigeonholeHandler();
        pigeonholeHandler.setDefaultWantCooldown();
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
    }

    // -- Spawn

    public static boolean checkPigeonSpawnRules(EntityType<Pigeon> pigeon, LevelAccessor level,
                                                MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Config.Server.PIGEON_SPAWNS_NATURALLY.get()
              && level.getBlockState(pos.below()).is(Envelope.Tags.Blocks.PIGEON_SPAWNABLE_ON)
              && isBrightEnoughToSpawn(level, pos);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        setVariant(Variant.getRandom(random));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // -- Variant

    public @NotNull Pigeon.Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT_ID));
    }

    public void setVariant(Variant variant) {
        entityData.set(DATA_VARIANT_ID, variant.id);
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

        getPigeonholeHandler().tick(level());

        if (level() instanceof ServerLevel && tickCount % 20 == 0) {
            if (!getPigeonholeHandler().isPigeonholeValid(level(), blockPosition())) {
                getPigeonholeHandler().setCurrentPos(null);
            }
            Bugger.PIGEON_PIGEONHOLE_HANDLER.send(getId(), getPigeonholeHandler());
        }
    }

    @Override
    public void checkDespawn() {
        // This method seems to be properly called when entity about to be unloaded (when chunk unloads)
        // We need to switch to background for the entity to not freeze in unloaded chunk.
        if (level() instanceof ServerLevel level
              && isDelivering()
              && !Position.isInSafeSimulationDistance(level, blockPosition())) {
            transitionToBackground(level);
        } else {
            super.checkDespawn();
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
            float descendRate = getDescendRate();
            this.setDeltaMovement(vec3.multiply(1, descendRate, 1));
        }

        this.flap = this.flap + this.flapping * 2.0F;
    }

    protected float getDescendRate() {
        if (delivery != null) {
            if (delivery.getCurrentPhase().isDescending()) return 0.9f;
            if (delivery.getCurrentPhase().isAscending()) return 0.5f;
        }
        return 0.75f;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) {
        super.dropAllDeathLoot(level, damageSource);

        if (delivery == null) return;

        String message = damageSource.getLocalizedDeathMessage(this).getString();
        String carriedItem = !delivery.getMail().isEmpty()
              ? " a " + delivery.getMail().getItemForReading().getHoverName().getString()
              : "";
        String addresses = delivery.getSender().getName() + " to " + delivery.getRecipient().getName();

        Envelope.LOGGER.info("{} at [{}] while delivering{} from {}!",
              message, blockPosition().toShortString(), carriedItem, addresses);

        //TODO: send pigeon death notice to sender
//        if (delivery.getSender() instanceof Address.Pigeonhole
//              && level.getEnvelopeContext().addresses().getAll(Address.Type.PIGEONHOLE).isKnown(delivery.getSender())) {
//            ItemStack letter = new ItemStack(Envelope.Items.LETTER.get());
//            letter.set(Envelope.DataComponents.LETTER_SUBJECT, "Courier Death Notice");
//            letter.set(Envelope.DataComponents.LETTER_MESSAGE, message + " at " + "[" + blockPosition().toShortString() + "]");
//
//            letter.set(Envelope.DataComponents.MAIL_SENDER, Address.MAIL_SERVICE);
//            letter.set(Envelope.DataComponents.MAIL_RECIPIENT, delivery.getSender());
//
//            level.getEnvelopeContext().startDelivery(letter);
//        }

        if (!delivery.getMail().isEmpty()) {
            //TODO: COD mail should not drop probably
            spawnAtLocation(delivery.getMail().getItemCopy());
            delivery.setMail(Mail.EMPTY);
        }
    }

    // -- Properties

    public boolean isSitting() {
        return entityData.get(DATA_SITTING);
    }

    public void setSitting(boolean sitting) {
        entityData.set(DATA_SITTING, sitting);
    }

    @Override
    public boolean isDelivering() {
        //TODO: rework
        if (level() instanceof ServerLevel) {
            return delivery != null;
        }
        return entityData.get(DATA_DELIVERING);
    }

    public void setDelivering(boolean delivering) {
        entityData.set(DATA_DELIVERING, delivering);
    }

    public boolean hasMail() {
        return entityData.get(DATA_HAS_MAIL);
    }

    public void setHasMail(boolean hasMail) {
        entityData.set(DATA_HAS_MAIL, hasMail);
    }

    public boolean isService() {
        return entityData.get(DATA_SERVICE);
    }

    public void setService(boolean service) {
        entityData.set(DATA_SERVICE, service);
    }

    public boolean hasFancyHat() {
        //TODO: supporters
        // return getOwnerUUID()
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
              .add(Attributes.MAX_HEALTH, 8.0)
              .add(Attributes.FLYING_SPEED, 2F)
              .add(Attributes.MOVEMENT_SPEED, 0.3F)
              .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT_ID, 0);
        builder.define(DATA_SITTING, false);
        builder.define(DATA_DELIVERING, false);
        builder.define(DATA_SERVICE, false);
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

    public boolean hasReachedTarget(BlockPos pos) {
        return hasReachedTarget(pos, 2);
    }

    protected boolean hasReachedTarget(BlockPos pos, int distance) {
        if (closerThan(pos, distance)) {
            return true;
        } else {
            Path path = getNavigation().getPath();
            return path != null && path.getTarget().equals(pos) && path.canReach() && path.isDone();
        }
    }

    public boolean closerThan(BlockPos pos, int distance) {
        return pos.closerThan(blockPosition(), distance);
    }

    // -- Pigeonhole

    public @NotNull PigeonholeHandler getPigeonholeHandler() {
        return pigeonholeHandler;
    }

    public void setPigeonholeHandler(PigeonholeHandler handler) {
        this.pigeonholeHandler = handler;
    }

    public void releasedFromPigeonhole(BlockPos pos, BlockState state, PigeonholeBlockEntity.ReleaseReason releaseReason) {
        getPigeonholeHandler().setCurrentPos(pos);
        getPigeonholeHandler().setLastReleasePos(pos);
        getPigeonholeHandler().setDefaultWantCooldown();
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

    // -- Courier

    public PigeonDeliveryHandler getDeliveryHandler() {
        return deliveryHandler;
    }

    @Override
    public void continueDelivery(ServerLevel level, Delivery delivery) {
        setDelivery(delivery);
        delivery.adjust(level, getDeliveryHandler());
    }

    public void startDelivery(Delivery delivery) {
        if (this.delivery != null && this.delivery != delivery) {
            LOGGER.warn("Starting new delivery when pigeon is already delivering. This is not might be an error.");
        }
        setDelivery(delivery);
    }

    public Optional<Delivery> getDelivery() {
        return Optional.ofNullable(delivery);
    }

    public void setDelivery(@Nullable Delivery delivery) {
        if (delivery == null && this.delivery == null) {
            return;
        }
        this.delivery = delivery;
        onDeliveryChanged();
    }

    public void onDeliveryChanged() {
        if (!level().isClientSide()) {
            setDelivering(isDelivering());
            setHasMail(delivery != null && !delivery.getMail().isEmpty());

            Bugger.PIGEON_DELIVERY.send(getId(), Optional.ofNullable(delivery));
        }
    }

    @Override
    public void onAppeared(ServerLevel level) {
        level.sendParticles(ParticleTypes.CLOUD, position().x, position().y, position().z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, position().x, position().y, position().z,
              SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
    }

    @Override
    public void onVanished(ServerLevel level) {
        level.sendParticles(ParticleTypes.CLOUD, position().x, position().y, position().z, 16, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, position().x, position().y, position().z,
              SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1, 1);
    }

    public void transitionToBackground(ServerLevel level) {
        if (level.getServer().isStopped() || isRemoved() || !isDelivering()) return;
        if (Envelope.debug()) LOGGER.info("Transitioning delivering Pigeon to background...");
        BackgroundCourier backgroundCourier = toBackgroundCourier();
        level.getEnvelopeContext().getBackgroundDelivery().addCourier(backgroundCourier);
        backgroundCourier.continueDelivery(level, backgroundCourier.delivery());
        onVanished(level);
        discard();
    }

    @Override
    public BackgroundCourier toBackgroundCourier() {
        stopRiding();
        return new BackgroundCourier(
              SpawnableEntityData.of(this, IGNORED_TAGS),
              isService()
                    ? CourierOrigin.service()
                    : CourierOrigin.real(Optional.ofNullable(getPigeonholeHandler().getLastReleasePos()).orElse(blockPosition())),
              getDelivery().orElseThrow());
    }

    // -- Save / Load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("PigeonholeHandler", PigeonholeHandler.CODEC.encode(getPigeonholeHandler(), NbtOps.INSTANCE, tag).getOrThrow());
        tag.putInt("Variant", getVariant().id);
        tag.putBoolean("Sitting", isSitting());

        if (delivery != null) {
            Tag deliveryTag = Delivery.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), delivery)
                  .getOrThrow();
            tag.put("Delivery", deliveryTag);
        }

        if (service) {
            tag.putBoolean("Service", true);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPigeonholeHandler(PigeonholeHandler.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("PigeonholeHandler")).getOrThrow());
        setVariant(Variant.byId(tag.getInt("Variant")));
        setSitting(tag.getBoolean("Sitting"));
        setDelivery(Delivery.parse(tag.getCompound("Delivery"), registryAccess()));
        setService(tag.getBoolean("Service"));

        setDelivering(delivery != null);
        setHasMail(delivery != null && !delivery.getMail().isEmpty());
    }

    // --

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
}