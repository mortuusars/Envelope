package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.envelope.command.argument.AddressArgument;
import io.github.mortuusars.envelope.command.suggestion.AddressSuggestions;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
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
                                      parseAddress(c, CompoundTagArgument.getCompoundTag(c, "sender")))))))
              .then(Commands.literal("mailbox")
                    .then(Commands.literal("list")
                          .executes(EnvelopeCommand::listAllMailboxes)
                          .then(Commands.literal("default")
                                .executes(EnvelopeCommand::listDefaultMailboxes)))
                    .then(Commands.literal("position")
                          .then(Commands.argument("address", AddressArgument.block())
                                .suggests(AddressSuggestions.block())
                                .executes(c -> mailboxPosition(c, AddressArgument.getBlock(c, "address"))))))
              .then(EnvelopeDebugCommand.commands()));
    }

    // -- Mail

    private static int sendMail(CommandContext<CommandSourceStack> context, ItemInput item, Address sender) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        ItemStack mail = item.createItemStack(1, false);

        Address recipient = Mail.getRecipientOrUnknown(mail);

        if (mail.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Cannot send: mail is empty."));
            return 0;
        }

        if (recipient.equals(Address.UNKNOWN)) {
            context.getSource().sendFailure(Component.literal("Cannot send: recipient is not defined."));
            return 0;
        }

        MailService.of(level).getDeliveryManager()
              .startService(Delivery.draft()
                    .deliver(mail)
                    .from(sender)
                    .to(recipient));

        Component message = Component.literal("Mail sent to ").append(recipient.format().asRecipient().toComponent());
        context.getSource().sendSuccess(() -> message, true);

        return 0;
    }

    private static Address parseAddress(CommandContext<CommandSourceStack> context, CompoundTag tag) {
        return Address.CODEC.parse(context.getSource().registryAccess().createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
    }

    // -- Mailbox

    private static int listAllMailboxes(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        Set<BlockAddress> addresses = MailService.of(level).getMailboxes().getAllAddresses();

        if (!addresses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("All mailboxes:"), true);
            for (BlockAddress address : addresses) {
                context.getSource().sendSuccess(() -> copyableAddressAndPos(address,
                      MailService.of(level).getMailboxes().getPositionOf(address)), true);
            }
        } else {
            context.getSource().sendSuccess(() ->
                  Component.literal("There are no known mailboxes."), true);
        }
        return 0;
    }

    private static int listDefaultMailboxes(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();

        Map<PlayerAddress, BlockAddress> defaultAddresses = MailService.of(level).getKnownPlayers().getDefaultAddresses();

        if (!defaultAddresses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Default addresses:"), true);

            defaultAddresses.forEach((playerAddress, address) -> {
                Optional<BlockPos> position = MailService.of(level).getMailboxes().getPositionOf(address);
                context.getSource().sendSuccess(() -> Component.literal(playerAddress.getString())
                      .append(" - ")
                      .append(copyableAddressAndPos(address, position)), true);
            });
        } else {
            context.getSource().sendSuccess(() ->
                  Component.literal("There are no default mailboxes."), true);
        }

        return 0;
    }

    private static int mailboxPosition(CommandContext<CommandSourceStack> context, BlockAddress address) {
        ServerLevel level = context.getSource().getLevel();
        if (!MailService.of(level).getMailboxes().exists(address)) {
            context.getSource().sendFailure(address.getComponent().append(" does not exist."));
            return 1;
        }

        MailService.of(level).getMailboxes().getPositionOf(address)
              .ifPresentOrElse(
                    pos -> context.getSource().sendSuccess(() -> copyableAddressAndPos(address, Optional.of(pos)), true),
                    () -> context.getSource().sendFailure(address.getComponent()
                          .append(" does not have a position associated with it.")));
        return 0;
    }

    private static MutableComponent copyableAddressAndPos(Address address, Optional<BlockPos> pos) {
        String name = address.getString();
        String posStr = pos.map(BlockPos::toShortString).orElse("");
        String posToCopy = posStr.replace(",", "");

        return Component.literal(name)
              .withStyle(Style.EMPTY
                    .withColor(Colors.ADDRESS_NEUTRAL)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy Address")
                          .append("\n")
                          .append(Component.literal(name).withStyle(ChatFormatting.GRAY))))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name)))
              .append(Component.literal("@[" + posStr + "]").withStyle(Style.EMPTY
                    .withColor(ChatFormatting.WHITE)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy Position")
                          .append("\n")
                          .append(Component.literal(posToCopy).withStyle(ChatFormatting.GRAY))))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, posToCopy))));
    }
}