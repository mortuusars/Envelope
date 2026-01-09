package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;

public record MailboxData(Address.Block address, BlockPos pos) {
    public static final Codec<MailboxData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Address.Block.STRING_CODEC.fieldOf("address").forGetter(MailboxData::address),
          BlockPos.CODEC.fieldOf("pos").forGetter(MailboxData::pos)
    ).apply(instance, MailboxData::new));
}
