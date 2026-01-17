package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.envelope.command.argument.AddressArgument;
import io.github.mortuusars.envelope.command.suggestion.AddressSuggestions;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.item.component.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class EnvelopeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("envelope")
              .requires((stack) -> stack.hasPermission(2))
              .then(Commands.literal("send")
                    .then(Commands.argument("mail", ItemArgument.item(context))
                          .executes(c -> sendMail(c, ItemArgument.getItem(c, "mail"), Address.UNKNOWN))
                          .then(Commands.argument("sender", CompoundTagArgument.compoundTag())
                                .executes(c -> sendMail(c,
                                      ItemArgument.getItem(c, "mail"),
                                      parseAddress(CompoundTagArgument.getCompoundTag(c, "sender")))))))
              .then(Commands.literal("mailbox")
                    .then(Commands.literal("list")
                          .executes(EnvelopeCommand::listAllMailboxes)
                          .then(Commands.literal("default")
                                .executes(EnvelopeCommand::listDefaultMailboxes)))
                    .then(Commands.literal("position")
                          .then(Commands.argument("address", AddressArgument.block())
                                .suggests(AddressSuggestions.block())
                                .executes(c -> mailboxPosition(c, AddressArgument.getBlock(c, "address"))))))
              .then(EnvelopeDebugCommand.commands())
              .then(Commands.literal("test")
                    .executes(EnvelopeCommand::test)));
    }

    // -- Mail

    private static int sendMail(CommandContext<CommandSourceStack> context, ItemInput item, Address sender) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        ItemStack mail = item.createItemStack(1, false);

        MailService.of(level).getDeliveryManager()
              .startService(Delivery.builder().deliver(mail).from(sender).to(Mail.getRecipient(mail)))
              .ifPresentOrElse(
                    delivery -> {
                        Component message = Component.literal("Mail sent to ")
                              .append(delivery.delivery().getRecipient().format().asRecipient().toComponent());
                        context.getSource().sendSuccess(() -> message, true);
                    },
                    error -> context.getSource().sendFailure(Component.literal("Cannot send: ").append(error.getTranslation()))
              );

        return 0;
    }

    private static Address parseAddress(CompoundTag tag) {
        return Address.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    // -- Mailbox

    private static int listAllMailboxes(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        Set<Address.Block> addresses = MailService.of(level).mailboxes().getAllAddresses();

        if (!addresses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("All mailboxes:"), true);
            for (Address.Block address : addresses) {
                context.getSource().sendSuccess(() -> copyableAddressAndPos(address,
                      MailService.of(level).mailboxes().getPositionOf(address)), true);
            }
        } else {
            context.getSource().sendSuccess(() ->
                  Component.literal("There are no known mailboxes."), true);
        }
        return 0;
    }

    private static int listDefaultMailboxes(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();

        Map<Address.Player, Address.Block> defaultAddresses = MailService.of(level).getPlayers().getDefaultAddresses();

        if (!defaultAddresses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Default addresses:"), true);

            defaultAddresses.forEach((playerAddress, address) -> {
                Optional<BlockPos> position = MailService.of(level).mailboxes().getPositionOf(address);
                context.getSource().sendSuccess(() -> Component.literal(playerAddress.toString())
                      .append(" - ")
                      .append(copyableAddressAndPos(address, position)), true);
            });
        } else {
            context.getSource().sendSuccess(() ->
                  Component.literal("There are no default mailboxes."), true);
        }

        return 0;
    }

    private static int mailboxPosition(CommandContext<CommandSourceStack> context, Address.Block address) {
        ServerLevel level = context.getSource().getLevel();
        if (!MailService.of(level).mailboxes().exists(address)) {
            context.getSource().sendFailure(address.getName().append(" does not exist."));
            return 1;
        }

        MailService.of(level).mailboxes().getPositionOf(address)
              .ifPresentOrElse(
                    pos -> context.getSource().sendSuccess(() -> copyableAddressAndPos(address, Optional.of(pos)), true),
                    () -> context.getSource().sendFailure(address.getName()
                          .append(" does not have a position associated with it.")));
        return 0;
    }

    private static MutableComponent copyableAddressAndPos(Address address, Optional<BlockPos> pos) {
        String addressId = address.getName().getString();
        String posStr = pos.map(BlockPos::toShortString).orElse("");
        String posToCopy = posStr.replace(",", "");

        return address.getName()
              .withStyle(Style.EMPTY
                    .withColor(AddressFormatter.NEUTRAL_COLOR)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy Address")
                          .append("\n")
                          .append(Component.literal(addressId).withStyle(ChatFormatting.GRAY))))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, addressId)))
              .append(Component.literal("@[" + posStr + "]").withStyle(Style.EMPTY
                    .withColor(ChatFormatting.WHITE)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy Position")
                          .append("\n")
                          .append(Component.literal(posToCopy).withStyle(ChatFormatting.GRAY))))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, posToCopy))));
    }

    // --

    private static int test(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

//        ItemStack pkg = new ItemStack(Envelope.Items.PACKAGE.get());
//        pkg.set(Envelope.DataComponents.PACKAGE_CONTENTS, new PackageContents(List.of(new ItemStack(Items.FEATHER, 5))));
//        pkg.set(Envelope.DataComponents.SENDER, new Address.Block("Original-Sender"));
//        pkg.set(Envelope.DataComponents.RECIPIENT, new Address.Block("Base"));
//        pkg.set(Envelope.DataComponents.PAYBACK, Payback.createOrDefault(List.of(
//              new RequestedItem(Items.EMERALD, 3), new RequestedItem(ItemTags.LOGS, 13))));
//
//        Mail mail = new Mail(pkg);
//
//        ItemStack paybackPackage = new ItemStack(Envelope.Items.PAYBACK_PACKING_BOX.get());
//        paybackPackage.set(Envelope.DataComponents.PAYBACK_SUBJECT, new StoredItemStack(mail.getItemCopy()));
//        paybackPackage.set(Envelope.DataComponents.SENDER, Address.MAIL_SERVICE);
//        paybackPackage.set(Envelope.DataComponents.RECIPIENT, mail.getRecipient());
//
//        Containers.dropItemStack(context.getSource().getLevel(), player.getX(), player.getY(), player.getZ(), paybackPackage);

        return 0;
    }
}