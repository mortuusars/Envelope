package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UnloadedInbox implements Inbox {
    public static final Codec<UnloadedInbox> CODEC = RecordCodecBuilder.create(i -> i.group(
          UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(UnloadedInbox::getId),
          Address.CODEC.fieldOf("address").forGetter(UnloadedInbox::getAddress),
          Codec.INT.optionalFieldOf("capacity", 512).forGetter(UnloadedInbox::getInboxCapacity),
          ItemStack.CODEC.listOf().optionalFieldOf("mail", Collections.emptyList()).forGetter(UnloadedInbox::getAllMail)
    ).apply(i, UnloadedInbox::new));

    private final UUID id;
    private final Address address;
    private final int capacity;
    private final List<ItemStack> mail;

    public UnloadedInbox(UUID id, Address address, int capacity, List<ItemStack> mail) {
        this.id = id;
        this.address = address;
        this.capacity = capacity;
        this.mail = new ArrayList<>(mail);
    }

    public UUID getId() {
        return id;
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public int getInboxCapacity() {
        return capacity;
    }

    @Override
    public @NotNull List<ItemStack> getAllMail() {
        return mail;
    }
}
