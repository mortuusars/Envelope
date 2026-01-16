package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PigeonLocateMailboxGoal extends Goal {
    protected final Pigeon pigeon;

    public PigeonLocateMailboxGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return !pigeon.isDelivering()
              && pigeon.getMailboxHandler().getLocateCooldown() <= 0
              && pigeon.getMailboxHandler().getTargetPos() == null
              && !pigeon.isTired()
              && pigeon.level().getRandom().nextFloat() < 0.05;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        pigeon.getMailboxHandler().resetLocateCooldown();
        List<BlockPos> mailboxes = findNearbyAvailableMailboxes();
        if (!mailboxes.isEmpty()) {
            for (BlockPos pos : mailboxes) {
                if (!pigeon.getMailboxHandler().isTargetBlacklisted(pos)) {
                    pigeon.getMailboxHandler().setTargetPos(pos);
                    return;
                }
            }

            pigeon.getMailboxHandler().clearBlacklist();
            pigeon.getMailboxHandler().setTargetPos(mailboxes.getFirst());
        }
    }

    private List<BlockPos> findNearbyAvailableMailboxes() {
        BlockPos pos = pigeon.blockPosition();
        PoiManager poiManager = ((ServerLevel) pigeon.level()).getPoiManager();
        return poiManager.getInRange(holder ->
                    holder.is(Envelope.PoiTypes.MAILBOX), pos, 20, PoiManager.Occupancy.ANY)
              .map(PoiRecord::getPos)
              .filter(p -> pigeon.level().getBlockEntity(p) instanceof MailboxBlockEntity mailbox
                    && mailbox.isAvailableForPickup())
              .sorted(Comparator.comparingDouble(p -> p.distSqr(pos)))
              .collect(Collectors.toList());
    }
}
