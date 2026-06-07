package io.github.mortuusars.envelope.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionReachTests {
    private static final BlockPos TARGET = new BlockPos(0, 64, 0);

    @Test
    void isWithinReach_succeedsInProjectedSpace() {
        Vec3 entityPos = Vec3.atCenterOf(TARGET);

        assertTrue(Position.isWithinReach(null, TARGET, entityPos, TARGET, 2));
    }

    @Test
    void isWithinReach_fallsBackToBlockGridDistance() {
        Vec3 entityPos = new Vec3(0.5, 67, 0.5);
        BlockPos entityBlockPos = new BlockPos(0, 64, 0);

        assertFalse(Position.closerThan(null, TARGET, entityPos, 2));
        assertTrue(Position.isWithinReach(null, TARGET, entityPos, entityBlockPos, 2));
    }

    @Test
    void isWithinReach_failsWhenTooFar() {
        BlockPos farPos = new BlockPos(32, 64, 32);
        Vec3 entityPos = Vec3.atCenterOf(farPos);

        assertFalse(Position.isWithinReach(null, TARGET, entityPos, farPos, 2));
    }

    @Test
    void getDistanceBetween_usesBlockCornersWithoutLevel() {
        BlockPos a = new BlockPos(0, 0, 0);
        BlockPos b = new BlockPos(3, 4, 0);

        assertEquals(5, Position.getDistanceBetween(a, b));
    }
}
