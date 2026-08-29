package io.github.mortuusars.envelope.world.entity.ai.goal.courier;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.integration.sable.ContraptionTargets;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.delivery.PhysicalCourier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

import java.util.List;

public class LocateMailboxGoal extends Goal {
    protected final PhysicalCourier courier;

    public LocateMailboxGoal(PhysicalCourier courier) {
        this.courier = courier;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return MailService.operatesIn(courier.level())
              && !courier.isDelivering()
              && courier.canStartDelivery()
              && courier.getMailboxHandler().getLocateCooldown() <= 0
              && courier.getMailboxHandler().getTargetPos() == null
              && courier.level().getRandom().nextFloat() < 0.05;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        courier.getMailboxHandler().resetLocateCooldown();
        List<BlockPos> mailboxes = findNearbyAvailableMailboxes();
        if (!mailboxes.isEmpty()) {
            for (BlockPos pos : mailboxes) {
                if (!courier.getMailboxHandler().isTargetBlacklisted(pos)) {
                    courier.getMailboxHandler().setTargetPos(pos);
                    return;
                }
            }

            courier.getMailboxHandler().clearBlacklist();
            courier.getMailboxHandler().setTargetPos(mailboxes.getFirst());
        }
    }

    private List<BlockPos> findNearbyAvailableMailboxes() {
        ServerLevel level = (ServerLevel) courier.level();
        int radius = 20;
        PoiManager poiManager = level.getPoiManager();
        List<BlockPos> poiResults = poiManager.getInRange(holder ->
                    holder.is(Envelope.PoiTypes.MAILBOX), courier.blockPosition(), radius, PoiManager.Occupancy.ANY)
              .map(PoiRecord::getPos)
              .filter(p -> level.getBlockEntity(p) instanceof MailboxBlockEntity mailbox
                    && mailbox.isAvailableForPickup())
              .toList();

        return ContraptionTargets.locateNearby(
              level,
              courier.position(),
              radius,
              poiResults,
              () -> ContraptionTargets.findNearbyMailboxes(
                    level, courier.position(), radius, MailboxBlockEntity::isAvailableForPickup));
    }
}
