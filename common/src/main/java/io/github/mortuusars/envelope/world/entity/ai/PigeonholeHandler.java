package io.github.mortuusars.envelope.world.entity.ai;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PigeonholeHandler {
    protected static final int MAX_BLACKLISTED_TARGETS = 3;
    public static final int COOLDOWN_BEFORE_LOCATING_NEW_PIGEONHOLE = 200;

    protected final Level level;
    protected final List<BlockPos> blacklistedPositions = new ArrayList<>();

    protected @Nullable BlockPos pigeonholePos;
    protected int cooldownBeforeEnteringPigeonhole;
    protected int cooldownBeforeWantingToEnterPigeonhole;
    protected int cooldownBeforeLocatingNewPigeonhole;

    public PigeonholeHandler(Level level) {
        this.level = level;
    }

    public @Nullable BlockPos getPigeonholePos() {
        return pigeonholePos;
    }

    public void setPigeonholePos(@Nullable BlockPos pigeonholePos) {
        this.pigeonholePos = pigeonholePos;
    }

    public int getCooldownBeforeEnteringPigeonhole() {
        return cooldownBeforeEnteringPigeonhole;
    }

    public void setCooldownBeforeEnteringPigeonhole(int cooldown) {
        this.cooldownBeforeEnteringPigeonhole = cooldown;
    }

    public int getCooldownBeforeWantingToEnterPigeonhole() {
        return cooldownBeforeWantingToEnterPigeonhole;
    }

    public void setCooldownBeforeWantingToEnterPigeonhole(int cooldown) {
        this.cooldownBeforeWantingToEnterPigeonhole = cooldown;
    }

    public void setDefaultCooldownBeforeWantingToEnterPigeonhole() {
        cooldownBeforeWantingToEnterPigeonhole = Config.Server.PIGEON_MIN_TICKS_OUTSIDE_PIGEONHOLE.get();
    }

    public int getCooldownBeforeLocatingNewPigeonhole() {
        return cooldownBeforeLocatingNewPigeonhole;
    }

    public void setCooldownBeforeLocatingNewPigeonhole(int cooldown) {
        this.cooldownBeforeLocatingNewPigeonhole = cooldown;
    }

    public void setDefaultCooldownBeforeLocatingNewPigeonhole() {
        setCooldownBeforeLocatingNewPigeonhole(COOLDOWN_BEFORE_LOCATING_NEW_PIGEONHOLE);
    }

    // --

    public void tick() {
        if (cooldownBeforeWantingToEnterPigeonhole > 0) {
            cooldownBeforeWantingToEnterPigeonhole--;
        }
        if (cooldownBeforeLocatingNewPigeonhole > 0) {
            cooldownBeforeLocatingNewPigeonhole--;
        }
    }

    // --

    public Optional<PigeonholeBlockEntity> getPigeonholeBlockEntity() {
        BlockPos pos = getPigeonholePos();
        if (pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity be) {
            return Optional.of(be);
        }
        return Optional.empty();
    }

    public boolean wantsToEnterPigeonhole() {
        if (getCooldownBeforeEnteringPigeonhole() > 0) return false;
        boolean wouldHateToBeOutside = level.isNight() || level.isRaining() || level.isThundering();
        boolean tiredOfOutside = getCooldownBeforeWantingToEnterPigeonhole() <= 0;
        return (wouldHateToBeOutside || tiredOfOutside) && !isPigeonholeNearFire();
    }

    protected boolean isPigeonholeNearFire() {
        return getPigeonholeBlockEntity().map(PigeonholeBlockEntity::isFireNearby).orElse(false);
    }

    public boolean isPigeonholeValid(BlockPos currentPos) {
        BlockPos pos = getPigeonholePos();
        if (pos == null) return false;
        if (!currentPos.closerThan(pos, 32)) return false;
        return level.getBlockEntity(pos) instanceof PigeonholeBlockEntity;
    }

    public boolean isTargetBlacklisted(BlockPos pos) {
        return this.blacklistedPositions.contains(pos);
    }

    private void blacklistTarget(BlockPos pos) {
        this.blacklistedPositions.add(pos);

        while (this.blacklistedPositions.size() > MAX_BLACKLISTED_TARGETS) {
            this.blacklistedPositions.removeFirst();
        }
    }

    public void clearBlacklist() {
        this.blacklistedPositions.clear();
    }

    public void dropAndBlacklistPigeonhole() {
        if (getPigeonholePos() != null) {
            blacklistTarget(getPigeonholePos());
        }

        dropPigeonhole();
    }

    public void dropPigeonhole() {
        setPigeonholePos(null);
        setDefaultCooldownBeforeLocatingNewPigeonhole();
    }

    public void load(CompoundTag tag) {
        pigeonholePos = NbtUtils.readBlockPos(tag, "PigeonholePos").orElse(null);
        cooldownBeforeEnteringPigeonhole = tag.getInt("CooldownBeforeEnteringPigeonhole");
        cooldownBeforeWantingToEnterPigeonhole = tag.getInt("CooldownBeforeWantingToEnterPigeonhole");
        cooldownBeforeLocatingNewPigeonhole = tag.getInt("CooldownBeforeLocatingNewPigeonhole");
    }

    public void save(CompoundTag tag) {
        if (getPigeonholePos() != null) tag.put("PigeonholePos", NbtUtils.writeBlockPos(getPigeonholePos()));
        if (cooldownBeforeEnteringPigeonhole > 0) tag.putInt("CooldownBeforeEnteringPigeonhole", cooldownBeforeEnteringPigeonhole);
        if (cooldownBeforeWantingToEnterPigeonhole > 0) tag.putInt("CooldownBeforeWantingToEnterPigeonhole", cooldownBeforeWantingToEnterPigeonhole);
        if (cooldownBeforeLocatingNewPigeonhole > 0) tag.putInt("CooldownBeforeLocatingNewPigeonhole", cooldownBeforeLocatingNewPigeonhole);
    }
}
