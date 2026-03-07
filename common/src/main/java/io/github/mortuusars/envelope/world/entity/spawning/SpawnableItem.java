package io.github.mortuusars.envelope.world.entity.spawning;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public record SpawnableItem(ItemStack item, BlockPos pos) {
    public static final Codec<SpawnableItem> CODEC = RecordCodecBuilder.create(i -> i.group(
          ItemStack.CODEC.fieldOf("item").forGetter(SpawnableItem::item),
          BlockPos.CODEC.fieldOf("pos").forGetter(SpawnableItem::pos)
    ).apply(i, SpawnableItem::new));
}
