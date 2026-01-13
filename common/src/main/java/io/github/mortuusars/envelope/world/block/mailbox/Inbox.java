package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public record Inbox(List<ItemStack> mail) {
    public static final Codec<Inbox> CODEC = RecordCodecBuilder.create(i -> i.group(
          ItemStack.CODEC.listOf().optionalFieldOf("mail", Collections.emptyList()).forGetter(Inbox::mail)
    ).apply(i, Inbox::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Inbox> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), Inbox::mail,
          Inbox::new
    );
}