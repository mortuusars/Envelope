package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.envelope.command.argument.AddressArgument;
import io.github.mortuusars.envelope.command.suggestion.AddressSuggestions;
import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.TestResults;
import io.github.mortuusars.envelope.util.bugger.test.cases.RequestedItemTests;
import io.github.mortuusars.envelope.world.delivery.Courier;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
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
                          .executes(c -> sendMail(c, ItemArgument.getItem(c, "mail")))))
              .then(Commands.literal("pigeonhole")
                    .then(Commands.literal("list")
                          .executes(EnvelopeCommand::listAllPigeonholes)
                          .then(Commands.literal("default")
                                .executes(EnvelopeCommand::listDefaultPigeonholes)))
                    .then(Commands.literal("position")
                          .then(Commands.argument("address", AddressArgument.pigeonhole())
                                .suggests(AddressSuggestions.pigeonhole())
                                .executes(c -> pigeonholePosition(c, AddressArgument.getPigeonhole(c, "address"))))))
              .then(Commands.literal("debug")
                    .then(Commands.literal("tests")
                          .executes(c -> runBuggerTests(c)))));
    }

    // -- Mail

    private static int sendMail(CommandContext<CommandSourceStack> context, ItemInput item) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        ItemStack mail = item.createItemStack(1, false);

        try {
            Courier courier = level.getEnvelopeContext().startDelivery(mail);
            Delivery delivery = courier.getDelivery().orElseThrow();
            Component message = Component.literal("Mail sent to ")
                  .append(delivery.getRecipient().format().asRecipient().toComponent());
            context.getSource().sendSuccess(() -> message, true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Cannot send: " + e.getMessage()));
        }

        return 0;
    }

    // -- Pigeonhole

    private static int listAllPigeonholes(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        Set<Address.Pigeonhole> addresses = level.getEnvelopeContext().getPigeonholeManager().getAllAddresses();

        if (!addresses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("All pigeonholes:"), true);
            for (Address.Pigeonhole address : addresses) {
                context.getSource().sendSuccess(() -> copyableAddressAndPos(address,
                      level.getEnvelopeContext().getPigeonholeManager().getPositionOf(address)), true);
            }
        } else {
            context.getSource().sendSuccess(() ->
                  Component.literal("There are no addressed pigeonholes."), true);
        }
        return 0;
    }

    private static int listDefaultPigeonholes(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();

        Map<Address.Player, Address.Pigeonhole> defaultAddresses = level.getEnvelopeContext().getPlayers().getDefaultAddresses();

        if (!defaultAddresses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Default addresses:"), true);

            defaultAddresses.forEach((playerAddress, address) -> {
                Optional<BlockPos> position = level.getEnvelopeContext().getPigeonholeManager().getPositionOf(address);
                context.getSource().sendSuccess(() -> Component.literal(playerAddress.toString())
                      .append(" - ")
                      .append(copyableAddressAndPos(address, position)), true);
            });
        } else {
            context.getSource().sendSuccess(() ->
                  Component.literal("There are no default pigeonholes."), true);
        }

        return 0;
    }

    private static int pigeonholePosition(CommandContext<CommandSourceStack> context, Address.Pigeonhole address) {
        ServerLevel level = context.getSource().getLevel();
        if (!level.getEnvelopeContext().getPigeonholeManager().exists(address)) {
            context.getSource().sendFailure(address.getName().append(" does not exist."));
            return 1;
        }

        level.getEnvelopeContext().getPigeonholeManager().getPositionOf(address)
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

    private static int runBuggerTests(CommandContext<CommandSourceStack> context) {
        TestResults testResults = new BuggerTests()
              .add(new RequestedItemTests(context.getSource().getServer()))
              .run(count -> context.getSource().sendSuccess(() ->
                    Component.literal("Running " + count + " bugger tests."), true));

        context.getSource().sendSuccess(() -> Component.literal("Bugger tests finished:"), true);

        if (testResults.failed().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("All tests are passed!")
                  .withStyle(ChatFormatting.GREEN), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Passed: " + testResults.passed().size() + "\n"), true);
            context.getSource().sendSuccess(() -> Component.literal("Failed: " + testResults.failed().size() + ":")
                  .withStyle(ChatFormatting.RED), true);

            testResults.failed().forEach(failedTest -> {
                context.getSource().sendSuccess(() -> Component.literal(" " + failedTest.name() + ": " + failedTest.error())
                      .withStyle(ChatFormatting.RED), true);
            });
        }

        return 0;
    }
}