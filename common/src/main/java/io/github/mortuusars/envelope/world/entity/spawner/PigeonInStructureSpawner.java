package io.github.mortuusars.envelope.world.entity.spawner;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PigeonInStructureSpawner implements CustomSpawner {
    protected static final int SPAWN_ATTEMPT_DELAY = 1000;
    protected int nextAttemptDelay;

    @Override
    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        if (!spawnFriendlies || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return 0;
        }

        nextAttemptDelay--;

        if (nextAttemptDelay > 0) {
            return 0;
        }

        nextAttemptDelay = SPAWN_ATTEMPT_DELAY;

        Player player = level.getRandomPlayer();
        if (player == null) {
            return 0;
        }

        RandomSource randomSource = level.random;
        int x = (8 + randomSource.nextInt(24)) * (randomSource.nextBoolean() ? -1 : 1);
        int y = (8 + randomSource.nextInt(24)) * (randomSource.nextBoolean() ? -1 : 1);
        BlockPos spawnPos = player.blockPosition().offset(x, 0, y);
        spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, spawnPos);

        if (!level.isLoaded(spawnPos)) {
            return 0;
        }

        if (SpawnPlacements.isSpawnPositionOk(Envelope.EntityTypes.PIGEON.get(), level, spawnPos)) {
            if (Config.Server.PIGEON_SPAWNS_IN_VILLAGE.get() && level.isCloseToVillage(spawnPos, 2)) {
                return this.spawnInVillage(level, spawnPos);
            }

            if (level.structureManager().getStructureWithPieceAt(spawnPos, Envelope.Tags.Structures.PIGEONS_SPAWN_IN).isValid()) {
                return this.spawnInStructure(level, spawnPos);
            }
        }

        return 0;
    }

    protected int spawnInVillage(ServerLevel serverLevel, BlockPos pos) {
        int area = 48;
        if (serverLevel.getPoiManager().getCountInRange(holder ->
              holder.is(PoiTypes.HOME), pos, area, PoiManager.Occupancy.IS_OCCUPIED) > 4L) {
            List<Pigeon> pigeonsInArea = serverLevel.getEntitiesOfClass(Pigeon.class, new AABB(pos).inflate(area, 8.0, area));
            if (pigeonsInArea.size() < 5) {
                 return spawn(serverLevel, pos);
            }
        }

        return 0;
    }

    protected int spawnInStructure(ServerLevel serverLevel, BlockPos pos) {
        int area = 16;
        List<Pigeon> list = serverLevel.getEntitiesOfClass(Pigeon.class, new AABB(pos).inflate(area, 8.0, area));
        return list.isEmpty() ? this.spawn(serverLevel, pos) : 0;
    }

    protected int spawn(ServerLevel level, BlockPos pos) {
        @Nullable Pigeon pigeon = Envelope.EntityTypes.PIGEON.get().create(level);
        if (pigeon == null) {
            return 0;
        }

        pigeon.moveTo(pos, 0.0F, 0.0F);
        pigeon.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        level.addFreshEntityWithPassengers(pigeon);
        level.playSound(null, pigeon, Envelope.SoundEvents.PIGEON_AMBIENT.get(), SoundSource.NEUTRAL, 2, 1);
        return 1;
    }
}
