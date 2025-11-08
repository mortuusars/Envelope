package io.github.mortuusars.envelope.world.entity.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PigeonholeHandler {
    public static final int MAX_BLACKLISTED_TARGETS = 3;
    public static final int DEFAULT_LOCATE_COOLDOWN = 200;

    public static final Codec<PigeonholeHandler> CODEC = RecordCodecBuilder.create(i -> i.group(
          BlockPos.CODEC.optionalFieldOf("current_pos").forGetter(o -> Optional.ofNullable(o.getCurrentPos())),
          BlockPos.CODEC.optionalFieldOf("last_release_pos").forGetter(o -> Optional.ofNullable(o.getLastReleasePos())),
          Codec.INT.optionalFieldOf("locate_cooldown", 0).forGetter(PigeonholeHandler::getLocateCooldown),
          Codec.INT.optionalFieldOf("want_cooldown", 0).forGetter(PigeonholeHandler::getWantCooldown),
          Codec.INT.optionalFieldOf("enter_cooldown", 0).forGetter(PigeonholeHandler::getEnterCooldown)
    ).apply(i, PigeonholeHandler::new));

    protected final List<BlockPos> blacklistedPositions = new ArrayList<>();

    protected @Nullable BlockPos currentPos;
    protected @Nullable BlockPos lastReleasePos;
    protected int locateCooldown;
    protected int wantCooldown;
    protected int enterCooldown;

    public PigeonholeHandler(Optional<BlockPos> currentPos, Optional<BlockPos> lastReleasePos,
                             int locateCooldown, int wantCooldown, int enterCooldown) {
        this.currentPos = currentPos.orElse(null);
        this.lastReleasePos = lastReleasePos.orElse(null);
        this.locateCooldown = locateCooldown;
        this.wantCooldown = wantCooldown;
        this.enterCooldown = enterCooldown;
    }

    public PigeonholeHandler() {
        this(Optional.empty(), Optional.empty(), 0, 0, 0);
    }

    public @Nullable BlockPos getCurrentPos() {
        return currentPos;
    }

    public void setCurrentPos(@Nullable BlockPos currentPos) {
        this.currentPos = currentPos;
    }

    public @Nullable BlockPos getLastReleasePos() {
        return lastReleasePos;
    }

    public PigeonholeHandler setLastReleasePos(@Nullable BlockPos lastReleasePos) {
        this.lastReleasePos = lastReleasePos;
        return this;
    }

    public int getEnterCooldown() {
        return enterCooldown;
    }

    public void setEnterCooldown(int cooldown) {
        this.enterCooldown = cooldown;
    }

    public int getWantCooldown() {
        return wantCooldown;
    }

    public void setWantCooldown(int cooldown) {
        this.wantCooldown = cooldown;
    }

    public void setDefaultWantCooldown() {
        wantCooldown = Config.Server.PIGEON_MIN_TICKS_OUTSIDE_PIGEONHOLE.get();
    }

    public int getLocateCooldown() {
        return locateCooldown;
    }

    public void setLocateCooldown(int cooldown) {
        this.locateCooldown = cooldown;
    }

    public void resetLocateCooldown() {
        setLocateCooldown(DEFAULT_LOCATE_COOLDOWN);
    }

    // --

    public void tick(Level level) {
        if (wantCooldown > 0) {
            wantCooldown--;
        }
        if (locateCooldown > 0) {
            locateCooldown--;
        }
    }

    // --

    public Optional<PigeonholeBlockEntity> getPigeonholeAtCurrentPos(Level level) {
        @Nullable BlockPos pos = getCurrentPos();
        if (pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity be) {
            return Optional.of(be);
        }
        return Optional.empty();
    }

    public boolean wantsToEnterPigeonhole(Level level) {
        if (getEnterCooldown() > 0) return false;
        boolean wouldHateToBeOutside = level.isNight() || level.isRaining() || level.isThundering();
        boolean tiredOfOutside = getWantCooldown() <= 0;
        return (wouldHateToBeOutside || tiredOfOutside) && !isPigeonholeNearFire(level);
    }

    protected boolean isPigeonholeNearFire(Level level) {
        return getPigeonholeAtCurrentPos(level).map(PigeonholeBlockEntity::isFireNearby).orElse(false);
    }

    public boolean isPigeonholeValid(Level level, BlockPos entityPos) {
        @Nullable BlockPos currentPos = getCurrentPos();
        if (currentPos == null) return false;
        if (!entityPos.closerThan(currentPos, 32)) return false;
        return level.getBlockEntity(currentPos) instanceof PigeonholeBlockEntity;
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
        if (getCurrentPos() != null) {
            blacklistTarget(getCurrentPos());
        }

        dropPigeonhole();
    }

    public void dropPigeonhole() {
        setCurrentPos(null);
        resetLocateCooldown();
    }

    @Override
    public String toString() {
        return "PigeonholeHandler{" +
              "currentPos=" + currentPos +
              ", lastReleasePos=" + lastReleasePos +
              ", locateCooldown=" + locateCooldown +
              ", wantCooldown=" + wantCooldown +
              ", enterCooldown=" + enterCooldown +
              '}';
    }
}
