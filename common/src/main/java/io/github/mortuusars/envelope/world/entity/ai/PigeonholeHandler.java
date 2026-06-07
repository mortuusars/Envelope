package io.github.mortuusars.envelope.world.entity.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PigeonholeHandler {
    public static final int MAX_BLACKLISTED_TARGETS = 3;
    public static final int DEFAULT_LOCATE_COOLDOWN = 200;

    public static final Codec<PigeonholeHandler> CODEC = RecordCodecBuilder.create(i -> i.group(
          BlockPos.CODEC.optionalFieldOf("current_pos").forGetter(o -> Optional.ofNullable(o.getTargetPos())),
          BlockPos.CODEC.optionalFieldOf("last_release_pos").forGetter(o -> Optional.ofNullable(o.getLastReleasePos())),
          Codec.INT.optionalFieldOf("locate_cooldown", 0).forGetter(PigeonholeHandler::getLocateCooldown),
          Codec.INT.optionalFieldOf("want_cooldown", 0).forGetter(PigeonholeHandler::getWantCooldown),
          Codec.INT.optionalFieldOf("enter_cooldown", 0).forGetter(PigeonholeHandler::getEnterCooldown)
    ).apply(i, PigeonholeHandler::new));

    protected final List<BlockPos> blacklistedPositions = new ArrayList<>();

    protected @Nullable BlockPos targetPos;
    protected @Nullable BlockPos lastReleasePos;
    protected int locateCooldown;
    protected int wantCooldown;
    protected int enterCooldown;

    public PigeonholeHandler(Optional<BlockPos> targetPos, Optional<BlockPos> lastReleasePos,
                             int locateCooldown, int wantCooldown, int enterCooldown) {
        this.targetPos = targetPos.orElse(null);
        this.lastReleasePos = lastReleasePos.orElse(null);
        this.locateCooldown = locateCooldown;
        this.wantCooldown = wantCooldown;
        this.enterCooldown = enterCooldown;
    }

    public PigeonholeHandler() {
        this(Optional.empty(), Optional.empty(), 0, 0, 0);
    }

    public @Nullable BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(@Nullable BlockPos targetPos) {
        this.targetPos = targetPos;
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
        setWantCooldown(Config.Server.PIGEON_MIN_TICKS_OUTSIDE_PIGEONHOLE.get());
    }

    public void setRandomWantCooldownUpToDefault(RandomSource random) {
        setWantCooldown(random.nextInt(Config.Server.PIGEON_MIN_TICKS_OUTSIDE_PIGEONHOLE.get()));
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

    public void tick(Pigeon pigeon, Level level) {
        if (wantCooldown > 0) {
            wantCooldown--;
        }
        if (locateCooldown > 0) {
            locateCooldown--;
        }

        if (level instanceof ServerLevel && pigeon.tickCount % 20 == 0) {
            if (!isPigeonholeValid(level, pigeon.blockPosition())) {
                setTargetPos(null);
            }
            Bugger.PIGEON_PIGEONHOLE_HANDLER.send(pigeon.getId(), this);
        }
    }

    // --

    public Optional<PigeonholeBlockEntity> getPigeonholeAtCurrentPos(Level level) {
        @Nullable BlockPos pos = getTargetPos();
        if (pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PigeonholeBlockEntity be) {
            return Optional.of(be);
        }
        return Optional.empty();
    }

    public boolean wantsToEnterPigeonhole(Pigeon pigeon) {
        if (getEnterCooldown() > 0) return false;
        Level level = pigeon.level();
        boolean wouldPreferInside = level.isNight() || level.isRaining() || level.isThundering();
        boolean tiredOfOutside = pigeon.isTired() || getWantCooldown() <= 0;
        return (wouldPreferInside || tiredOfOutside);
    }

    public boolean isPigeonholeValid(Level level, BlockPos entityPos) {
        @Nullable BlockPos currentPos = getTargetPos();
        if (currentPos == null) return false;
        if (Position.distanceToSqr(level, currentPos, entityPos.getCenter()) > 32 * 32) return false;
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
        if (getTargetPos() != null) {
            blacklistTarget(getTargetPos());
        }

        dropPigeonhole();
    }

    public void dropPigeonhole() {
        setTargetPos(null);
        resetLocateCooldown();
    }

    @Override
    public String toString() {
        return "PigeonholeHandler{" +
              "currentPos=" + targetPos +
              ", lastReleasePos=" + lastReleasePos +
              ", locateCooldown=" + locateCooldown +
              ", wantCooldown=" + wantCooldown +
              ", enterCooldown=" + enterCooldown +
              '}';
    }
}
