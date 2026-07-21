package io.github.mortuusars.envelope.world.entity.ai;

import io.github.mortuusars.envelope.world.mail.delivery.DeliveryPhase;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PigeonNavigationTests {
    @Test
    void getSegmentApproachTarget_returnsEndPosWhenNotDescending() {
        BlockPos endPos = new BlockPos(1, 2, 3);
        assertEquals(endPos, PigeonNavigation.getSegmentApproachTarget(null, endPos, DeliveryPhase.DEPARTING_SENDER));
    }
}
