package io.github.mortuusars.envelope.world.entity;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CharredPigeon extends Monster implements Enemy {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<Boolean> DATA_HAS_MAIL = SynchedEntityData.defineId(CharredPigeon.class, EntityDataSerializers.BOOLEAN);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    protected float flapping = 1.0F;
    protected float nextFlap = 1.0F;

    protected ItemStack carriedMail = ItemStack.EMPTY;
    protected int timeInSafeDimension = 0;

    public CharredPigeon(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 10, false);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    // -- Spawn

    public static boolean checkSpawnRules(EntityType<CharredPigeon> pigeon, LevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Config.Server.CHARRED_PIGEON_SPAWNS_NATURALLY.get();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (level.getRandom().nextDouble() < Config.Server.CHARRED_PIGEON_MAIL_CHANCE.get()) {
            LootTable table = level.getLevel().getServer().reloadableRegistries().getLootTable(Envelope.LootTables.CHARRED_PIGEON_MAIL);
            if (table != LootTable.EMPTY) {
                List<ItemStack> items = new ArrayList<>();
                LootParams lootParams = new LootParams.Builder(level.getLevel())
                      .withParameter(LootContextParams.THIS_ENTITY, this)
                      .withParameter(LootContextParams.ORIGIN, this.position())
                      .create(LootContextParamSets.EQUIPMENT);
                LootContext lootContext = new LootContext.Builder(lootParams)
                      .withOptionalRandomSource(level.getRandom())
                      .create(Optional.empty());
                table.getRandomItems(lootContext, items::add);
                if (!items.isEmpty()) {
                    setCarriedMail(Util.getRandom(items, level.getRandom()));
                }
            }
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
              .add(Attributes.MAX_HEALTH, 8.0)
              .add(Attributes.FLYING_SPEED, 1F)
              .add(Attributes.MOVEMENT_SPEED, 0.2F)
              .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAS_MAIL, false);
    }

    // --

    public boolean hasMail() {
        return entityData.get(DATA_HAS_MAIL);
    }

    public ItemStack getCarriedMail() {
        return carriedMail;
    }

    public void setCarriedMail(ItemStack carriedMail) {
        this.carriedMail = carriedMail;
        entityData.set(DATA_HAS_MAIL, !carriedMail.isEmpty());
    }

    // -- AI

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.5, false));
        goalSelector.addGoal(5, new WanderGoal(this, 1));
        goalSelector.addGoal(8, new FloatGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, true, arg -> Math.abs(arg.getY() - this.getY()) <= 4.0));
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
        calculateFlapping();

        spawnParticle();

        if (Config.Server.CHARRED_PIGEON_CONVERT_INTO_REGULAR.get() && level() instanceof ServerLevel serverLevel) {
            if (canConvert()) {
                timeInSafeDimension++;

                if (!isDeadOrDying() && (isInWaterOrBubble() || timeInSafeDimension > Config.Server.CHARRED_PIGEON_CONVERT_INTO_REGULAR_TICKS.get())) {
                    convert(serverLevel);
                }
            } else {
                timeInSafeDimension = 0;
            }
        }
    }

    protected void spawnParticle() {
        if (level().isClientSide() && level().getRandom().nextInt(3) == 0) {
            ParticleOptions particle = getRandom().nextInt(20) == 0
                  ? ParticleTypes.LAVA
                  : getRandom().nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME;
            Vec3 p = position().add(getViewVector(0).scale(-0.25));
            level().addParticle(particle,
                  p.x + getRandom().nextFloat() * 0.8f - 0.4f,
                  p.y + 0.2 + getRandom().nextFloat() * 0.3f,
                  p.z + getRandom().nextFloat() * 0.8f - 0.4f, 0, 0, 0);
        }
    }

    public boolean canConvert() {
        return !level().dimensionType().ultraWarm() && !isNoAi();
    }

    public void convert(ServerLevel serverLevel) {
        ItemStack carriedMail = getCarriedMail();
        @Nullable Pigeon pigeon = convertTo(Envelope.EntityTypes.PIGEON.get(), true);
        if (pigeon != null) {
            PigeonVariant.get(registryAccess(), PigeonVariant.CHARRED).ifPresentOrElse(pigeon::setVariant,
                  () -> LOGGER.error("Cannot set charred variant when converting to regular pigeon. Variant is not found."));

            if (!carriedMail.isEmpty()) {
                spawnAtLocation(carriedMail);
                setCarriedMail(ItemStack.EMPTY);
            }

            pigeon.setDeltaMovement(getDeltaMovement());
            pigeon.setXRot(getXRot());
            pigeon.setYHeadRot(getYHeadRot());
            pigeon.setYRot(getYRot());
            if (getNavigation().getTargetPos() != null) {
                pigeon.getNavigation().moveTo(pigeon.getNavigation().createPath(getNavigation().getTargetPos(), 1), 1);
            }

            serverLevel.playSound(null, pigeon, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.6f, 1);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, position().x, position().y + 0.2, position().z, 5, 0.3, 0.3, 0.3, 0);
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
            this.setDeltaMovement(vec3.multiply(1, 0.75f, 1));
        }

        this.flap = this.flap + this.flapping * 2.0F;
    }

    @Override
    protected boolean isFlapping() {
        return flyDist > nextFlap;
    }

    @Override
    protected void onFlap() {
        playSound(Envelope.SoundEvents.CHARRED_PIGEON_FLY.get(), 0.15F, 1.0F);
        nextFlap = flyDist + flapSpeed / 2.0F;
    }

    public boolean isFlying() {
        return !this.onGround() && !isPassenger();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (super.doHurtTarget(target)) {
            target.igniteForSeconds((float) Config.Server.CHARRED_PIGEON_IGNITE_SECONDS.getAsDouble());
            return true;
        }
        return false;
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).isEmpty() && !getCarriedMail().isEmpty()) {
            player.setItemInHand(hand, getCarriedMail().copy());
            setCarriedMail(ItemStack.EMPTY);
            level().playSound(null, player, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1, 1);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (damageSource.is(DamageTypes.LAVA) && Bugger.isEnabled()) {
            LOGGER.info("Charred Pigeon has died in lava at: [{}]!", blockPosition().toShortString());
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        if (!getCarriedMail().isEmpty()) {
            spawnAtLocation(getCarriedMail());
            setCarriedMail(ItemStack.EMPTY);
        }
    }

    // -- Sound

    @Nullable
    @Override
    public SoundEvent getAmbientSound() {
        return Envelope.SoundEvents.CHARRED_PIGEON_AMBIENT.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(DamageSource damageSource) {
        return Envelope.SoundEvents.CHARRED_PIGEON_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return Envelope.SoundEvents.CHARRED_PIGEON_DEATH.get();
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
        playSound(Envelope.SoundEvents.CHARRED_PIGEON_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return getPitch(random) + (isBaby() ? 0.3f : 0);
    }

    public static float getPitch(RandomSource random) {
        return (random.nextFloat() - random.nextFloat()) * 0.1F + 1.0F;
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
        if (!getCarriedMail().isEmpty()) tag.put("CarriedMail", getCarriedMail().save(registryAccess()));
        if (timeInSafeDimension > 0) tag.putInt("TimeInSafeDimension", timeInSafeDimension);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCarriedMail(ItemStack.parse(registryAccess(), tag.getCompound("CarriedMail")).orElse(ItemStack.EMPTY));
        timeInSafeDimension = tag.getInt("TimeInSafeDimension");
    }

    // --

    static class WanderGoal extends WaterAvoidingRandomFlyingGoal {
        final CharredPigeon pigeon;

        public WanderGoal(CharredPigeon pigeon, double speedModifier) {
            super(pigeon, speedModifier);
            this.pigeon = pigeon;
            interval = 10;
        }

        @Override
        protected @Nullable Vec3 getPosition() {
            if (pigeon.isInWaterOrBubble()) {
                @Nullable Vec3 pos = LandRandomPos.getPos(pigeon, 15, 15);
                if (pos != null) {
                    return pos;
                }
            }

            Vec3 direction = pigeon.getViewVector(0.0F);

            int range = 8;
            int yRange = 8;

            Vec3 hoverPos = HoverRandomPos.getPos(pigeon, range, yRange,
                  direction.x, direction.z, (float) (Math.PI / 2), 3, 1);
            if (hoverPos != null) {
                return hoverPos;
            }

            return AirAndWaterRandomPos.getPos(pigeon, range, yRange, -1,
                  direction.x, direction.z, (float) (Math.PI / 2));
        }
    }
}
