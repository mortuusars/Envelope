package io.github.mortuusars.envelope.world.item.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.ContainerUtils;
import io.github.mortuusars.envelope.world.item.PackageItem;
import io.github.mortuusars.envelope.world.item.component.*;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryInfo;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryLog;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.tooltip.MailAddressTagTooltip;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.mortaar.world.item.tooltip.CompositeTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import java.util.stream.Stream;

public final class Mail {
    private Mail() {
    }

    // -- Address

    public static Optional<Address> getSender(ItemStack stack) {
        return DeliveryInfo.of(stack).sender();
    }

    public static @NotNull Address getSenderOrElse(ItemStack stack, Address orElse) {
        return getSender(stack).orElse(orElse);
    }

    public static @NotNull Address getSenderOrUnknown(ItemStack stack) {
        return getSenderOrElse(stack, Address.UNKNOWN);
    }

    public static void setSender(@NotNull ItemStack stack, @Nullable Address sender) {
        DeliveryInfo.of(stack).mutable().sender(sender).immutableApplyTo(stack);
    }

    public static Optional<Address> getRecipient(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.MAIL_ADDRESS_TAG));
    }

    public static @NotNull Address getRecipientOrElse(ItemStack stack, Address orElse) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_ADDRESS_TAG, orElse);
    }

    public static @NotNull Address getRecipientOrUnknown(ItemStack stack) {
        return getRecipientOrElse(stack, Address.UNKNOWN);
    }

    public static ItemStack setRecipient(@NotNull ItemStack stack, @Nullable Address recipient) {
        stack.set(Envelope.DataComponents.MAIL_ADDRESS_TAG, recipient);
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
        stack.set(Envelope.DataComponents.MAIL_PAYBACK_TAG, request);
        return stack;
    }

    // -- Returned

    public static boolean isReturned(ItemStack stack) {
        return DeliveryInfo.of(stack).isReturned();
    }

    public static ItemStack setReturned(ItemStack stack, boolean returned) {
        DeliveryInfo.of(stack).mutable().returned(returned).immutableApplyTo(stack);
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
        return DeliveryInfo.of(stack).log();
    }

    public static ItemStack setLog(ItemStack stack, DeliveryLog log) {
        return DeliveryInfo.of(stack).mutable().setLog(log).immutableApplyTo(stack);
    }

    public static ItemStack writeToLog(ItemStack stack, DeliveryRecord record) {
        return DeliveryInfo.of(stack).mutable().updateLog(log -> log.append(record)).immutableApplyTo(stack);
    }

    public static ItemStack writeToLog(ItemStack stack, DeliveryRecord... records) {
        return DeliveryInfo.of(stack).mutable().updateLog(log -> log.append(records)).immutableApplyTo(stack);
    }

    // --

    public static ItemStack removePreviousDeliveryData(ItemStack mail) {
        if (mail.isEmpty()) return ItemStack.EMPTY;

        mail.remove(Envelope.DataComponents.MAIL_ID);
        mail.remove(Envelope.DataComponents.MAIL_DELIVERY_INFO);

        return mail;
    }

    public static ItemStack removeAllDeliveryData(ItemStack mail) {
        if (mail.isEmpty()) return ItemStack.EMPTY;

        mail.remove(Envelope.DataComponents.MAIL_ADDRESS_TAG);
        mail.remove(Envelope.DataComponents.MAIL_PAYBACK_TAG);
        return removePreviousDeliveryData(mail);
    }

    public static ItemStack asDelivered(ItemStack mail) {
        if (mail.isEmpty()) return ItemStack.EMPTY;

        if (!isReturned(mail)) {
            DeliveryInfo.of(mail).mutable().recipient(mail.get(Envelope.DataComponents.MAIL_ADDRESS_TAG)).immutableApplyTo(mail);
            mail.remove(Envelope.DataComponents.MAIL_ADDRESS_TAG);
            mail.remove(Envelope.DataComponents.MAIL_PAYBACK_TAG);
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

    public static MailBuilder<?> createLetter(LetterContent content) {
        return new MailBuilder<>(Envelope.Items.LETTER.get())
              .set(Envelope.DataComponents.LETTER_CONTENT, content);
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
     * @param builder callback for each package builder, allows setting necessary data for each created package.
     */
    public static List<ItemStack> createPackages(List<ItemStack> items, Consumer<MailBuilder<?>> builder) {
        items = new ArrayList<>(items.stream().filter(stack -> !stack.isEmpty()).toList()); // Non-empty and ensure mutability
        if (items.isEmpty()) return List.of();

        // Separate items that are already a package:
        List<ItemStack> packages = new ArrayList<>();
        items.removeIf(stack -> {
            if (stack.getItem() instanceof PackageItem) {
                packages.add(Mail.of(stack).apply(builder).get());
                return true;
            }
            return false;
        });

        List<ItemStack> packedPackages = ContainerUtils.split(ContainerUtils.compact(items), PackageContents.SLOTS)
              .stream()
              .map(PackageContents::new)
              .map(contents -> Mail.createPackage(contents).apply(builder).get())
              .toList();

        return Stream.concat(packages.stream(), packedPackages.stream()).toList();
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

    public static Optional<TooltipComponent> modifyTooltipImage(ItemStack stack, Optional<TooltipComponent> original) {
        if (stack.is(Envelope.Tags.Items.MAILABLE)) {
            return CompositeTooltip.of(
                  original,
                  Optional.ofNullable(stack.get(Envelope.DataComponents.MAIL_ADDRESS_TAG)).map(MailAddressTagTooltip::new),
                  Optional.ofNullable(stack.get(Envelope.DataComponents.MAIL_PAYBACK_TAG))
            );
        }

        return original;
    }

    public static void appendTooltipLines(ItemStack stack, Consumer<Component> consumer,
                                          Item.TooltipContext context, Player player, TooltipFlag tooltipFlag) {
        DeliveryInfo deliveryInfo = DeliveryInfo.of(stack);
        if (!deliveryInfo.isEmpty()) {

            if (Screen.hasShiftDown() && !deliveryInfo.log().isEmpty()) {
                consumer.accept(Component.translatable("gui.envelope.delivery_log"));
                for (DeliveryRecord record : deliveryInfo.log().records()) {
                    consumer.accept(record.getDisplayComponent());
                }
            } else {
                deliveryInfo.sender().ifPresent(sender -> {
                    consumer.accept(Component.translatable("gui.envelope.mail.from", sender.format().asSender().toComponent())
                          .withStyle(ChatFormatting.GRAY));
                });

                deliveryInfo.recipient().ifPresent(recipient -> {
                    consumer.accept(Component.translatable("gui.envelope.mail.to", recipient.format().asRecipient().toComponent())
                          .withStyle(ChatFormatting.GRAY));
                });
            }
        }

        if (tooltipFlag.isAdvanced()) {
            Optional.ofNullable(getId(stack)).ifPresent(id -> {
                consumer.accept(Component.literal("Mail Id: " + id).withStyle(ChatFormatting.DARK_GRAY));
            });
        }
    }
}
