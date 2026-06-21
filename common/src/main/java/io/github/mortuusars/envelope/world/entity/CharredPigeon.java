package io.github.mortuusars.envelope.world.entity;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.EnumSet;

public class CharredPigeon extends Monster implements Enemy {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final EntityDataAccessor<Boolean> DATA_HAS_MAIL = SynchedEntityData.defineId(CharredPigeon.class, EntityDataSerializers.BOOLEAN);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    protected float flapping = 1.0F;
    protected float nextFlap = 1.0F;

    public CharredPigeon(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 10, false);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    // -- Spawn

    public static boolean checkSpawnRules(EntityType<CharredPigeon> pigeon, LevelAccessor level,
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        //TODO: config spawn
        return true;
    }

//    @Override
//    public @NotNull SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
//                                                 MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
//        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
//    }

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

    public void setHasMail(boolean hasMail) {
        entityData.set(DATA_HAS_MAIL, hasMail);
    }

    // -- AI

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        goalSelector.addGoal(5, new WanderGoal(this, 0.8));
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
        this.calculateFlapping();
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

    public boolean isFlying() {
        return !this.onGround() && !isPassenger();
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (damageSource.is(DamageTypes.LAVA)) {
            LOGGER.info("Charred Pigeon has died in lava at: [{}]!", blockPosition().toShortString());
        }
    }

//    @Override
//    protected float getBlockSpeedFactor() {
//        return super.getBlockSpeedFactor();
//    }
//
//    @Override
//    public float getSpeed() {
//        return super.getSpeed();
//    }
//
//    @Override
//    public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 deltaMovement, float friction) {
//        return super.handleRelativeFrictionAndCalculateMovement(deltaMovement, friction);
//    }
//
//    @Override
//    public void moveRelative(float amount, Vec3 relative) {
//        super.moveRelative(amount, relative);
//    }

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
    protected @NotNull SoundEvent getHurtSound(DamageSource damageSource) {
        return Envelope.SoundEvents.PIGEON_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
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
        if (hasMail()) tag.putBoolean("HasMail", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHasMail(tag.getBoolean("HasMail"));
    }

    // --

    static class CharredPigeonMoveControl extends MoveControl {
        private final CharredPigeon pigeon;
        private int floatDuration;

        public CharredPigeonMoveControl(CharredPigeon pigeon) {
            super(pigeon);
            this.pigeon = pigeon;
        }

        public void tick() {
            if (this.operation == Operation.MOVE_TO) {
                if (this.floatDuration-- <= 0) {
                    this.floatDuration += this.pigeon.getRandom().nextInt(5) + 2;
                    Vec3 vec3 = new Vec3(this.wantedX - this.pigeon.getX(), this.wantedY - this.pigeon.getY(), this.wantedZ - this.pigeon.getZ());
                    double d = vec3.length();
                    vec3 = vec3.normalize();
                    if (this.canReach(vec3, Mth.ceil(d))) {
                        this.pigeon.setDeltaMovement(this.pigeon.getDeltaMovement().add(vec3.scale(0.1)));
                    } else {
                        this.operation = Operation.WAIT;
                    }
                }
            }
        }

        private boolean canReach(Vec3 pos, int length) {
            AABB aABB = this.pigeon.getBoundingBox();

            for(int i = 1; i < length; ++i) {
                aABB = aABB.move(pos);
                if (!this.pigeon.level().noCollision(this.pigeon, aABB)) {
                    return false;
                }
            }

            return true;
        }
    }

    static class RandomFloatAroundGoal extends Goal {
        private final CharredPigeon pigeon;

        public RandomFloatAroundGoal(CharredPigeon pigeon) {
            this.pigeon = pigeon;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            MoveControl moveControl = pigeon.getMoveControl();
            if (!moveControl.hasWanted()) {
                return true;
            } else {
                double d = moveControl.getWantedX() - pigeon.getX();
                double e = moveControl.getWantedY() - pigeon.getY();
                double f = moveControl.getWantedZ() - pigeon.getZ();
                double g = d * d + e * e + f * f;
                return g < 1.0 || g > 3600.0;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            RandomSource randomSource = pigeon.getRandom();
            double d = pigeon.getX() + (randomSource.nextFloat() * 2.0F - 1.0F) * 16.0F;
            double e = pigeon.getY() + (randomSource.nextFloat() * 2.0F - 1.0F) * 16.0F;
            double f = pigeon.getZ() + (randomSource.nextFloat() * 2.0F - 1.0F) * 16.0F;
            pigeon.getMoveControl().setWantedPosition(d, e, f, 1.0);
        }
    }

    static class WanderGoal extends WaterAvoidingRandomFlyingGoal {
        final CharredPigeon pigeon;

        public WanderGoal(CharredPigeon pigeon, double speedModifier) {
            super(pigeon, speedModifier);
            this.pigeon = pigeon;
            interval = 50;
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
