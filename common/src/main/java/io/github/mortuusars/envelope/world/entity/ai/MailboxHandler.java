package io.github.mortuusars.envelope.world.entity.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.Position;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MailboxHandler {
    public static final int MAX_BLACKLISTED_TARGETS = 3;
    public static final int DEFAULT_LOCATE_COOLDOWN = 100;

    public static final Codec<MailboxHandler> CODEC = RecordCodecBuilder.create(i -> i.group(
          BlockPos.CODEC.optionalFieldOf("target_pos").forGetter(o -> Optional.ofNullable(o.getTargetPos())),
          Codec.INT.optionalFieldOf("locate_cooldown", 0).forGetter(MailboxHandler::getLocateCooldown)
    ).apply(i, MailboxHandler::new));

    protected final List<BlockPos> blacklistedPositions = new ArrayList<>();

    protected @Nullable BlockPos targetPos;
    protected int locateCooldown;

    public MailboxHandler(Optional<BlockPos> targetPos, int locateCooldown) {
        this.targetPos = targetPos.orElse(null);
        this.locateCooldown = locateCooldown;
    }

    public MailboxHandler() {
        this(Optional.empty(), 0);
    }

    public @Nullable BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(@Nullable BlockPos targetPos) {
        this.targetPos = targetPos;
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
        if (locateCooldown > 0) {
            locateCooldown--;
        }

        if (level instanceof ServerLevel && pigeon.tickCount % 20 == 0) {
            if (!isMailboxValid(level, pigeon.blockPosition())) {
                setTargetPos(null);
            }
            Bugger.PIGEON_MAILBOX_HANDLER.send(pigeon.getId(), this);
        }
    }

    public boolean canStartDelivery(Pigeon pigeon) {
        return !pigeon.isTired();
    }

    // --

    public Optional<MailboxBlockEntity> getMailboxAtCurrentPos(Level level) {
        @Nullable BlockPos pos = getTargetPos();
        if (pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MailboxBlockEntity be) {
            return Optional.of(be);
        }
        return Optional.empty();
    }

    public boolean isMailboxValid(Level level, BlockPos entityPos) {
        @Nullable BlockPos currentPos = getTargetPos();
        if (currentPos == null) return false;
        if (Position.distanceToSqr(level, currentPos, entityPos.getCenter()) > 32 * 32) return false;
        return level.getBlockEntity(currentPos) instanceof MailboxBlockEntity blockEntity
              && blockEntity.isAvailableForPickup();
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

    public void dropAndBlacklistMailbox() {
        if (getTargetPos() != null) {
            blacklistTarget(getTargetPos());
        }

        dropMailbox();
    }

    public void dropMailbox() {
        setTargetPos(null);
        resetLocateCooldown();
    }

    @Override
    public String toString() {
        return "MailboxHandler{" +
              ", targetPos=" + targetPos +
              ", locateCooldown=" + locateCooldown +
              '}';
    }
}
