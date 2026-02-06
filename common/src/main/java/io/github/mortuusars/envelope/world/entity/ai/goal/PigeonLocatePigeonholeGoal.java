package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        BlockPos pos = pigeon.blockPosition();
        PoiManager poiManager = ((ServerLevel) pigeon.level()).getPoiManager();
        return poiManager.getInRange(holder ->
                    holder.is(Envelope.PoiTypes.PIGEONHOLE), pos, 20, PoiManager.Occupancy.ANY)
              .map(PoiRecord::getPos)
              .filter(p -> pigeon.level().getBlockEntity(p) instanceof PigeonholeBlockEntity pigeonhole
                    && pigeonhole.hasSpaceForAnotherOccupant())
              .sorted(Comparator.comparingDouble(p -> p.distSqr(pos)))
              .collect(Collectors.toList());
    }
}
