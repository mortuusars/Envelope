package io.github.mortuusars.envelope.world.entity;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.occupiable.Occupiable;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.entity.ai.PigeonNavigation;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import io.github.mortuusars.envelope.world.entity.ai.goal.*;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableEntityData;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.delivery.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

public class Pigeon extends Animal implements VariantHolder<Holder<PigeonVariant>>, FlyingAnimal, PhysicalCourier {
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

    public static final Predicate<LivingEntity> AVOID_SELECTOR = entity -> {
        if (entity instanceof Cat) return Config.Server.PIGEON_HUNTED_BY_CAT.get();
        if (entity instanceof Ocelot) return Config.Server.PIGEON_HUNTED_BY_OCELOT.get();
        if (entity instanceof Fox) return Config.Server.PIGEON_HUNTED_BY_FOX.get();
        return false;
    };

    public static final Predicate<ItemEntity> SEARCHED_FOOD_ITEMS_PREDICATE = item ->
          item.isAlive() && !item.hasPickUpDelay() && item.getItem().is(Envelope.Tags.Items.PIGEON_FOOD);

    public static final Predicate<Mob> FOLLOW_PREDICATE = mob -> Config.Server.PIGEON_EATS_SEEDS.get()
          && Config.Server.VILLAGER_FEEDING_PIGEONS.get()
          && mob instanceof Villager villager
          && (!Config.Server.VILLAGER_FEEDING_PIGEONS_NITWIT_ONLY.get()
          || villager.getVillagerData().getProfession() == VillagerProfession.NITWIT)
          && mob.getRandom().nextInt(60) == 0;

    private static final EntityDataAccessor<Holder<PigeonVariant>> DATA_VARIANT = SynchedEntityData.defineId(Pigeon.class, Envelope.EntityDataSerializers.PIGEON_VARIANT.get());
    private static final EntityDataAccessor<Boolean> DATA_DELIVERING = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_MAIL = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SERVICE = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TIRED_TICKS = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EATING_TICKS = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.INT);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    protected float flapping = 1.0F;
    protected float nextFlap = 1.0F;

    protected PigeonholeHandler pigeonholeHandler;
    protected MailboxHandler mailboxHandler;

    protected PigeonWanderGoal wanderGoal;

    protected @Nullable Delivery delivery;
    protected @Nullable CourierOrigin origin;
    private PigeonAvoidEntityGoal<Animal> avoidEntityGoal;

    public Pigeon(EntityType<? extends Pigeon> entityType, Level level) {
        super(entityType, level);
        moveControl = new FlyingMoveControl(this, 10, false);
        pigeonholeHandler = new PigeonholeHandler();
        mailboxHandler = new MailboxHandler();
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        setCanPickUpLoot(true);
    }

    // -- Spawn

    public static boolean checkSpawnRules(EntityType<Pigeon> pigeon, LevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Config.Server.PIGEON_SPAWNS_NATURALLY.get()
              && level.getBlockState(pos.below()).is(Envelope.Tags.Blocks.PIGEONS_SPAWNABLE_ON)
              && isBrightEnoughToSpawn(level, pos);
    }

    public static Pigeon createService(ServerLevel level) {
        Pigeon pigeon = Objects.requireNonNull(Envelope.EntityTypes.PIGEON.get().create(level),
              "Failed to create an entity. This should not happen.");
        pigeon.setVariant(PigeonVariant.getRandomServiceVariant(level.registryAccess(), level.getRandom()));
        pigeon.setOrigin(CourierOrigin.service());
        return pigeon;
    }

    public static Courier spawnServiceCourier(ServerLevel level, Delivery delivery) {
        Pigeon pigeon = createService(level);
        pigeon.startDelivery(delivery);
        return pigeon.transitionToBackground(level);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                 MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        getPigeonholeHandler().setRandomWantCooldownUpToDefault(getRandom());
        setVariant(PigeonVariant.getRandomSpawnVariant(level.registryAccess(), getRandom(), level.getBiome(blockPosition())));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
              .add(Attributes.MAX_HEALTH, 8.0)
              .add(Attributes.FLYING_SPEED, 1F)
              .add(Attributes.MOVEMENT_SPEED, 0.4F)
              .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, PigeonVariant.withFallback(registryAccess(), Optional.empty()));
        builder.define(DATA_DELIVERING, false);
        builder.define(DATA_SERVICE, false);
        builder.define(DATA_HAS_MAIL, false);
        builder.define(DATA_TIRED_TICKS, 0);
        builder.define(DATA_EATING_TICKS, 0);
    }

    // -- Variant

    public @NotNull Holder<PigeonVariant> getVariant() {
        return entityData.get(DATA_VARIANT);
    }

    public void setVariant(Holder<PigeonVariant> variant) {
        entityData.set(DATA_VARIANT, variant);
    }

    // -- AI

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PigeonDeliverMailGoal(this));
        goalSelector.addGoal(1, avoidEntityGoal = new PigeonAvoidEntityGoal<>(this, Animal.class,
              5, 0.5, 0.6, AVOID_SELECTOR));
        goalSelector.addGoal(2, new PigeonPanicGoal(this, 3.5));
        goalSelector.addGoal(2, new PigeonEnterPigeonholeGoal(this));
        goalSelector.addGoal(3, new PigeonStartDeliveryFromMailboxGoal(this));
        goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        goalSelector.addGoal(5, new TemptGoal(this, 1.25, itemStack -> itemStack.is(Envelope.Tags.Items.PIGEON_FOOD), false));
        goalSelector.addGoal(6, new FollowParentGoal(this, 1.25));
        goalSelector.addGoal(7, new PigeonLocatePigeonholeGoal(this));
        goalSelector.addGoal(7, new PigeonGoToPigeonholeGoal(this));
        goalSelector.addGoal(7, new PigeonLocateMailboxGoal(this));
        goalSelector.addGoal(7, new PigeonSitGoal(this));
        goalSelector.addGoal(8, new PigeonGoToMailboxGoal(this));
        goalSelector.addGoal(9, new PigeonSearchForFoodGoal(this));
        goalSelector.addGoal(9, new FollowSpecificMobGoal(this, FOLLOW_PREDICATE, 0.5, 3, 8));
        goalSelector.addGoal(10, wanderGoal = new PigeonWanderGoal(this, 0.5));
        goalSelector.addGoal(11, new FloatGoal(this));
        goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(13, new LookAtPlayerGoal(this, Villager.class, 8.0F, 0.01F));
    }

    @Override
    protected @NotNull FlyingPathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new PigeonFlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.calculateFlapping();

        getPigeonholeHandler().tick(this, level());
        getMailboxHandler().tick(this, level());

        if (getTiredTicks() > 0) {
            setTiredTicks(getTiredTicks() - 1);

            if (level().getRandom().nextInt(32) == 0) {
                level().addParticle(ParticleTypes.SMOKE, position().x, position().y, position().z, 0, 0, 0);
            }
        }

        if (isSitting() && getNavigation().isInProgress()) {
            setSitting(false);
        }

        ItemStack mainHandItem = getMainHandItem();
        if (isFood(mainHandItem)) {
            setEatingTicks(getEatingTicks() + 1);

            if (getEatingTicks() == 5) {
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, mainHandItem),
                          position().x, position().y + 0.3, position().z, 5, 0.25, 0.25, 0.25, 0);
                }
                playSound(getEatingSound(mainHandItem), 0.5f, getRandom().nextFloat() * 0.2f + 0.9f);
            }

            if (getEatingTicks() >= 10) {
                setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                setEatingTicks(0);
            }
        } else if (getEatingTicks() > 10) {
            setEatingTicks(0);
        }
    }

    @Override
    public void checkDespawn() {
        // This method seems to be called every tick, even if entity is not in ticking range
        // It's a good place to check if courier should be transitioned to background
        if (level() instanceof ServerLevel level && !isNoAi() && isDelivering() && !Position.isInSimulationDistance(level, this)) {
            transitionToBackground(level);
            return;
        }

        super.checkDespawn();
    }

    @Override
    public boolean isPersistenceRequired() {
        // Not sure if this changes much, but just to make sure.
        return super.isPersistenceRequired() || isDelivering() || getPigeonholeHandler().getHomePos() != null;
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
    public boolean wantsToPickUp(ItemStack stack) {
        return Config.Server.PIGEON_EATS_SEEDS.get()
              && onGround()
              && getMainHandItem().isEmpty()
              && isFood(stack)
              && getEatingTicks() <= 0;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        return wantsToPickUp(stack);
    }

    @Override
    public boolean canHoldItem(ItemStack stack) {
        return wantsToPickUp(stack);
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        if (getRandom().nextInt(10) != 0) {
            return;
        }
        ItemStack item = itemEntity.getItem();
        if (itemEntity.onGround() && distanceTo(itemEntity) < 1 && canHoldItem(item)) {
            if (item.getCount() > 1) {
                spawnAtLocation(item.split(item.getCount() - 1));
            }

            onItemPickup(itemEntity);
            setItemSlot(EquipmentSlot.MAINHAND, item.split(1));
            take(itemEntity, item.getCount());
            itemEntity.discard();
            setEatingTicks(0);
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return super.shouldDropLoot() && (origin == null || !origin.isService());
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) {
        super.dropAllDeathLoot(level, damageSource);

        getCurrentDelivery().ifPresent(delivery -> {
            diedWhileDelivering(level, damageSource, delivery);
        });
    }

    protected void diedWhileDelivering(ServerLevel level, DamageSource damageSource, Delivery delivery) {
        String message = damageSource.getLocalizedDeathMessage(this).getString();
        String carriedItem = !delivery.getMail().isEmpty()
              ? " a " + delivery.getMail().getHoverName().getString()
              : "";
        String addresses = delivery.getSender().getString()
              + " to "
              + delivery.getRecipient().getString();
        String service = getOrigin().isService() ? "Service " : "";
        Envelope.LOGGER.info("{}{} at [{}] while delivering{} from {}!", service, message, blockPosition().toShortString(), carriedItem, addresses);

        if (!getOrigin().isService()) {
            MailService.of(level).sendCourierDeathNotice(this, delivery, damageSource);
        }

        if (!delivery.getMail().isEmpty()) {
            ItemStack mail = delivery.getPhase().isOnRecipientSide()
                  ? Mail.asDelivered(delivery.getMail())
                  : delivery.getMail();
            spawnAtLocation(mail);
            delivery.setMail(ItemStack.EMPTY);
        }
    }

    // -- Properties

    public boolean isSitting() {
        return getPose() == Pose.SITTING;
    }

    public void setSitting(boolean sitting) {
        setPose(sitting ? Pose.SITTING : Pose.STANDING);
    }

    @Override
    public boolean isDelivering() {
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

    public boolean isTired() {
        return getTiredTicks() > 0;
    }

    public int getTiredTicks() {
        return entityData.get(DATA_TIRED_TICKS);
    }

    public void setTiredTicks(int ticks) {
        if (ticks <= 0 && getTiredTicks() > 0 && level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                  position().x, position().y + getBoundingBox().getYsize() / 2f, position().z,
                  7, 0.25, 0.25, 0.25, 0);
        }

        entityData.set(DATA_TIRED_TICKS, ticks);
    }

    public int getEatingTicks() {
        return entityData.get(DATA_EATING_TICKS);
    }

    public void setEatingTicks(int ticks) {
        entityData.set(DATA_EATING_TICKS, ticks);
    }

    public boolean hasMailmanHat() {
        return isService();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        @Nullable Pigeon offspring = Envelope.EntityTypes.PIGEON.get().create(level);
        if (offspring != null && otherParent instanceof Pigeon otherPigeon) {
            offspring.setVariant(getRandom().nextBoolean() ? this.getVariant() : otherPigeon.getVariant());
        }
        return offspring;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Envelope.Tags.Items.PIGEON_FOOD);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) return false;
        if (isDeadOrDying()) return false;

        if (getRandom().nextDouble() < Config.Server.PIGEON_DAMAGE_EVASION_CHANCE_WHILE_DELIVERING.get()
              && !source.is(Envelope.Tags.DamageTypes.BYPASSES_PIGEON_DELIVERY_EVASION)) {

            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.POOF, position().x, position().y, position().z, 3, 0.3, 0.3, 0.3, 0);
                level.playSound(null, this, SoundEvents.ALLAY_THROW, SoundSource.NEUTRAL, 1,
                      getRandom().nextFloat() * 0.1f + 0.95f);
            }

            return false;
        }

        return super.hurt(source, amount);
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
        return !this.onGround() && !isPassenger();
    }

    @Override
    protected float getFlyingSpeed() {
        if (isPanicking() || avoidEntityGoal.isAvoiding()) return 0.030f;
        if (isTired()) return 0.0175f;
        return 0.0275f;
    }

    @Override
    public @NotNull Vec3 getLeashOffset() {
        return new Vec3(0.0, getEyeHeight() * 0.65F, getBbWidth() * 0.4F);
    }

    public boolean hasReachedTarget(BlockPos localPos) {
        return PigeonNavigation.hasReachedTarget(this, localPos, PigeonNavigation.DEFAULT_REACH_DISTANCE);
    }

    protected boolean hasReachedTarget(BlockPos localPos, double distance) {
        return PigeonNavigation.hasReachedTarget(this, localPos, distance);
    }

    public boolean closerThan(BlockPos localPos, double distance) {
        return PigeonNavigation.isWithinReach(this, localPos, distance);
    }

    public boolean pathfindDirectlyTowards(BlockPos localPos) {
        BlockPos navigationPos = PigeonNavigation.getNavigationPos(this, localPos);
        getNavigation().setMaxVisitedNodesMultiplier(10.0F);
        getNavigation().moveTo(navigationPos.getX(), navigationPos.getY(), navigationPos.getZ(), 2, 1);
        return getNavigation().getPath() != null && getNavigation().getPath().canReach();
    }

    public void pathfindRandomlyTowards(BlockPos localPos) {
        Vec3 vec3 = Position.getGlobalCenter(level(), localPos).subtract(0, 0.5, 0);
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
        int m = blockPos.distManhattan(Position.getNavigationPos(level(), localPos));
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
        if (releaseReason == Occupiable.ReleaseReason.EMERGENCY) {
            getPigeonholeHandler().setEnterCooldown(200);
        } else {
            getPigeonholeHandler().setTargetPos(pos);
            getPigeonholeHandler().setHomePos(pos);
            getPigeonholeHandler().setDefaultWantCooldown();

            if (isTired()) {
                setTiredTicks(0);
                if (level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, position().x, position().y, position().z, 5, 0.25, 0.25, 0.25, 0);
                }
            }
        }

        getPigeonholeHandler().setTicksSinceLastRest(0);

        // Immediately going for mailbox after release looks weird
        getMailboxHandler().setLocateCooldown(level().getRandom().nextInt(20, MailboxHandler.DEFAULT_LOCATE_COOLDOWN));

        // Force wandering immediately, instead of standing (or falling) in front of the Pigeonhole
        // - looks especially stupid if there's an emergency
        wanderGoal.trigger();
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
    public @NotNull SoundEvent getEatingSound(ItemStack stack) {
        return Envelope.SoundEvents.PIGEON_EAT.get();
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
        return (random.nextFloat() - random.nextFloat()) * 0.1F + 1.0F;
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

    public boolean canStartDelivery() {
        return !isLeashed() && !isTired() && !level().isNight() && !level().isRaining() && !level().isThundering();
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
            setDelivering(getCurrentDelivery().isPresent());
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
        setService(origin != null && origin.isService());
    }

    @Override
    public SpawnableEntityData toSpawnableData() {
        return SpawnableEntityData.of(this, IGNORED_TAGS);
    }

    @Override
    public int getPhaseDuration(ServerLevel level, Delivery delivery, DeliveryPhase phase) {
        return switch (phase) {
            // Longer approach/depart phases to allow for pathfinding to finish
            case DEPARTING_SENDER, APPROACHING_RECIPIENT, DEPARTING_RECIPIENT, APPROACHING_SENDER -> 30 * Ticks.SECOND;
            default -> PhysicalCourier.super.getPhaseDuration(level, delivery, phase);
        };
    }

    @Override
    public void phaseStarted(ServerLevel level, Delivery delivery) {
        PhysicalCourier.super.phaseStarted(level, delivery);
        if (delivery.getPhase().isTraveling()) {
            transitionToBackground(level);
        }
        onDeliveryChanged();
    }

    @Override
    public boolean handlePhaseTransition(ServerLevel level, Delivery delivery) {
        if (delivery.getPhase() == DeliveryPhase.DEPARTING_SENDER && !hasReachedSegmentEndPos(delivery)) {
            Mail.returned(delivery.getMail(), DeliveryRecord.Message.UNABLE_TO_REACH);
            delivery.beginPhase(DeliveryPhase.APPROACHING_SENDER);
            return true;
        }

        if (delivery.getPhase() == DeliveryPhase.APPROACHING_RECIPIENT && !hasReachedSegmentEndPos(delivery)) {
            Mail.returned(delivery.getMail(), DeliveryRecord.Message.UNABLE_TO_REACH);
            delivery.beginPhase(DeliveryPhase.DEPARTING_RECIPIENT);
            return true;
        }

        return PhysicalCourier.super.handlePhaseTransition(level, delivery);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (!delivery.getMail().isEmpty()) {
            spawnAtLocation(delivery.getMail().copy());
            Pigeon.LOGGER.info("{} has dropped undelivered mail on the ground.", getName().getString());
            delivery.setMail(ItemStack.EMPTY);
        }

        setDelivery(null);

        if (getOrigin().isService()) {
            onVanished(level);
            discard();
        } else {
            setOrigin(null);
            getPigeonholeHandler().setTargetPos(getPigeonholeHandler().getHomePos());
            // Prevent Pigeon entering Pigeonhole immediately:
            getPigeonholeHandler().setWantCooldown(20);
            setTiredTicks(Config.Server.PIGEON_TIRED_AFTER_DELIVERY_TICKS.get());
        }
    }

    protected boolean hasReachedSegmentEndPos(Delivery delivery) {
        return delivery.getRoute().getSegment(delivery.getPhase()).endPos()
              .map(endPos -> hasReachedTarget(
                    PigeonNavigation.getSegmentApproachTarget(level(), endPos, delivery.getPhase())))
              .orElse(true);
    }

    // -- Save / Load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("PigeonholeHandler", PigeonholeHandler.CODEC.encode(getPigeonholeHandler(), NbtOps.INSTANCE, new CompoundTag()).getOrThrow());
        tag.put("MailboxHandler", MailboxHandler.CODEC.encode(getMailboxHandler(), NbtOps.INSTANCE, new CompoundTag()).getOrThrow());
        getVariant()
              .unwrapKey()
              .ifPresent(key -> tag.putString("Variant", key.location().toString()));
        if (isSitting()) tag.putBoolean("Sitting", true);
        if (getTiredTicks() > 0) tag.putInt("TiredTicks", getTiredTicks());
        if (getEatingTicks() > 0) tag.putInt("EatingTicks", getEatingTicks());

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

        PigeonholeHandler.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("PigeonholeHandler"))
              .resultOrPartial(e -> LOGGER.error("Cannot parse PigeonholeHandler from tag '{}': {}", tag.getCompound("PigeonholeHandler"), e))
              .ifPresent(this::setPigeonholeHandler);
        MailboxHandler.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("MailboxHandler"))
              .resultOrPartial(e -> LOGGER.error("Cannot parse MailboxHandler from tag '{}': {}", tag.getCompound("MailboxHandler"), e))
              .ifPresent(this::setMailboxHandler);

        String variant = tag.contains("Variant", Tag.TAG_INT)
              ? PigeonVariant.fromLegacyId(tag.getInt("Variant"))
              : tag.getString("Variant");

        Optional.ofNullable(ResourceLocation.tryParse(variant))
              .map(key -> ResourceKey.create(Envelope.Registries.PIGEON_VARIANT, key))
              .flatMap(key -> registryAccess().registryOrThrow(Envelope.Registries.PIGEON_VARIANT)
                    .getHolder(key))
              .ifPresent(this::setVariant);

        setSitting(tag.getBoolean("Sitting"));
        setTiredTicks(tag.getInt("TiredTicks"));
        setEatingTicks(tag.getInt("EatingTicks"));

        if (tag.contains("Delivery")) {
            setDelivery(Delivery.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.getCompound("Delivery"))
                  .resultOrPartial(e -> LOGGER.error("Cannot parse Delivery from tag '{}': {}", tag.getCompound("Delivery"), e))
                  .orElse(null)
            );
        }

        if (tag.contains("Origin")) {
            setOrigin(CourierOrigin.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("Origin"))
                  .resultOrPartial(e -> LOGGER.error("Cannot parse CourierOrigin from tag '{}': {}", tag.getCompound("Origin"), e))
                  .orElse(null));
        }

        setDelivering(delivery != null);
        setHasMail(delivery != null && !delivery.getMail().isEmpty());
    }
}