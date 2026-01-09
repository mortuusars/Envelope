package io.github.mortuusars.envelope.world.block.mailbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.NewMail;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Inbox implements Container {
    public static final Codec<Inbox> CODEC = RecordCodecBuilder.create(i -> i.group(
          ItemStack.CODEC.listOf().optionalFieldOf("mail", Collections.emptyList()).forGetter(Inbox::getMail)
    ).apply(i, Inbox::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Inbox> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), Inbox::getMail,
          Inbox::new
    );

    private final List<ItemStack> mail;

    public Inbox(List<ItemStack> mail) {
        this.mail = new ArrayList<>(mail); // Make sure it's mutable
    }

    public List<ItemStack> getMail() {
        return mail;
    }

    public void add(ItemStack mail) {
        if (mail.isEmpty()) {
            return;
        }
        this.mail.add(mail);
    }

    // --

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getContainerSize() {
        return mail.size();
    }

    @Override
    public boolean isEmpty() {
        return mail.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slot >= 0 && slot < mail.size()
              ? mail.get(slot)
              : ItemStack.EMPTY;
    }

    public @NotNull ItemStack removeItem(int slot) {
        return removeItem(slot, Integer.MAX_VALUE);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack stack = removeItemNoUpdate(slot);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return slot >= 0 && slot < mail.size()
              ? mail.remove(slot)
              : ItemStack.EMPTY;
    }

    public ItemStack removeItemById(@NotNull Id id) {
        int index = -1;
        for (int i = 0; i < mail.size(); i++) {
            ItemStack stack = mail.get(i);
            if (id.equals(NewMail.getId(stack))) {
                index = i;
                break;
            }
        }
        return index >= 0 ? removeItem(index) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return;

        if (slot >= 0 && slot < mail.size()) {
            mail.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public void clearContent() {
        mail.clear();
        setChanged();
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}