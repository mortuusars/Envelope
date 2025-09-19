package io.github.mortuusars.envelope.world.entity.ai;

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
    private static final int COOLDOWN_BEFORE_LOCATING_NEW_PIGEONHOLE = 200;

    protected final Level level;
    protected final List<BlockPos> blacklistedPositions;

    protected @Nullable BlockPos pigeonholePos;
    protected @Nullable BlockPos lastPigeonholePos;
    protected long leftPigeonholeAt;
    protected int wouldWantToEnterPigeonholeAfter;
    protected int cooldownBeforeLocatingNewPigeonhole;

    public PigeonholeHandler(Level level) {
        this.level = level;
        wouldWantToEnterPigeonholeAfter = level.getRandom().nextInt(200, 600);
        blacklistedPositions = new ArrayList<>();
    }

    public @Nullable BlockPos getPigeonholePos() {
        return pigeonholePos;
    }

    public void setPigeonholePos(@Nullable BlockPos pigeonholePos) {
        this.pigeonholePos = pigeonholePos;
    }

    public @Nullable BlockPos getLastPigeonholePos() {
        return lastPigeonholePos;
    }

    public void setLastPigeonholePos(@Nullable BlockPos lastPigeonholePos) {
        this.lastPigeonholePos = lastPigeonholePos;
    }

    public long getLeftPigeonholeAt() {
        return leftPigeonholeAt;
    }

    public void setLeftPigeonholeAt(long leftPigeonholeAt) {
        this.leftPigeonholeAt = leftPigeonholeAt;
    }

    public int getWouldWantToEnterPigeonholeAfter() {
        return wouldWantToEnterPigeonholeAfter;
    }

    public void setWouldWantToEnterPigeonholeAfter(int wouldWantToEnterPigeonholeAfter) {
        this.wouldWantToEnterPigeonholeAfter = wouldWantToEnterPigeonholeAfter;
    }

    public int getCooldownBeforeLocatingNewPigeonhole() {
        return cooldownBeforeLocatingNewPigeonhole;
    }

    public void setCooldownBeforeLocatingNewPigeonhole(int cooldownBeforeLocatingNewPigeonhole) {
        this.cooldownBeforeLocatingNewPigeonhole = cooldownBeforeLocatingNewPigeonhole;
    }

    public void setCooldownBeforeLocatingNewPigeonhole() {
        setCooldownBeforeLocatingNewPigeonhole(COOLDOWN_BEFORE_LOCATING_NEW_PIGEONHOLE);
    }

    // --

    public long getTicksSinceLeftPigeonhole() {
        return level.getGameTime() - leftPigeonholeAt;
    }

    public Optional<PigeonholeBlockEntity> getPigeonhole() {
        BlockPos pos = getPigeonholePos();
        if (pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity be) {
            return Optional.of(be);
        }
        return Optional.empty();
    }

    public boolean wantsToEnterPigeonhole() {
        if (getTicksSinceLeftPigeonhole() < 200) return false; // Cooldown

        boolean wantsToEnter = (level.isNight() || level.isThundering())
                || (level.getGameTime() >= leftPigeonholeAt + wouldWantToEnterPigeonholeAfter);

        return wantsToEnter && !isPigeonholeNearFire();
    }

    protected boolean isPigeonholeNearFire() {
        return getPigeonhole().map(PigeonholeBlockEntity::isFireNearby).orElse(false);
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
        cooldownBeforeLocatingNewPigeonhole = COOLDOWN_BEFORE_LOCATING_NEW_PIGEONHOLE;
    }

    public void load(CompoundTag tag) {
        pigeonholePos = NbtUtils.readBlockPos(tag, "PigeonholePos").orElse(null);
        lastPigeonholePos = NbtUtils.readBlockPos(tag, "LastPigeonholePos").orElse(null);
        leftPigeonholeAt = tag.getLong("LeftPigeonholeAt");
        wouldWantToEnterPigeonholeAfter = tag.getInt("WouldWantToEnterPigeonholeAfter");
    }

    public void save(CompoundTag tag) {
        if (getPigeonholePos() != null) {
            tag.put("PigeonholePos", NbtUtils.writeBlockPos(getPigeonholePos()));
        }
        if (lastPigeonholePos != null) {
            tag.put("LastPigeonholePos", NbtUtils.writeBlockPos(lastPigeonholePos));
        }
        if (leftPigeonholeAt > 0) {
            tag.putLong("LeftPigeonholeAt", leftPigeonholeAt);
        }
        if (wouldWantToEnterPigeonholeAfter > 0) {
            tag.putInt("WouldWantToEnterPigeonholeAfter", wouldWantToEnterPigeonholeAfter);
        }
    }
}
