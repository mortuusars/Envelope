package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.integration.sable.ContraptionTargets;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

import java.util.List;

public class PigeonLocatePigeonholeGoal extends Goal {
    protected final Pigeon pigeon;

    public PigeonLocatePigeonholeGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
    }

    @Override
    public boolean canUse() {
        return MailService.operatesIn(pigeon.level())
              && pigeon.getPigeonholeHandler().getLocateCooldown() == 0
              && pigeon.getPigeonholeHandler().getTargetPos() == null
              && pigeon.getPigeonholeHandler().wantsToEnterPigeonhole(pigeon);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        pigeon.getPigeonholeHandler().resetLocateCooldown();
        List<BlockPos> pigeonholes = findNearbyPigeonholesWithSpace();
        if (!pigeonholes.isEmpty()) {
            for (BlockPos pos : pigeonholes) {
                if (!pigeon.getPigeonholeHandler().isTargetBlacklisted(pos)) {
                    pigeon.getPigeonholeHandler().setTargetPos(pos);
                    return;
                }
            }

            pigeon.getPigeonholeHandler().clearBlacklist();
            pigeon.getPigeonholeHandler().setTargetPos(pigeonholes.getFirst());
        }
    }

    private List<BlockPos> findNearbyPigeonholesWithSpace() {
        ServerLevel level = (ServerLevel) pigeon.level();
        int radius = 20;
        PoiManager poiManager = level.getPoiManager();
        List<BlockPos> poiResults = poiManager.getInRange(holder ->
                    holder.is(Envelope.PoiTypes.PIGEONHOLE), pigeon.blockPosition(), radius, PoiManager.Occupancy.ANY)
              .map(PoiRecord::getPos)
              .filter(p -> level.getBlockEntity(p) instanceof PigeonholeBlockEntity pigeonhole
                    && pigeonhole.hasSpaceForAnotherOccupant())
              .toList();

        return ContraptionTargets.locateNearby(
              level,
              pigeon.position(),
              radius,
              poiResults,
              () -> ContraptionTargets.findNearbyPigeonholes(
                    level, pigeon.position(), radius, PigeonholeBlockEntity::hasSpaceForAnotherOccupant));
    }
}
