package io.github.mortuusars.envelope.world.mail.delivery.background;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.entity.spawning.SpawnableEntityData;
import net.minecraft.core.BlockPos;

public record FinishedBackgroundCourier(SpawnableEntityData entityData, BlockPos spawnPos, long finishedAt) {
    public static final Codec<FinishedBackgroundCourier> CODEC = RecordCodecBuilder.create(i -> i.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(FinishedBackgroundCourier::entityData),
          BlockPos.CODEC.fieldOf("spawn_pos").forGetter(FinishedBackgroundCourier::spawnPos),
          Codec.LONG.fieldOf("finished_at").forGetter(FinishedBackgroundCourier::finishedAt)
    ).apply(i, FinishedBackgroundCourier::new));
}
