package io.github.mortuusars.envelope.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public interface EnvelopeStreamCodecs {
    StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
          ByteBufCodecs.DOUBLE, Vec3::x,
          ByteBufCodecs.DOUBLE, Vec3::y,
          ByteBufCodecs.DOUBLE, Vec3::z,
          Vec3::new
    );

    StreamCodec<FriendlyByteBuf, BlockHitResult> BLOCK_HIT_RESULT = StreamCodec.composite(
          VEC3, BlockHitResult::getLocation,
          Direction.STREAM_CODEC, BlockHitResult::getDirection,
          BlockPos.STREAM_CODEC, BlockHitResult::getBlockPos,
          ByteBufCodecs.BOOL, BlockHitResult::isInside,
          BlockHitResult::new
    );
}
