package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.integration.sable.ContraptionTargets;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

import java.util.List;

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
        return MailService.operatesIn(pigeon.level())
              && !pigeon.isDelivering()
              && pigeon.canStartDelivery()
              && pigeon.getMailboxHandler().getLocateCooldown() <= 0
              && pigeon.getMailboxHandler().getTargetPos() == null
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
        ServerLevel level = (ServerLevel) pigeon.level();
        int radius = 20;
        PoiManager poiManager = level.getPoiManager();
        List<BlockPos> poiResults = poiManager.getInRange(holder ->
                    holder.is(Envelope.PoiTypes.MAILBOX), pigeon.blockPosition(), radius, PoiManager.Occupancy.ANY)
              .map(PoiRecord::getPos)
              .filter(p -> level.getBlockEntity(p) instanceof MailboxBlockEntity mailbox
                    && mailbox.isAvailableForPickup())
              .toList();

        return ContraptionTargets.locateNearby(
              level,
              pigeon.position(),
              radius,
              poiResults,
              () -> ContraptionTargets.findNearbyMailboxes(
                    level, pigeon.position(), radius, MailboxBlockEntity::isAvailableForPickup));
    }
}
