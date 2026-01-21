package io.github.mortuusars.envelope.world.block.mailbox;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Inbox {
    Address getAddress();

    int getInboxCapacity();

    @NotNull List<ItemStack> getAllMail();

    void onInboxChanged();

    default boolean isInboxFull() {
        return getAllMail().size() >= getInboxCapacity();
    }

    default boolean isInboxEmpty() {
        return getAllMail().isEmpty();
    }

    default ItemStack getMail(int slot) {
        if (slot >= 0 && slot < getAllMail().size()) {
            return getAllMail().get(slot);
        }
        return ItemStack.EMPTY;
    }

    // --

    default boolean addMailNoUpdate(int slot, ItemStack mail) {
        Preconditions.checkArgument(!mail.isEmpty(), "Inbox can only store non-empty mail.");
        Preconditions.checkArgument(mail.getCount() == 1, "Inbox can only store mail with stack size of 1. Got: " + mail.getCount());
        Preconditions.checkArgument(Mail.hasId(mail), "Inbox can only store mail with 'envelope:mail_id' component.");
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

    default boolean addMailNoUpdate(ItemStack mail) {
        return addMailNoUpdate(Integer.MAX_VALUE, mail);
    }

    default boolean addMail(int slot, ItemStack mail) {
        if (addMailNoUpdate(slot, mail)) {
            onInboxChanged();
            onMailAdded(mail);
            return true;
        }
        return false;
    }

    default boolean addMail(ItemStack mail) {
        if (addMailNoUpdate(mail)) {
            onInboxChanged();
            onMailAdded(mail);
            return true;
        }
        return false;
    }

    // --

    default ItemStack removeMailNoUpdate(int slot) {
        if (slot >= 0 && slot < getAllMail().size()) {
            return getAllMail().remove(slot);
        }
        return ItemStack.EMPTY;
    }

    default ItemStack removeMailNoUpdate() {
        return removeMailNoUpdate(0);
    }

    default ItemStack removeMail(Id id) {
        for (int i = 0; i < getAllMail().size(); i++) {
            ItemStack mail = getAllMail().get(i);
            if (id.equals(Mail.getId(mail))) {
                return removeMail(i);
            }
        }
        return ItemStack.EMPTY;
    }

    default ItemStack removeMail(int slot) {
        ItemStack mail = removeMailNoUpdate(slot);
        if (!mail.isEmpty()) {
            onInboxChanged();
            onMailRemoved(slot, mail);
        }
        return mail;
    }

    default ItemStack removeMail() {
        return removeMail(0);
    }

    default boolean clearMail() {
        if (getAllMail().isEmpty()) {
            return false;
        }
        getAllMail().clear();
        onInboxChanged();
        onMailCleared();
        return true;
    }

    // --

    /**
     * Intended to be called from outside (when delivering mail for example). <br>
     * Inbox can override it to add custom logic on deliberate insertion (not through hopper, etc).
     */
    default void onMailInserted(ItemStack mail) {
    }

    /**
     * Called when mail has been added. Can be overriden by implementations to execute related logic.
     */
    default void onMailAdded(ItemStack mail) {
    }

    /**
     * Called when mail has been removed. Can be overriden by implementations to execute related logic.
     */
    default void onMailRemoved(int slot, ItemStack mail) {
    }

    /**
     * Called when mail has been cleared. Can be overriden by implementations to execute related logic.
     */
    default void onMailCleared() {
    }
}
