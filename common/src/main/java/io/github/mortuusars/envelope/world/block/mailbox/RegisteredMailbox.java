package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.BlockPos;

public class RegisteredMailbox {
    public static final Codec<RegisteredMailbox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Address.Block.STRING_CODEC.fieldOf("address").forGetter(RegisteredMailbox::getAddress),
          BlockPos.CODEC.fieldOf("pos").forGetter(RegisteredMailbox::getPos)
    ).apply(instance, RegisteredMailbox::new));

    private final Address.Block address;
    private final BlockPos pos;

    public RegisteredMailbox(Address.Block address, BlockPos pos) {
        this.address = address;
        this.pos = pos;
    }

    public Address.Block getAddress() {
        return address;
    }

    public BlockPos getPos() {
        return pos;
    }
}