package io.github.mortuusars.envelope.world.entity;

import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.ai.goal.PigeonWanderGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

public class Pigeon extends ShoulderRidingEntity implements VariantHolder<Pigeon.Variant>, FlyingAnimal {
    private static final EntityDataAccessor<Integer> DATA_VARIANT_ID = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_HOMING = SynchedEntityData.defineId(Pigeon.class, EntityDataSerializers.BOOLEAN);

    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    private float flapping = 1.0F;
    private float nextFlap = 1.0F;

    public Pigeon(EntityType<? extends Pigeon> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
    }

    public static boolean checkPigeonSpawnRules(EntityType<Pigeon> pigeon, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Config.Server.PIGEON_SPAWNS_NATURALLY.get() && level.getBlockState(pos.below()).is(Envelope.Tags.Blocks.PIGEON_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        setVariant(Variant.getRandom(random));

        if (spawnGroupData == null) {
            spawnGroupData = new AgeableMob.AgeableMobGroupData(false);
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TamableAnimal.TamableAnimalPanicGoal(1.25));
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 5.0F, 1.0F));
        this.goalSelector.addGoal(2, new PigeonWanderGoal(this, 1.0));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FLYING_SPEED, 0.6F)
                .add(Attributes.MOVEMENT_SPEED, 0.2F)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected @NotNull PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level);
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(true);
        flyingPathNavigation.setCanPassDoors(true);
        return flyingPathNavigation;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.calculateFlapping();
    }

    private void calculateFlapping() {
        this.oFlap = this.flap;
        this.oFlapSpeed = this.flapSpeed;
        this.flapSpeed = this.flapSpeed + (float)(!this.onGround() && !this.isPassenger() ? 4 : -1) * 0.3F;
        this.flapSpeed = Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
        if (!this.onGround() && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }

        this.flapping *= 0.9F;
        Vec3 vec3 = this.getDeltaMovement();
        if (!this.onGround() && vec3.y < 0.0) {
            this.setDeltaMovement(vec3.multiply(1.0, 0.6, 1.0));
        }

        this.flap = this.flap + this.flapping * 2.0F;
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).isEmpty() && player.isSecondaryUseActive() && this.isTame()) {
            if (player instanceof ServerPlayer serverPlayer) {
                this.setEntityOnShoulder(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = player.getItemInHand(hand);
        if (!this.isTame() && itemStack.is(ItemTags.PARROT_FOOD)) {
            itemStack.consume(1, player);
            if (!this.isSilent()) {
                this.level()
                        .playSound(
                                null,
                                this.getX(),
                                this.getY(),
                                this.getZ(),
                                Envelope.SoundEvents.PIGEON_EAT.get(),
                                this.getSoundSource(),
                                1.0F,
                                1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
                        );
            }

            if (!this.level().isClientSide) {
                if (this.random.nextInt(10) == 0) {
                    this.tame(player);
                    this.level().broadcastEntityEvent(this, (byte)7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte)6);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else if (!itemStack.is(ItemTags.PARROT_POISONOUS_FOOD)) {
            if (!this.isFlying() && this.isTame() && this.isOwnedBy(player)) {
                if (!this.level().isClientSide) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                }

                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
                return super.mobInteract(player, hand);
            }
        } else {
            itemStack.consume(1, player);
            this.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
            if (player.isCreative() || !this.isInvulnerable()) {
                this.hurt(this.damageSources().playerAttack(player), Float.MAX_VALUE);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean canMate(Animal otherAnimal) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

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
        this.playSound(Envelope.SoundEvents.PIGEON_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    protected boolean isFlapping() {
        return this.flyDist > this.nextFlap;
    }

    @Override
    protected void onFlap() {
        this.playSound(Envelope.SoundEvents.PIGEON_FLY.get(), 0.15F, 1.0F);
        this.nextFlap = this.flyDist + this.flapSpeed / 2.0F;
    }

    @Override
    public float getVoicePitch() {
        return getPitch(this.random);
    }

    public static float getPitch(RandomSource random) {
        return (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F;
    }

    @Override
    public @NotNull SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if (!this.level().isClientSide) {
                this.setOrderedToSit(false);
            }

            return super.hurt(source, amount);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT_ID, 0);
        builder.define(DATA_IS_HOMING, false);
    }

    public @NotNull Pigeon.Variant getVariant() {
        return Pigeon.Variant.byId(this.entityData.get(DATA_VARIANT_ID));
    }

    public void setVariant(Pigeon.Variant variant) {
        entityData.set(DATA_VARIANT_ID, variant.id);
    }

    public boolean isHoming() {
        return entityData.get(DATA_IS_HOMING);
    }

    public void setHoming(boolean homing) {
        entityData.set(DATA_IS_HOMING, homing);
    }

    public boolean hasFancyHat() {
        //TODO: supporters
        // return getOwnerUUID()
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant().id);
        tag.putBoolean("Homing", isHoming());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(Pigeon.Variant.byId(tag.getInt("Variant")));
        setHoming(tag.getBoolean("Homing"));
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    protected boolean canFlyToOwner() {
        return true;
    }

    @Override
    public @NotNull Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5F * getEyeHeight(), getBbWidth() * 0.4F);
    }

    public enum Variant implements StringRepresentable, WeightedEntry {
        GRAY(0, "gray", 12),
        BROWN(1, "brown", 6),
        WHITE(2, "white", 1);

        public static final Codec<Pigeon.Variant> CODEC = StringRepresentable.fromEnum(Pigeon.Variant::values);
        private static final IntFunction<Pigeon.Variant> BY_ID = ByIdMap.continuous(Pigeon.Variant::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
        public static final WeightedRandomList<Variant> WEIGHTED_LIST = WeightedRandomList.create(Variant.values());
        private final int id;
        private final String name;
        private final Weight weight;

        Variant(final int id, final String name, int weight) {
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

        public static Pigeon.Variant byId(int id) {
            return BY_ID.apply(id);
        }

        public static Variant getRandom(RandomSource random) {
            return WEIGHTED_LIST.getRandom(random).orElseThrow();
        }
    }
}
