package io.github.mortuusars.envelope.world.pigeonhole;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.mail.Address;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PigeonholeData {
    public static final Codec<PigeonholeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Address.Pigeonhole.STRING_CODEC.fieldOf("address").forGetter(PigeonholeData::getAddress),
            BlockPos.CODEC.fieldOf("pos").forGetter(PigeonholeData::getPos),
            Codec.list(ItemStack.CODEC).fieldOf("mail").forGetter(PigeonholeData::getMail)
    ).apply(instance, PigeonholeData::new));

    private Address.Pigeonhole address;
    private BlockPos pos;
    private List<ItemStack> mail;

    protected PigeonholeData(Address.Pigeonhole address, BlockPos pos, List<ItemStack> mail) {
        this.address = address;
        this.pos = pos;
        this.mail = new ArrayList<>(mail); // Make sure the list is writable.
    }

    public PigeonholeData(Address.Pigeonhole address, BlockPos pos) {
        this(address, pos, new ArrayList<>());
    }

    public Address.Pigeonhole getAddress() {
        return address;
    }

    public void setAddress(Address.Pigeonhole address) {
        this.address = address;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public List<ItemStack> getMail() {
        return mail;
    }

    public void setMail(List<ItemStack> mail) {
        this.mail = mail;
    }
}
