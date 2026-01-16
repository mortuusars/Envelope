package io.github.mortuusars.envelope.world.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.occupiable.Occupiable;
import io.github.mortuusars.envelope.world.delivery.*;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import io.github.mortuusars.envelope.world.entity.ai.goal.*;
import io.github.mortuusars.envelope.world.mail.Mail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
    private static final EntityDataAccessor<Boolean> DATA_HAS_MAIL = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TIRED = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    protected float flapping = 1.0F;
    protected float nextFlap = 1.0F;
    protected int tiredTicks;

    protected PigeonholeHandler pigeonholeHandler;
    protected MailboxHandler mailboxHandler;
    protected PigeonDeliveryHandler deliveryHandler;
    protected PigeonDeliverMailGoal deliverMailGoal;

    protected @Nullable Delivery delivery;
    protected @Nullable CourierOrigin origin;

    public Pigeon(EntityType<? extends Pigeon> entityType, Level level) {
        super(entityType, level);
        moveControl = new FlyingMoveControl(this, 10, false);
        pigeonholeHandler = new PigeonholeHandler();
        pigeonholeHandler.setRandomWantCooldownUpToDefault(level.getRandom());
        mailboxHandler = new MailboxHandler();
        deliveryHandler = new PigeonDeliveryHandler(this);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
    }

    // -- Spawn

    public static boolean checkSpawnRules(EntityType<Pigeon> pigeon, LevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Config.Server.PIGEON_SPAWNS_NATURALLY.get()
              && level.getBlockState(pos.below()).is(Envelope.Tags.Blocks.PIGEON_SPAWNABLE_ON)
              && isBrightEnoughToSpawn(level, pos);
    }

    public static Pigeon createService(ServerLevel level) {
        Pigeon pigeon = Objects.requireNonNull(Envelope.EntityTypes.PIGEON.get().create(level),
              "Failed to create an entity. This should not happen.");
        pigeon.setVariant(Variant.getRandom(level.getRandom()));
        pigeon.setOrigin(CourierOrigin.service());
        return pigeon;
    }

    public static Courier spawnServiceCourier(ServerLevel level, Delivery delivery) {
        Pigeon pigeon = createService(level);
        pigeon.startDelivery(delivery);
        Optional<BlockPos> spawnPos = delivery.getRoute().senderPos().map(p -> Position.aboveGround(level, p, 1));
        return spawnPos.filter(pos -> Position.isInSimulationDistance(level, pos))
              .map(pos -> {
                  pigeon.moveTo(
                        (double) pos.getX() + 0.5,
                        (double) pos.getY() + 0.5,
                        (double) pos.getZ() + 0.5,
                        Mth.wrapDegrees(level.random.nextFloat() * 360.0F),
                        0.0F);
                  level.addFreshEntity(pigeon);
                  pigeon.onAppeared(level);
                  return (Courier) pigeon;
              })
              .orElseGet(() -> pigeon.transitionToBackground(level));
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                 MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
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
        deliverMailGoal = new PigeonDeliverMailGoal(this);
        goalSelector.addGoal(0, deliverMailGoal);
        goalSelector.addGoal(1, new PigeonEnterPigeonholeGoal(this));
        goalSelector.addGoal(1, new PigeonStartDeliveryFromMailboxGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.25, itemStack -> itemStack.is(Envelope.Tags.Items.PIGEON_FOOD), false));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.25));
        goalSelector.addGoal(5, new PigeonLocatePigeonholeGoal(this));
        goalSelector.addGoal(5, new PigeonGoToPigeonholeGoal(this));
        goalSelector.addGoal(5, new PigeonLocateMailboxGoal(this));
        goalSelector.addGoal(5, new PigeonGoToMailboxGoal(this));
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

        if (tiredTicks > 0) {
            setTiredTicks(tiredTicks - 1);
            if (level() instanceof ServerLevel serverLevel && level().getRandom().nextFloat() < 0.1) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, position().x, position().y, position().z, 1, 0.2, 0.2, 0.2, 0);
            }
        }

        getPigeonholeHandler().tick(this, level());
        getMailboxHandler().tick(this, level());
    }

    @Override
    public void checkDespawn() {
        // This method seems to be called every tick, even if entity is not in ticking range
        // It's a good place to check if courier should be transitioned to background
        if (level() instanceof ServerLevel level && isDelivering() && !Position.isInSimulationDistance(level, this)) {
            transitionToBackground(level);
            return;
        }

        super.checkDespawn();
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
            if (delivery.getPhase().isDescending()) return 0.9f;
            if (delivery.getPhase().isAscending()) return 0.5f;
        }
        return 0.75f;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) {
        super.dropAllDeathLoot(level, damageSource);

        if (delivery == null) return;

        String message = damageSource.getLocalizedDeathMessage(this).getString();
        String carriedItem = !delivery.getMail().isEmpty()
              ? " a " + delivery.getMail().getItem().getHoverName().getString()
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
            ItemStack droppedMail = delivery.getPhase().ordinal() >= DeliveryPhase.TRAVELING_TO_RECIPIENT.ordinal()
                                    && delivery.getPhase().ordinal() <= DeliveryPhase.DEPARTING_RECIPIENT.ordinal()
                  ? delivery.getMail().asDeliveryResult().getItem()
                  : delivery.getMail().getItem().copy();
            spawnAtLocation(droppedMail);
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

    public boolean isTired() {
        if (!level().isClientSide) {
            return tiredTicks > 0;
        }
        return entityData.get(DATA_TIRED);
    }

    public void setTiredTicks(int ticks) {
        tiredTicks = ticks;
        entityData.set(DATA_TIRED, tiredTicks > 0);
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
        builder.define(DATA_HAS_MAIL, false);
        builder.define(DATA_TIRED, false);
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

    // -- Pigeonhole

    public @NotNull PigeonholeHandler getPigeonholeHandler() {
        return pigeonholeHandler;
    }

    public void setPigeonholeHandler(PigeonholeHandler handler) {
        this.pigeonholeHandler = handler;
    }

    public void releasedFromPigeonhole(BlockPos pos, BlockState state, Occupiable.ReleaseReason releaseReason) {
        getPigeonholeHandler().setTargetPos(pos);
        getPigeonholeHandler().setLastReleasePos(pos);
        getPigeonholeHandler().setDefaultWantCooldown();

        if (releaseReason == Occupiable.ReleaseReason.EMERGENCY) {
            getPigeonholeHandler().setEnterCooldown(200);
        }

        getMailboxHandler().setLocateCooldown(level().getRandom().nextInt(20, MailboxHandler.DEFAULT_LOCATE_COOLDOWN));

        if (isTired()) {
            setTiredTicks(0);
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, position().x, position().y, position().z, 5, 0.25, 0.25, 0.25, 0);
            }
        }
    }

    // -- Mailbox

    public MailboxHandler getMailboxHandler() {
        return mailboxHandler;
    }

    public Pigeon setMailboxHandler(MailboxHandler mailboxHandler) {
        this.mailboxHandler = mailboxHandler;
        return this;
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

    @Override
    public DeliveryHandler getDeliveryHandler() {
        return deliveryHandler;
    }

    public boolean canStartDelivery() {
        return !isTired() && !level().isNight() && !level().isRaining() && !level().isThundering();
    }

    public Courier startDelivery(Delivery delivery) {
        if (this.delivery != null && this.delivery != delivery) {
            LOGGER.warn("Starting new delivery when pigeon is already delivering. This might be an error.");
        }
        if (origin == null || !origin.isService()) {
            setOrigin(CourierOrigin.regular(blockPosition()));
        }
        stopRiding();
        setDelivery(delivery);
        return this;
    }

    public Optional<Delivery> getCurrentDelivery() {
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

    public @NotNull CourierOrigin getOrigin() {
        if (origin == null) {
            LOGGER.warn("Origin of a Pigeon was not set properly. Current position will be used as origin instead.");
            origin = CourierOrigin.regular(blockPosition());
        }
        return origin;
    }

    public void setOrigin(@Nullable CourierOrigin origin) {
        this.origin = origin;
    }

    @Override
    public SpawnableEntityData toSpawnableData() {
        return SpawnableEntityData.of(this, IGNORED_TAGS);
    }

    // -- Save / Load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("PigeonholeHandler", PigeonholeHandler.CODEC.encode(getPigeonholeHandler(), NbtOps.INSTANCE, tag).getOrThrow());
        tag.put("MailboxHandler", MailboxHandler.CODEC.encode(getMailboxHandler(), NbtOps.INSTANCE, tag).getOrThrow());
        tag.putInt("Variant", getVariant().id);
        tag.putBoolean("Sitting", isSitting());
        if (tiredTicks > 0) tag.putInt("TiredTicks", tiredTicks);

        if (delivery != null) {
            Delivery.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), delivery)
                  .resultOrPartial(LOGGER::error)
                  .ifPresent(value -> tag.put("Delivery", value));
        }
        if (origin != null) {
            CourierOrigin.CODEC.encodeStart(NbtOps.INSTANCE, origin)
                  .resultOrPartial(LOGGER::error)
                  .ifPresent(value -> tag.put("Origin", value));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPigeonholeHandler(PigeonholeHandler.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("PigeonholeHandler")).getOrThrow());
        setMailboxHandler(MailboxHandler.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("MailboxHandler")).getOrThrow());
        setVariant(Variant.byId(tag.getInt("Variant")));
        setSitting(tag.getBoolean("Sitting"));
        setTiredTicks(tag.getInt("TiredTicks"));

        if (tag.contains("Delivery")) {
            setDelivery(Delivery.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.getCompound("Delivery"))
                  .resultOrPartial(e -> LOGGER.error("Cannot parse Delivery from tag '{}': {}", tag.getCompound("Delivery"), e))
                  .orElse(null)
            );
        }

        if (tag.contains("Origin")) {
            origin = CourierOrigin.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("Origin"))
                  .resultOrPartial(e -> LOGGER.error("Cannot parse CourierOrigin from tag '{}': {}", tag.getCompound("Origin"), e))
                  .orElse(null);
        }

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