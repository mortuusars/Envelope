package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.TestResults;
import io.github.mortuusars.envelope.util.bugger.test.cases.RequestedItemTests;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class EnvelopeDebugCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("debug")
              .then(Commands.literal("timeout_all_mail_awaiting_payback")
                    .executes(EnvelopeDebugCommand::timeoutAllPaybackMail))
              .then(Commands.literal("tests")
                    .executes(EnvelopeDebugCommand::runBuggerTests));
    }

    private static int timeoutAllPaybackMail(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        MailService service = MailService.of(level);
        int returnedCount = service.getMailService().getPaybackDepartment().returnAllAwaitingAsTimedOut();

        if (returnedCount > 0) {
            context.getSource().sendSuccess(() -> Component.literal("Returned " +
                  returnedCount + " mail awaiting payback."), true);
        } else {
            context.getSource().sendFailure(Component.literal("No mail awaiting payback is returned."));
        }

        return 0;
    }

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
