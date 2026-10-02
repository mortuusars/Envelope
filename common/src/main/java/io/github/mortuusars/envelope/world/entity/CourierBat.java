package io.github.mortuusars.envelope.world.entity;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.entity.ai.CourierNavigation;
import io.github.mortuusars.envelope.world.entity.ai.MailboxHandler;
import io.github.mortuusars.envelope.world.entity.ai.goal.courier.*;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableEntityData;
import io.github.mortuusars.envelope.world.mail.delivery.CourierOrigin;
import io.github.mortuusars.envelope.world.mail.delivery.CourierProperties;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import io.github.mortuusars.mortaar.bugger.Bugger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class CourierBat extends PathfinderMob implements FlyingAnimal, PhysicalCourier {
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
          "Delivery"
    );

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<Boolean> DATA_DELIVERING = SynchedEntityData.defineId(CourierBat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_MAIL = SynchedEntityData.defineId(CourierBat.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState flyAnimationState = new AnimationState();

    protected MailboxHandler mailboxHandler = new MailboxHandler();

    protected @Nullable Delivery delivery;

    protected @Nullable BlockPos spawnPos;
    protected int deliveries;

    public CourierBat(EntityType<? extends CourierBat> entityType, Level level) {
        super(entityType, level);
        moveControl = new FlyingMoveControl(this, 10, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
              .add(Attributes.MAX_HEALTH, 6.0)
              .add(Attributes.FLYING_SPEED, 1F)
              .add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    public static CourierBat createService(ServerLevel level) {
        return Objects.requireNonNull(Envelope.EntityTypes.COURIER_BAT.get().create(level),
              "Failed to create an entity. This should not happen.");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DELIVERING, false);
        builder.define(DATA_HAS_MAIL, false);
    }

    // -- Mailbox

    public MailboxHandler getMailboxHandler() {
        return mailboxHandler;
    }

    public void setMailboxHandler(MailboxHandler mailboxHandler) {
        this.mailboxHandler = mailboxHandler;
    }

    // -- Properties

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

    public @Nullable BlockPos getSpawnPos() {
        return spawnPos;
    }

    public CourierBat setSpawnPos(@Nullable BlockPos pos) {
        this.spawnPos = pos;
        return this;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {

    }

    protected Entity.@NotNull MovementEmission getMovementEmission() {
        return MovementEmission.EVENTS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    protected void pushEntities() {
    }

    public boolean isIgnoringBlockTriggers() {
        return true;
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
        return super.isPersistenceRequired() || isDelivering();
    }

    /**
     * Implementing FlyingAnimal makes significant difference, as some movement code (in LivingEntity) executes differently if entity is one.
     * Without it, Bat turns back occasionally making its flight look weird.<br>
     * The method itself doesn't really make a difference, but Bat is flying - so returning 'true' here wouldn't hurt.
     */
    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    protected float getFlyingSpeed() {
        return 0.04f;
    }

    public boolean isFlapping() {
        return (float) tickCount % 10.0F == 0.0F;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // -- Loot

    @Override
    protected @NotNull ResourceKey<LootTable> getDefaultLootTable() {
        return EntityType.BAT.getDefaultLootTable();
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) {
        super.dropAllDeathLoot(level, damageSource);
        getCurrentDelivery().ifPresent(delivery -> diedWhileDelivering(level, damageSource, delivery));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) return false;
        if (isDeadOrDying()) return false;

        if (getRandom().nextDouble() < Config.Server.DELIVERY_COURIER_DAMAGE_EVASION_CHANCE.get()
              && !source.is(Envelope.Tags.DamageTypes.BYPASSES_COURIER_DELIVERY_EVASION)) {

            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.POOF, position().x, position().y, position().z, 3, 0.3, 0.3, 0.3, 0);
                level.playSound(null, this, SoundEvents.ALLAY_THROW, SoundSource.NEUTRAL, 1,
                      getRandom().nextFloat() * 0.1f + 0.95f);
            }

            return false;
        }

        return super.hurt(source, amount);
    }

    // -- Sound

    @Override
    protected float getSoundVolume() {
        return 0.1F;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.95F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {

    }

    @Override
    @Nullable
    public SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    // -- AI

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new DespawnGoal());
        goalSelector.addGoal(0, new LeaveGoal(this));
        goalSelector.addGoal(0, new DeliverMailGoal(this));
        goalSelector.addGoal(1, new StartDeliveryFromMailboxGoal(this));
        goalSelector.addGoal(2, new GoToMailboxGoal(this));
        goalSelector.addGoal(3, new LocateMailboxGoal(this, 32, 1f));

        WaterAvoidingRandomFlyingGoal wanderGoal = new WaterAvoidingRandomFlyingGoal(this, 1f);
        wanderGoal.setInterval(1);
        goalSelector.addGoal(4, wanderGoal);
        goalSelector.addGoal(10, new FloatGoal(this));
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
    public void tick() {
        super.tick();
        flyAnimationState.startIfStopped(this.tickCount);
    }

    @Override
    public void aiStep() {
        if (isNoAi()) {
            return;
        }

        getMailboxHandler().tick(this, level());

        if (spawnPos == null && !isDelivering() && getMailboxHandler().getTargetPos() == null) {
            spawnPos = Position.ascendTowards(level(), blockPosition(),
                  Optional.empty(), Config.Server.DELIVERY_ASCEND_DISTANCE.get(), getId());
        }

        super.aiStep();
    }

    // -- Courier

    @Override
    public boolean canEat(ItemStack food) {
        return food.is(Envelope.Tags.Items.BAT_FOOD);
    }

    @Override
    public CourierProperties getCourierProperties() {
        return new CourierProperties(Config.Server.BAT_DELIVERY_TRAVEL_SPEED.get());
    }

    @Override
    public ParticleOptions getTransitionParticle() {
        return ParticleTypes.LARGE_SMOKE;
    }

    @Override
    public boolean canStartDelivery() {
        return deliveries < Config.Server.BAT_MAX_DELIVERIES.get() && level().isNight() && !level().isRaining() && !level().isThundering();
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

        if (!level().isClientSide() && Bugger.isEnabled()) {
            Envelope.BuggerData.COURIER_DELIVERY.send(getId(), Optional.ofNullable(delivery));
        }
    }

    public void onDeliveryChanged() {
        if (!level().isClientSide()) {
            setDelivering(getCurrentDelivery().isPresent());
            setHasMail(delivery != null && !delivery.getMail().isEmpty());
        }
    }

    @Override
    public @NotNull CourierOrigin getCourierOrigin() {
        return CourierOrigin.service();
    }

    @Override
    public SpawnableEntityData toSpawnableCourierData() {
        return SpawnableEntityData.of(this, IGNORED_TAGS);
    }

    @Override
    public void endDelivery(ServerLevel level, Delivery delivery) {
        if (!delivery.getMail().isEmpty()) {
            spawnAtLocation(delivery.getMail().copy());
            LOGGER.info("{} has dropped undelivered mail on the ground.", getName().getString());
            delivery.setMail(ItemStack.EMPTY);
        }

        setDelivery(null);

        deliveries++;

        getMailboxHandler().setTargetPos(null);
        if (spawnPos == null) {
            spawnPos = Position.ascendTowards(level, blockPosition(),
                  Optional.empty(), Config.Server.DELIVERY_ASCEND_DISTANCE.get(), getId());
        }
        playAmbientSound();
    }

    public void despawn() {
        if (level() instanceof ServerLevel level) {
            onVanished(level);
            discard();
        }
    }

    // -- Save / Load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("MailboxHandler", MailboxHandler.CODEC.encode(getMailboxHandler(), NbtOps.INSTANCE, new CompoundTag()).getOrThrow());
        if (delivery != null) {
            Delivery.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), delivery)
                  .resultOrPartial(LOGGER::error)
                  .ifPresent(value -> tag.put("Delivery", value));
        }

        if (deliveries > 0) {
            tag.putInt("Deliveries", deliveries);
        }

        if (spawnPos != null) {
            tag.put("SpawnPos", NbtUtils.writeBlockPos(spawnPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        MailboxHandler.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("MailboxHandler"))
              .resultOrPartial(e -> LOGGER.error("Cannot parse MailboxHandler from tag '{}': {}", tag.getCompound("MailboxHandler"), e))
              .ifPresent(this::setMailboxHandler);

        if (tag.contains("Delivery")) {
            setDelivery(Delivery.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.getCompound("Delivery"))
                  .resultOrPartial(e -> LOGGER.error("Cannot parse Delivery from tag '{}': {}", tag.getCompound("Delivery"), e))
                  .orElse(null)
            );
        }

        setDelivering(delivery != null);
        setHasMail(delivery != null && !delivery.getMail().isEmpty());

        deliveries = tag.getInt("Deliveries");
        spawnPos = NbtUtils.readBlockPos(tag, "SpawnPos").orElse(null);
    }

    public class LeaveGoal extends AbstractGoToBlockGoal {
        private Path lastPath;
        private int unreachableTicks;

        public LeaveGoal(PhysicalCourier courier) {
            super(courier);
        }

        @Override
        public @Nullable BlockPos getBlockPos() {
            return spawnPos;
        }

        @Override
        public boolean canUse() {
            if (isDelivering()) return false;
            return super.canUse() && (!canStartDelivery() || getMailboxHandler().getTargetPos() == null);
        }

        @Override
        public void tick() {
            BlockPos target = getBlockPos();

            if (target == null) {
                return;
            }

            travellingTicks++;
            if (travellingTicks > adjustedTickDelay(600)) {
                spawnPos = null;
            } else if (!getNavigation().isInProgress()) {
                if (!closerThan(target, 20)) {
                    pathfindRandomlyTowards(target);
                } else {
                    boolean canReach = pathfindDirectlyTowards(target);
                    if (!canReach) {
                        unreachableTicks++;
                        if (unreachableTicks > 20) {
                            despawn();
                        }
                    } else if (lastPath != null && Objects.requireNonNull(getNavigation().getPath()).sameAs(lastPath)) {
                        // Fixes bat sometimes not being able to reach the target while being just under it.
                        if (hasReachedTarget(target, 2.5)) {
                            despawn();
                        }

                        ticksStuck++;
                        if (ticksStuck > 40) {
                            spawnPos = null;
                            ticksStuck = 0;
                        }
                    } else {
                        lastPath = getNavigation().getPath();
                    }
                }
            }
        }
    }

    public class DespawnGoal extends Goal {
        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            if (isDelivering()) return false;
            return (!canStartDelivery() || getMailboxHandler().getTargetPos() == null)
                  && (spawnPos == null || hasReachedTarget(spawnPos, CourierNavigation.getReachDistance() + 1));
        }

        @Override
        public void start() {
            despawn();
        }
    }
}
