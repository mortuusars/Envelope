package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

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
    public static final Logger LOGGER = LogUtils.getLogger();

    private final UUID id;
    private final Address address;
    private final int capacity;
    private final List<ItemStack> mail;

    private @Nullable Runnable onChanged;

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

    // --

    public void onChanged(@NotNull Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public void onInboxChanged() {
        if (onChanged == null) {
            LOGGER.warn("UnloadedInbox was changed without onChanged callback being set. Changes may not persist.");
        } else {
            onChanged.run();
        }
    }
}
