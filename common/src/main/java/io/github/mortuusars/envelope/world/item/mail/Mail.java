package io.github.mortuusars.envelope.world.item.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.PackageItem;
import io.github.mortuusars.envelope.world.item.component.*;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryLog;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class Mail {
    private Mail() {
    }

    // -- Address

    public static Optional<Address> getSender(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.MAIL_SENDER));
    }

    public static @NotNull Address getSenderOrElse(ItemStack stack, Address orElse) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_SENDER, orElse);
    }

    public static @NotNull Address getSenderOrUnknown(ItemStack stack) {
        return getSenderOrElse(stack, Address.UNKNOWN);
    }

    public static void setSender(@NotNull ItemStack stack, @Nullable Address sender) {
        stack.set(Envelope.DataComponents.MAIL_SENDER, sender);
    }

    public static Optional<Address> getRecipient(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.MAIL_RECIPIENT));
    }

    public static @NotNull Address getRecipientOrElse(ItemStack stack, Address orElse) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_RECIPIENT, orElse);
    }

    public static @NotNull Address getRecipientOrUnknown(ItemStack stack) {
        return getRecipientOrElse(stack, Address.UNKNOWN);
    }

    public static ItemStack setRecipient(@NotNull ItemStack stack, @Nullable Address recipient) {
        stack.set(Envelope.DataComponents.MAIL_RECIPIENT, recipient);
        return stack;
    }

    // -- Id

    public static boolean hasId(ItemStack stack) {
        return stack.has(Envelope.DataComponents.MAIL_ID);
    }

    public static @Nullable Id getId(ItemStack stack) {
        return stack.get(Envelope.DataComponents.MAIL_ID);
    }

    public static Id getOrCreateId(ItemStack stack, Level level) {
        return stack.update(Envelope.DataComponents.MAIL_ID, Id.create(level), UnaryOperator.identity());
    }

    public static ItemStack setId(ItemStack stack, Id id) {
        stack.set(Envelope.DataComponents.MAIL_ID, id);
        return stack;
    }

    // -- Payback

    public static ItemStack setPaybackRequest(@NotNull ItemStack stack, @Nullable PaybackRequest request) {
        stack.set(Envelope.DataComponents.MAIL_PAYBACK_REQUEST, request);
        return stack;
    }

    // -- Returned

    public static boolean isReturned(ItemStack stack) {
        return stack.has(Envelope.DataComponents.MAIL_RETURNED);
    }

    public static ItemStack setReturned(ItemStack stack, boolean returned) {
        stack.set(Envelope.DataComponents.MAIL_RETURNED, returned ? Unit.INSTANCE : null);
        return stack;
    }

    public static ItemStack setReturned(ItemStack stack) {
        return setReturned(stack, true);
    }

    public static ItemStack returned(ItemStack mail, Component message) {
        if (mail.isEmpty()) {
            return mail;
        }
        writeToLog(mail, DeliveryRecord.returned(message));
        setReturned(mail);
        return mail;
    }

    // -- Log

    public static DeliveryLog getLog(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, DeliveryLog.EMPTY);
    }

    public static ItemStack setLog(ItemStack stack, DeliveryLog log) {
        stack.set(Envelope.DataComponents.MAIL_DELIVERY_LOG, log);
        return stack;
    }

    public static ItemStack writeToLog(ItemStack stack, DeliveryRecord record) {
        stack.update(Envelope.DataComponents.MAIL_DELIVERY_LOG, DeliveryLog.EMPTY, log -> log.append(record));
        return stack;
    }

    public static ItemStack writeToLog(ItemStack stack, DeliveryRecord... records) {
        stack.update(Envelope.DataComponents.MAIL_DELIVERY_LOG, DeliveryLog.EMPTY, log -> log.append(records));
        return stack;
    }

    // --

    public static ItemStack removePreviousDeliveryData(ItemStack mail) {
        if (mail.isEmpty()) return ItemStack.EMPTY;

        mail.remove(Envelope.DataComponents.MAIL_ID);
        mail.remove(Envelope.DataComponents.MAIL_SENDER);
        mail.remove(Envelope.DataComponents.MAIL_DELIVERY_LOG);
        mail.remove(Envelope.DataComponents.MAIL_RETURNED);

        return mail;
    }

    public static ItemStack removeAllDeliveryData(ItemStack mail) {
        if (mail.isEmpty()) return ItemStack.EMPTY;

        mail.remove(Envelope.DataComponents.MAIL_RECIPIENT);
        mail.remove(Envelope.DataComponents.MAIL_PAYBACK_REQUEST);
        return removePreviousDeliveryData(mail);
    }

    public static ItemStack asDelivered(ItemStack mail) {
        if (mail.isEmpty()) return ItemStack.EMPTY;

        if (!isReturned(mail)) {
            mail.remove(Envelope.DataComponents.MAIL_RECIPIENT);
            mail.remove(Envelope.DataComponents.MAIL_PAYBACK_REQUEST);
        }

        return mail;
    }

    // --

    public static MailBuilder<?> of(ItemStack stack) {
        return new MailBuilder<>(stack);
    }

    public static MailBuilder<?> createLetter(Component text) {
        return new MailBuilder<>(Envelope.Items.LETTER.get())
              .set(Envelope.DataComponents.LETTER_CONTENT, new LetterContent(text));
    }

    public static MailBuilder<?> createPackage(PackageContents contents) {
        return new MailBuilder<>(Envelope.Items.PACKAGE.get())
              .set(Envelope.DataComponents.PACKAGE_CONTENTS, contents);
    }

    public static MailBuilder<?> createPackage(ResourceKey<LootTable> lootTable) {
        return new MailBuilder<>(Envelope.Items.PACKAGE.get())
              .set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(lootTable, 0L));
    }

    /**
     * Splits items into packages, however many are required to fit all items.
     *
     * @param builder callback for each package builder, allows setting necessary data for all created packages.
     */
    public static List<ItemStack> createPackages(List<ItemStack> items, Consumer<MailBuilder<?>> builder) {
        items = new ArrayList<>(items); // Ensure mutability
        List<ItemStack> packages = new ArrayList<>();

        items.removeIf(stack -> {
            if (stack.getItem() instanceof PackageItem) {
                packages.add(Mail.of(stack).apply(builder).get());
                return true;
            }
            return false;
        });

        for (int i = 0; i < items.size(); i += PackageContents.SLOTS) {
            PackageContents contents = new PackageContents(items.subList(i, Math.min(i + PackageContents.SLOTS, items.size())));
            packages.add(Mail.createPackage(contents).apply(builder).get());
        }

        return packages;
    }

    public static MailBuilder<?> createPaybackBox(PaybackSubject subject) {
        return new MailBuilder<>(Envelope.Items.PAYBACK_BOX.get())
              .set(Envelope.DataComponents.PAYBACK_SUBJECT, subject);
    }

    // --

    public static boolean handleShearsUse(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        if (!slot.allowModification(player)) return false;
        if (!slot.getItem().is(Envelope.Tags.Items.MAILABLE)) return false;

        ItemStack result = removePreviousDeliveryData(slot.getItem().copy());

        if (!ItemStack.isSameItemSameComponents(slot.getItem(), result)) {
            slot.setByPlayer(result);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            player.playSound(SoundEvents.SHEEP_SHEAR, 0.75f, 1f);
        } else {
            player.playSound(SoundEvents.COMPARATOR_CLICK, 0.75f, 1f);
        }

        return true;
    }
}
