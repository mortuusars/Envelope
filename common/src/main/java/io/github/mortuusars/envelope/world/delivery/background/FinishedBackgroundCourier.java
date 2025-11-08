package io.github.mortuusars.envelope.world.delivery.background;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.entity.SpawnableEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public record FinishedBackgroundCourier(SpawnableEntityData entityData, BlockPos spawnPos, ItemStack undeliveredMail) {
    public static final Codec<FinishedBackgroundCourier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          SpawnableEntityData.CODEC.fieldOf("entity").forGetter(FinishedBackgroundCourier::entityData),
          BlockPos.CODEC.fieldOf("spawn_pos").forGetter(FinishedBackgroundCourier::spawnPos),
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("undelivered_mail", ItemStack.EMPTY).forGetter(FinishedBackgroundCourier::undeliveredMail)
    ).apply(instance, FinishedBackgroundCourier::new));
}
