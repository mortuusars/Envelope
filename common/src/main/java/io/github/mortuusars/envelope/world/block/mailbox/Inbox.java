package io.github.mortuusars.envelope.world.block.mailbox;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.component.NewMail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;

public interface Inbox {
    Logger LOGGER = LogUtils.getLogger();

    Address getAddress();

    int getInboxCapacity();

    @NotNull List<ItemStack> getAllMail();

    default boolean isFull() {
        return getAllMail().size() >= getInboxCapacity();
    }

    default ItemStack getMail(int slot) {
        if (slot >= 0 && slot < getAllMail().size()) {
            return getAllMail().get(slot);
        }
        return ItemStack.EMPTY;
    }

    default boolean addMailNoUpdate(int slot, ItemStack mail) {
        Preconditions.checkArgument(!mail.isEmpty(), "Inbox can only store non-empty mail.");
        Preconditions.checkArgument(mail.getCount() == 1, "Inbox can only store mail with stack size of 1. Got: " + mail.getCount());
        Preconditions.checkArgument(NewMail.hasId(mail), "Inbox can only store mail with 'envelope:mail_id' component.");
        Preconditions.checkArgument(slot >= 0, "Slot index should be larger or equal to 0. Got: " + slot);
        int size = getAllMail().size();
        if (size < getInboxCapacity()) {
            if (slot > size) {
                getAllMail().add(mail);
            } else {
                getAllMail().add(slot, mail);
            }
            return true;
        }
        return false;
    }

    default boolean addMail(int slot, ItemStack mail) {
        if (addMailNoUpdate(slot, mail)) {
            onMailAdded(mail);
            return true;
        }
        return false;
    }

    default boolean addMailNoUpdate(ItemStack mail) {
        return addMailNoUpdate(Integer.MAX_VALUE, mail);
    }

    default boolean addMail(ItemStack mail) {
        if (addMailNoUpdate(mail)) {
            onMailAdded(mail);
            return true;
        }
        return false;
    }

    default ItemStack removeMail(Id id) {
        for (int i = 0; i < getAllMail().size(); i++) {
            ItemStack mail = getAllMail().get(i);
            if (id.equals(NewMail.getId(mail))) {
                return removeMail(i);
            }
        }
        return ItemStack.EMPTY;
    }

    default ItemStack removeMail(int slot) {
        ItemStack mail = removeMailNoUpdate(slot);
        if (!mail.isEmpty()) {
            onMailRemoved(slot, mail);
        }
        return mail;
    }

    default ItemStack removeMail() {
        return removeMail(0);
    }

    default ItemStack removeMailNoUpdate(int slot) {
        if (slot >= 0 && slot < getAllMail().size()) {
            return getAllMail().remove(slot);
        }
        return ItemStack.EMPTY;
    }

    default ItemStack removeMailNoUpdate() {
        return removeMailNoUpdate(0);
    }

    default boolean clearMail() {
        if (getAllMail().isEmpty()) {
            return false;
        }
        getAllMail().clear();
        onMailCleared();
        return true;
    }

    // --

    default void onMailAdded(ItemStack mail) {
        onInboxChanged();
    }

    default void onMailRemoved(int slot, ItemStack mail) {
        onInboxChanged();
    }

    default void onMailCleared() {
        onInboxChanged();
    }

    default void onInboxChanged() {

    }
}
