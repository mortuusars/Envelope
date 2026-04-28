package io.github.mortuusars.envelope.world.entity.ai;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Ticks;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PigeonholeHandler {
    public static final int MAX_BLACKLISTED_TARGETS = 3;
    public static final int DEFAULT_LOCATE_COOLDOWN = 100;
    public static final int FORGET_HOME_TIME = Ticks.IN_GAME_DAY * 3;

    public static final Codec<PigeonholeHandler> CODEC = RecordCodecBuilder.create(i -> i.group(
          BlockPos.CODEC.optionalFieldOf("target_pos").forGetter(o -> Optional.ofNullable(o.getTargetPos())),
          BlockPos.CODEC.optionalFieldOf("home_pos").forGetter(o -> Optional.ofNullable(o.getHomePos())),
          Codec.INT.optionalFieldOf("ticks_since_last_rest", 0).forGetter(PigeonholeHandler::getTicksSinceLastRest),
          Codec.INT.optionalFieldOf("locate_cooldown", 0).forGetter(PigeonholeHandler::getLocateCooldown),
          Codec.INT.optionalFieldOf("want_cooldown", 0).forGetter(PigeonholeHandler::getWantCooldown),
          Codec.INT.optionalFieldOf("enter_cooldown", 0).forGetter(PigeonholeHandler::getEnterCooldown)
    ).apply(i, PigeonholeHandler::new));
    public static final Logger LOGGER = LogUtils.getLogger();

    protected final List<BlockPos> blacklistedPositions = new ArrayList<>();

    protected @Nullable BlockPos targetPos;
    protected @Nullable BlockPos homePos;
    protected int ticksSinceLastRest;
    protected int locateCooldown;
    protected int wantCooldown;
    protected int enterCooldown;

    public PigeonholeHandler(Optional<BlockPos> targetPos, Optional<BlockPos> homePos, int ticksSinceLastRest,
                             int locateCooldown, int wantCooldown, int enterCooldown) {
        this.targetPos = targetPos.orElse(null);
        this.homePos = homePos.orElse(null);
        this.ticksSinceLastRest = ticksSinceLastRest;
        this.locateCooldown = locateCooldown;
        this.wantCooldown = wantCooldown;
        this.enterCooldown = enterCooldown;
    }

    public PigeonholeHandler() {
        this(Optional.empty(), Optional.empty(), 0, 0, 0, 0);
    }

    public @Nullable BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(@Nullable BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    public @Nullable BlockPos getHomePos() {
        return homePos;
    }

    public PigeonholeHandler setHomePos(@Nullable BlockPos homePos) {
        this.homePos = homePos;
        return this;
    }

    public int getTicksSinceLastRest() {
        return ticksSinceLastRest;
    }

    public PigeonholeHandler setTicksSinceLastRest(int ticks) {
        this.ticksSinceLastRest = ticks;
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
        ticksSinceLastRest++;
        if (wantCooldown > 0) wantCooldown--;
        if (locateCooldown > 0) locateCooldown--;

        if (level instanceof ServerLevel) {
            if (getHomePos() != null && getTicksSinceLastRest() > FORGET_HOME_TIME) {
                LOGGER.debug("{} has forgotten their home position [{}].", pigeon, getHomePos().toShortString());
                setHomePos(null);
            }

            if (pigeon.tickCount % 20 == 0) {
                // This is important for the pigeon to forget its previous pigeonhole if far away or no longer existing
                if (getTargetPos() != null && !pigeon.isDelivering() && !isPigeonholeValid(level, pigeon.blockPosition())) {
                    setTargetPos(null);
                }
                Bugger.PIGEON_PIGEONHOLE_HANDLER.send(pigeon.getId(), this);
            }
        }
    }

    // --

    public List<BlockPos> findNearbyPigeonholesWithSpace(ServerLevel level, BlockPos pos) {
        PoiManager poiManager = level.getPoiManager();
        return poiManager.getInRange(holder ->
                    holder.is(Envelope.PoiTypes.PIGEONHOLE), pos, 48, PoiManager.Occupancy.ANY)
              .map(PoiRecord::getPos)
              .filter(p -> level.getBlockEntity(p) instanceof PigeonholeBlockEntity pigeonhole
                    && pigeonhole.hasSpaceForAnotherOccupant())
              .sorted(Comparator.comparingDouble(p -> p.distSqr(pos)))
              .collect(Collectors.toList());
    }

    public Optional<PigeonholeBlockEntity> getPigeonholeAtTargetPos(Level level) {
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
        return blacklistedPositions.contains(pos);
    }

    private void blacklistTarget(BlockPos pos) {
        blacklistedPositions.add(pos);

        while (blacklistedPositions.size() > MAX_BLACKLISTED_TARGETS) {
            blacklistedPositions.removeFirst();
        }
    }

    public void clearBlacklist() {
        blacklistedPositions.clear();
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
              "targetPos=" + targetPos +
              ", homePos=" + homePos +
              ", ticksSinceLastRest=" + ticksSinceLastRest +
              ", locateCooldown=" + locateCooldown +
              ", wantCooldown=" + wantCooldown +
              ", enterCooldown=" + enterCooldown +
              '}';
    }

    // --

    public static boolean isPigeonholeSafe(Level level, BlockPos pos) {
        return !CampfireBlock.isSmokeyPos(level, pos) && !Position.isFireNearby(level, pos);
    }
}
